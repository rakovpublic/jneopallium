/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrial;

/**
 * Deterministic first-order-plus-dead-time (FOPDT) model of a continuous
 * stirred-tank reactor (CSTR) with a cooling jacket, for
 * {@code demo-01-reactor-cascade-control.md}.
 *
 * <p>This is the in-process stand-in for the external plant the demo
 * describes (a {@code python-asyncua} script or a real PLC). The physics
 * are intentionally simple but produce the behaviours the demo needs:
 * a cascade-controllable temperature, a fast inner coolant-flow loop that
 * can be driven into a period-2 limit cycle by an over-aggressive gain,
 * and a high-temperature interlock flag.
 *
 * <h2>Model</h2>
 * <pre>
 *   coolant flow:  F += (valve - F) * (dt / tauF)            [%]
 *   reactor temp:  dT/dt = (Qrxn - K * (F/100) * (T - Tcool)) / C   [°C/s]
 * </pre>
 * More valve ⇒ more coolant flow ⇒ more heat removed ⇒ lower temperature,
 * so the outer temperature loop is <em>reverse-acting</em>.
 *
 * <p>The class is single-threaded and side-effect free apart from its own
 * state; the bridge clock drives it via {@link #step(double)}.
 */
public final class ReactorPlantSimulator {

    // --- plant parameters (chosen so equilibrium is T=80°C at F=50%) ---
    private final double coolInlet;     // Tcool, coolant inlet temperature [°C]
    private final double thermalMass;   // C, lumped thermal capacitance
    private final double coolGain;      // K, cooling authority
    private final double flowTau;       // tauF, coolant-flow time constant [s]
    private final double hiTempLimit;   // hard interlock threshold [°C]

    private double heatLoad;            // Qrxn, exothermic heat input
    private double reactorTemp;         // T, reactor temperature [°C]
    private double coolantFlow;         // F, coolant flow [%]
    private double valve;               // jacket valve opening [%], 0..100

    private boolean forcedInterlock;    // operator/test forced trip
    private Double tempOverride;        // when set, reported PV is pinned here

    /** Builds a plant pre-loaded at its nominal 80 °C / 50 % operating point. */
    public ReactorPlantSimulator() {
        this(20.0, 80.0, 6.0, 0.12, 95.0, 180.0);
        this.reactorTemp = 80.0;
        this.coolantFlow = 50.0;
        this.valve = 50.0;
    }

    public ReactorPlantSimulator(double coolInlet, double thermalMass, double coolGain,
                                 double flowTau, double hiTempLimit, double heatLoad) {
        this.coolInlet = coolInlet;
        this.thermalMass = thermalMass;
        this.coolGain = coolGain;
        this.flowTau = Math.max(1e-3, flowTau);
        this.hiTempLimit = hiTempLimit;
        this.heatLoad = heatLoad;
        this.reactorTemp = coolInlet;
        this.coolantFlow = 0.0;
        this.valve = 0.0;
    }

    /** Advances the plant by {@code dtSeconds} using the current valve command. */
    public synchronized void step(double dtSeconds) {
        if (dtSeconds <= 0) return;
        double a = Math.min(1.0, dtSeconds / flowTau);
        coolantFlow += (valve - coolantFlow) * a;
        coolantFlow = clamp(coolantFlow, 0.0, 100.0);

        double removed = coolGain * (coolantFlow / 100.0) * (reactorTemp - coolInlet);
        double dT = (heatLoad - removed) / thermalMass;
        reactorTemp += dT * dtSeconds;
    }

    /* ---- actuator (written by the bridge) ---- */

    public synchronized void setValve(double percent) {
        this.valve = clamp(percent, 0.0, 100.0);
    }

    public synchronized double getValve() { return valve; }

    /* ---- measurements (read by the bridge) ---- */

    /** Reactor temperature PV (°C); honours a test/operator pin if set. */
    public synchronized double getReactorTempPV() {
        return tempOverride != null ? tempOverride : reactorTemp;
    }

    public synchronized double getCoolantFlowPV() { return coolantFlow; }

    /** True once the (possibly pinned) temperature crosses the hard limit. */
    public synchronized boolean isHiTempInterlock() {
        return forcedInterlock || getReactorTempPV() >= hiTempLimit;
    }

    public double getHiTempLimit() { return hiTempLimit; }

    /* ---- disturbance / scenario injection ---- */

    /** Steps the exothermic heat input, e.g. to drive a runaway. */
    public synchronized void setHeatLoad(double q) { this.heatLoad = q; }

    /** Forces the interlock regardless of temperature (demo step 5). */
    public synchronized void forceHiTempInterlock(boolean on) { this.forcedInterlock = on; }

    /** Pins the reported temperature PV (deterministic interlock trip); null clears. */
    public synchronized void pinReactorTemp(Double t) { this.tempOverride = t; }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : Math.min(v, hi);
    }
}
