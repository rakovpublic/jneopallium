package com.rakovpublic.jneuropallium.ai.signals.fast;

import com.rakovpublic.jneuropallium.ai.signals.BaseSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class ConsequenceQuerySignal extends BaseSignal {
    private String actionPlanId;
    private MotorCommandSignal[] candidateActions;
    private int simulationHorizon;
    private String requestingNeuronId;

    public ConsequenceQuerySignal() { super(); this.loop = 1; this.epoch = 1L; }
    public ConsequenceQuerySignal(String actionPlanId, MotorCommandSignal[] candidateActions, int simulationHorizon, String requestingNeuronId) {
        this(); this.actionPlanId = actionPlanId; this.candidateActions = candidateActions;
        this.simulationHorizon = simulationHorizon; this.requestingNeuronId = requestingNeuronId;
    }

    public String getActionPlanId() { return actionPlanId; }
    public void setActionPlanId(String actionPlanId) { this.actionPlanId = actionPlanId; }
    public MotorCommandSignal[] getCandidateActions() { return candidateActions; }
    public void setCandidateActions(MotorCommandSignal[] candidateActions) { this.candidateActions = candidateActions; }
    public int getSimulationHorizon() { return simulationHorizon; }
    public void setSimulationHorizon(int simulationHorizon) { this.simulationHorizon = simulationHorizon; }
    public String getRequestingNeuronId() { return requestingNeuronId; }
    public void setRequestingNeuronId(String requestingNeuronId) { this.requestingNeuronId = requestingNeuronId; }

    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return ConsequenceQuerySignal.class; }
    @Override public String getDescription() { return "ConsequenceQuerySignal"; }
    @Override public <K extends ISignal<Void>> K copySignal() {
        ConsequenceQuerySignal c = new ConsequenceQuerySignal(actionPlanId, candidateActions, simulationHorizon, requestingNeuronId);
        c.sourceLayer = this.sourceLayer; c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop; c.epoch = this.epoch; c.name = this.name;
        return (K) c;
    }
}
