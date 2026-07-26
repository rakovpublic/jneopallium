package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

public class UavMissionNeuron {
    private final UavMissionStateMachine stateMachine = new UavMissionStateMachine();

    public UavMissionState state() {
        return stateMachine.state();
    }

    public UavMissionState transition(UavMissionState next) {
        return stateMachine.transition(next);
    }
}

