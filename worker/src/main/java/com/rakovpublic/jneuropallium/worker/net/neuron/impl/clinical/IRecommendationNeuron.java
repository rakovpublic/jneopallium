package com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.TreatmentProposalSignal;

import java.util.List;

public interface IRecommendationNeuron extends IModulatableNeuron {
    void setContraindicationFilter(IContraindicationNeuron c);
    void setTopK(int k);
    int getTopK();
    void setMinBenefitMinusRisk(double v);
    double getMinBenefitMinusRisk();
    String getMode();
    List<RecommendationNeuron.Recommendation> evaluate(List<TreatmentProposalSignal> candidates);
    List<RecommendationNeuron.Recommendation> recommend(List<TreatmentProposalSignal> candidates);
    List<RecommendationNeuron.Recommendation> rejected(List<TreatmentProposalSignal> candidates);
}
