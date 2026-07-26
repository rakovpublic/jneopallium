/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fmi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the FMU lifecycle and the per-tick simulation loop
 * (03-FMI-FMU.md §4, §4.1).
 *
 * <h2>Lifecycle</h2>
 * <pre>
 *   service = new FmuClientService(driver, description, config);
 *   service.initialize();                       // setupExperiment + init modes
 *   while (running) {
 *       // reads from cache → pipeline
 *       service.setReal(varName, value);        // aggregator writes (buffered to driver)
 *       service.step(wallClockMs);              // doStep → refresh cache
 *   }
 *   service.close();                            // terminate + driver.close()
 * </pre>
 *
 * <h2>Thread safety</h2>
 * {@link #setReal} / {@link #setBoolean} are allowed from the aggregator's
 * save() call which runs on the same single-threaded dispatcher as
 * {@link #step}. The read cache ({@link #getReal} etc.) uses a
 * {@link ConcurrentHashMap} so the input-side read path (a separate
 * IInitInput thread) is safe.
 *
 * <h2>Clock modes (§4.1)</h2>
 * <ul>
 *   <li>{@code AS_FAST_AS_POSSIBLE}: {@link #step} calls {@code doStep}
 *       immediately and returns. CI default.</li>
 *   <li>{@code REAL_TIME}: {@link #step} sleeps until the next scheduled
 *       wall-clock instant before returning. HIL demo mode.</li>
 * </ul>
 */
public final class FmuClientService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(FmuClientService.class);

    private final FmuDriver driver;
    private final FmuModelDescription description;
    private final FmiBridgeConfig config;

    /** Latest cached Real values keyed by FMU variable name. */
    private final ConcurrentHashMap<String, Double> realCache = new ConcurrentHashMap<>();

    /** Latest cached Boolean values keyed by FMU variable name. */
    private final ConcurrentHashMap<String, Boolean> boolCache = new ConcurrentHashMap<>();

    /** Latest cached Integer values keyed by FMU variable name. */
    private final ConcurrentHashMap<String, Integer> intCache = new ConcurrentHashMap<>();

    /** Pending setReal() calls buffered until the next step(). */
    private final Map<Integer, Double> pendingRealWrites = new HashMap<>();

    /** Pending setBoolean() calls. */
    private final Map<Integer, Boolean> pendingBoolWrites = new HashMap<>();

    private double simulationTime;
    private boolean initialized;
    private boolean terminated;
    private FmuDriver.FmiStatus lastStatus;

    /** Wall-clock time of the next scheduled real-time tick (epoch ms). */
    private long nextRealTimeTick;

    public FmuClientService(FmuDriver driver, FmuModelDescription description, FmiBridgeConfig config) {
        this.driver = Objects.requireNonNull(driver, "driver");
        this.description = Objects.requireNonNull(description, "description");
        this.config = Objects.requireNonNull(config, "config");
        this.simulationTime = config.clock().startTime();
        this.lastStatus = FmuDriver.FmiStatus.OK;
    }

    /**
     * Run the FMI initialization sequence and do the first read from all
     * configured variables. Must be called once before the first {@link #step}.
     */
    public synchronized void initialize() {
        if (initialized) throw new IllegalStateException("Already initialized");
        String modelName = description.modelName();
        FmiBridgeConfig.FmuConfig fc = config.fmu();
        log.info("FmuClientService: initializing '{}' (FMI {})", modelName, description.fmiVersion());

        driver.instantiate(modelName + "-jneopallium", fc.loggingOn());
        driver.setupExperiment(simulationTime, fc.toleranceDefined(), fc.tolerance());
        driver.enterInitializationMode();
        driver.exitInitializationMode();

        refreshAllFromDriver();
        initialized = true;
        nextRealTimeTick = System.currentTimeMillis()
                + (long) (config.clock().stepSize() * 1000);
        log.info("FmuClientService: initialized. simulationTime={}", simulationTime);
    }

    /**
     * Flush pending writes, advance the simulation by one step, then refresh
     * the read cache.
     *
     * <p>In {@code REAL_TIME} mode this method blocks until the next scheduled
     * wall-clock tick to maintain a fixed cadence.
     *
     * @param wallClockMs current wall-clock epoch-millis (used only for
     *                    real-time sleep scheduling; ignored in AFAP mode)
     * @throws FmuException if doStep returns ERROR or FATAL
     */
    public synchronized void step(long wallClockMs) {
        if (!initialized) throw new IllegalStateException("Not initialized");
        if (terminated) throw new IllegalStateException("Already terminated");

        // Apply buffered writes to the driver before stepping
        pendingRealWrites.forEach(driver::setReal);
        pendingRealWrites.clear();
        pendingBoolWrites.forEach(driver::setBoolean);
        pendingBoolWrites.clear();

        double h = config.clock().stepSize();
        FmuDriver.FmiStatus status = driver.doStep(simulationTime, h);
        lastStatus = status;

        if (status == FmuDriver.FmiStatus.ERROR || status == FmuDriver.FmiStatus.FATAL) {
            log.error("FMU doStep returned {} at t={}", status, simulationTime);
            throw new FmuException("doStep returned " + status + " at t=" + simulationTime, status);
        }
        if (status == FmuDriver.FmiStatus.WARNING || status == FmuDriver.FmiStatus.DISCARD) {
            log.warn("FMU doStep returned {} at t={}", status, simulationTime);
        }

        simulationTime += h;
        refreshAllFromDriver();

        if (config.clock().mode() == FmiBridgeConfig.ClockConfig.ClockMode.REAL_TIME) {
            sleepUntilNextTick();
        }
    }

    /** Buffer a Real write; flushed to the driver at the next {@link #step}. */
    public void setReal(String variableName, double value) {
        int vr = description.valueReference(variableName);
        pendingRealWrites.put(vr, value);
    }

    /** Buffer a Boolean write; flushed to the driver at the next {@link #step}. */
    public void setBoolean(String variableName, boolean value) {
        int vr = description.valueReference(variableName);
        pendingBoolWrites.put(vr, value);
    }

    /** Read the latest cached Real value for a FMU variable. Returns {@code Double.NaN} if not yet read. */
    public double getReal(String variableName) {
        return realCache.getOrDefault(variableName, Double.NaN);
    }

    /** Read the latest cached Boolean value. Returns {@code false} if not yet read. */
    public boolean getBoolean(String variableName) {
        return boolCache.getOrDefault(variableName, Boolean.FALSE);
    }

    /** Read the latest cached Integer value. Returns {@code 0} if not yet read. */
    public int getInteger(String variableName) {
        return intCache.getOrDefault(variableName, 0);
    }

    public double simulationTime() { return simulationTime; }
    public FmuDriver.FmiStatus lastStatus() { return lastStatus; }
    public boolean isInitialized() { return initialized; }
    public boolean isTerminated() { return terminated; }

    /** Terminate and release the FMU. Safe to call multiple times. */
    @Override
    public synchronized void close() {
        if (terminated) return;
        terminated = true;
        try {
            if (initialized) {
                driver.terminate();
            }
        } catch (RuntimeException e) {
            log.warn("FMU terminate() threw: {}", e.getMessage());
        } finally {
            try {
                driver.close();
            } catch (RuntimeException e) {
                log.warn("FMU driver.close() threw: {}", e.getMessage());
            }
        }
        log.info("FmuClientService: terminated at simulationTime={}", simulationTime);
    }

    // ============================================================

    private void refreshAllFromDriver() {
        for (FmiBridgeConfig.ReadBindingConfig r : config.reads()) {
            if (description.contains(r.fmuVariable())) {
                int vr = description.valueReference(r.fmuVariable());
                realCache.put(r.fmuVariable(), driver.getReal(vr));
            }
        }
        for (FmiBridgeConfig.EventBindingConfig e : config.events()) {
            if (description.contains(e.fmuVariable())) {
                int vr = description.valueReference(e.fmuVariable());
                boolCache.put(e.fmuVariable(), driver.getBoolean(vr));
            }
        }
    }

    private void sleepUntilNextTick() {
        long now = System.currentTimeMillis();
        long remaining = nextRealTimeTick - now;
        if (remaining > 1) {
            try {
                Thread.sleep(remaining);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        nextRealTimeTick += (long) (config.clock().stepSize() * 1000);
    }
}
