package web.study;

import web.neuron.INeuron;

import java.util.List;

/**
 * Created by Rakovskyi Dmytro on 27.10.2017.
 */
public interface IStudyingProcessor<N extends INeuron> {
    void study();
    IStudyingRequest<N> prepareRequest(N neuron);
    void register(N neuron);
    void addRequest(IStudyingRequest<N> studyingRequest);
    void addRequests(List<IStudyingRequest<N>> studyingRequests);
}
