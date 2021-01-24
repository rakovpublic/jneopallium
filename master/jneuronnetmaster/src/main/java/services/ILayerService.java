package services;

import java.util.List;

public interface ILayerService {
    void deleteNeuron(Integer layerId, Long neuronId);
    void addNeuron(String neuronJson,Integer layerId);
    void mergeNeurons(Integer layerId, List<Long> neuronIds);
}
