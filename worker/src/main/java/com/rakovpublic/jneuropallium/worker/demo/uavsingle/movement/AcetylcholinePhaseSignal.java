package com.rakovpublic.jneuropallium.worker.demo.uavsingle.movement;

import com.rakovpublic.jneuropallium.worker.demo.uavsingle.UavSingleSignal;

/** Temporal gate for movement action neurons: dip gates learning, burst gates vigor. */
public class AcetylcholinePhaseSignal extends UavSingleSignal {
    public enum Phase { DIP, BASELINE, BURST }

    private Phase phase = Phase.BASELINE;
    private double intensity = 1.0;

    public AcetylcholinePhaseSignal() {
        setEventType("ACETYLCHOLINE_PHASE_MOVEMENT");
        this.loop = 2;
    }

    public AcetylcholinePhaseSignal(Phase phase, double intensity, long tick) {
        this();
        this.phase = phase == null ? Phase.BASELINE : phase;
        this.intensity = intensity;
        setTick(tick);
    }

    public Phase getPhase() { return phase; }
    public void setPhase(Phase phase) { this.phase = phase == null ? Phase.BASELINE : phase; }
    public double getIntensity() { return intensity; }
    public void setIntensity(double intensity) { this.intensity = intensity; }
}
