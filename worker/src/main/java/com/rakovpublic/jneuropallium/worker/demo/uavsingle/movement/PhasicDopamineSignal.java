package com.rakovpublic.jneuropallium.worker.demo.uavsingle.movement;

import com.rakovpublic.jneuropallium.worker.demo.uavsingle.UavSingleSignal;

/** Reward-prediction pulse delivered to temporal movement action neurons. */
public class PhasicDopamineSignal extends UavSingleSignal {
    private double concentration;
    private double predictionError;
    private String reason = "MOVEMENT_REWARD";

    public PhasicDopamineSignal() {
        setEventType("PHASIC_DOPAMINE_MOVEMENT");
        this.loop = 2;
    }

    public PhasicDopamineSignal(double concentration, double predictionError, long tick, String reason) {
        this();
        this.concentration = concentration;
        this.predictionError = predictionError;
        this.reason = reason == null ? this.reason : reason;
        setTick(tick);
    }

    public double getConcentration() { return concentration; }
    public void setConcentration(double concentration) { this.concentration = concentration; }
    public double getPredictionError() { return predictionError; }
    public void setPredictionError(double predictionError) { this.predictionError = predictionError; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason == null ? this.reason : reason; }
}
