/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals;

public class InputStatusMeta {
    private Boolean status;
    private Boolean mandatoryUpdated;
    private Integer currentRuns;
    private final String name;
    private Boolean beenUsed;

    public InputStatusMeta(Boolean status, Boolean mandatoryUpdated, String name) {
        this.status = status;
        this.mandatoryUpdated = mandatoryUpdated;
        currentRuns = 0;
        this.name = name;
        beenUsed = false;
    }


    public Boolean isBeenUsed() {
        return beenUsed;
    }

    public void setBeenUsed(Boolean beenUsed) {
        this.beenUsed = beenUsed;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public Boolean getMandatoryUpdated() {
        return mandatoryUpdated;
    }

    public void setMandatoryUpdated(Boolean mandatoryUpdated) {
        this.mandatoryUpdated = mandatoryUpdated;
    }


    public Integer getCurrentRuns() {
        return currentRuns;
    }

    public void setCurrentRuns(Integer currentRuns) {
        this.currentRuns = currentRuns;
    }

    public String getName() {
        return name;
    }
}
