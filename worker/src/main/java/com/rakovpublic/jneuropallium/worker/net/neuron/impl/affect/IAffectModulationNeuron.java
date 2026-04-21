package com.rakovpublic.jneuropallium.worker.net.neuron.impl.affect;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.affect.AffectStateSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.affect.AppraisalSignal;

public interface IAffectModulationNeuron extends IModulatableNeuron, IAffectiveNeuron {
    void onAffect(AffectStateSignal s);
    AffectState currentState();
    void modulateThreshold(double arousalFactor);
    void onAppraisal(AppraisalSignal s);
    double getShortTermLearningScale();
    double getLongTermConsolidationScale();
    double getExplorationBonus();
    double getHarmThresholdMultiplier();
    double getHarmClampMin();
    void setHarmClampMin(double harmClampMin);
    double getHarmClampMax();
    void setHarmClampMax(double harmClampMax);
    double getValence();
    double getArousal();
    double getFiringThreshold();
    double getBaselineThreshold();
}
