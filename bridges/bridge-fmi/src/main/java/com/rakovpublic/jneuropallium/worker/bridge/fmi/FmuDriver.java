/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fmi;

/**
 * Abstraction over the FMI C API for co-simulation (FMI 2.0 §3, FMI 3.0 §4).
 *
 * <p>All value-reference integers are resolved by {@link FmuModelDescription}
 * from the FMU's {@code modelDescription.xml}. The interface covers the
 * lifecycle methods needed for the driver's single doStep loop:
 * instantiate → setupExperiment → enterInit → exitInit →
 * {setInputs → doStep → getOutputs}* → terminate.
 *
 * <p>Implementations:
 * <ul>
 *   <li>Production: a JNA or javafmi adapter that loads the native shared
 *       library extracted from the FMU ZIP.</li>
 *   <li>Tests: {@code StubFmuDriver} — a pure-Java tank-temperature model
 *       that never touches the file system or native code.</li>
 * </ul>
 *
 * <p>All methods may throw {@link FmuException} on FMI-layer errors.
 * Callers must call {@link #terminate()} before {@link #close()} even on
 * error paths (see R2 in 03-FMI-FMU.md §10).
 */
public interface FmuDriver extends AutoCloseable {

    void instantiate(String instanceName, boolean loggingOn);

    void setupExperiment(double startTime, boolean toleranceDefined, double tolerance);

    void enterInitializationMode();

    void exitInitializationMode();

    double getReal(int valueReference);

    boolean getBoolean(int valueReference);

    int getInteger(int valueReference);

    void setReal(int valueReference, double value);

    void setBoolean(int valueReference, boolean value);

    /**
     * Advance the simulation by {@code stepSize} seconds from {@code currentTime}.
     *
     * @return the FMI status; callers must treat anything other than {@link FmiStatus#OK}
     *         or {@link FmiStatus#WARNING} as a terminal error.
     */
    FmiStatus doStep(double currentTime, double stepSize);

    void terminate();

    /** Release all native resources. Called after {@link #terminate()}. */
    @Override
    void close();

    enum FmiStatus { OK, WARNING, DISCARD, ERROR, FATAL, PENDING }
}
