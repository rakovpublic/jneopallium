package com.rakovpublic.jneuropallium.ai.signals.fast;

import com.rakovpublic.jneuropallium.ai.signals.BaseSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class ConsequenceSimulationSignal extends BaseSignal {
    private String actionPlanId;
    private double[] projectedStateVector;
    private int simulationStep;
    private double[] humanStateImpact; // length=5: 0=physicalIntegrity,1=autonomy,2=resource,3=information,4=emotional

    public ConsequenceSimulationSignal() { super(); this.loop = 1; this.epoch = 2L; }
    public ConsequenceSimulationSignal(String actionPlanId, double[] projectedStateVector, int simulationStep, double[] humanStateImpact) {
        this(); this.actionPlanId = actionPlanId; this.projectedStateVector = projectedStateVector;
        this.simulationStep = simulationStep; this.humanStateImpact = humanStateImpact;
    }

    public String getActionPlanId() { return actionPlanId; }
    public void setActionPlanId(String actionPlanId) { this.actionPlanId = actionPlanId; }
    public double[] getProjectedStateVector() { return projectedStateVector; }
    public void setProjectedStateVector(double[] v) { this.projectedStateVector = v; }
    public int getSimulationStep() { return simulationStep; }
    public void setSimulationStep(int simulationStep) { this.simulationStep = simulationStep; }
    public double[] getHumanStateImpact() { return humanStateImpact; }
    public void setHumanStateImpact(double[] humanStateImpact) { this.humanStateImpact = humanStateImpact; }

    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return ConsequenceSimulationSignal.class; }
    @Override public String getDescription() { return "ConsequenceSimulationSignal"; }
    @Override public <K extends ISignal<Void>> K copySignal() {
        ConsequenceSimulationSignal c = new ConsequenceSimulationSignal(actionPlanId, projectedStateVector, simulationStep, humanStateImpact);
        c.sourceLayer = this.sourceLayer; c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop; c.epoch = this.epoch; c.name = this.name;
        return (K) c;
    }
}
