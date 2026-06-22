package com.rakovpublic.jneuropallium.worker.demo.uavsingle.movement;

import com.rakovpublic.jneuropallium.worker.demo.uavsingle.UavSingleSignal;

/** Slow movement-layer homeostasis signal used to reduce repeated low-value actions. */
public class HomeostasisSignal extends UavSingleSignal {
    private double fatigue;
    private double explorationPressure;
    private double stress;

    public HomeostasisSignal() {
        setEventType("MOVEMENT_HOMEOSTASIS");
        this.loop = 2;
    }

    public HomeostasisSignal(double fatigue, double explorationPressure, double stress, long tick) {
        this();
        this.fatigue = fatigue;
        this.explorationPressure = explorationPressure;
        this.stress = stress;
        setTick(tick);
    }

    public double getFatigue() { return fatigue; }
    public void setFatigue(double fatigue) { this.fatigue = fatigue; }
    public double getExplorationPressure() { return explorationPressure; }
    public void setExplorationPressure(double explorationPressure) { this.explorationPressure = explorationPressure; }
    public double getStress() { return stress; }
    public void setStress(double stress) { this.stress = stress; }
}
