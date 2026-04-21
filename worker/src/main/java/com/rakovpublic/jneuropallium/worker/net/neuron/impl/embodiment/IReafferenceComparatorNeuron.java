package com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.signals.slow.HarmFeedbackSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.EfferenceCopySignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.ProprioceptiveSignal;
import java.util.HashMap;
import java.util.Map;

public interface IReafferenceComparatorNeuron extends IModulatableNeuron, IEmbodied {
    void registerEfferenceCopy(EfferenceCopySignal s);
    BodySchema currentSchema();
    void onProprioceptive(ProprioceptiveSignal p);
    HarmFeedbackSignal maybeEmitFailure(String actionPlanId);
    double getLastMismatch();
    double getMismatchThreshold();
    void setMismatchThreshold(double mismatchThreshold);
    double getFailureEmitThreshold();
    void setFailureEmitThreshold(double failureEmitThreshold);
    int pendingCount();
}
