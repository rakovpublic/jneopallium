package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.BatchStateSignal;

public interface ICampaignPlanningNeuron extends IModulatableNeuron {
    void enqueueCampaign(String campaignId, BatchStateSignal target);
    String currentCampaign();
    BatchStateSignal nextPhase();
    int queuedCampaigns();
}
