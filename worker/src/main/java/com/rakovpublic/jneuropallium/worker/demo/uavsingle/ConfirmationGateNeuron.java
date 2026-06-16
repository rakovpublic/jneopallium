package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

public class ConfirmationGateNeuron {
    private final ConfirmationProcessor processor = new ConfirmationProcessor();

    public ConfirmationEvaluation evaluate(TargetConfirmationRequestSignal request,
                                           TargetConfirmationResponseSignal response,
                                           long tick) {
        return processor.evaluate(request, response, tick);
    }
}

