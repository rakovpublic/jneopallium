package sample;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.neuron.impl.Axon;
import com.rakovpublic.jneuropallium.worker.neuron.impl.NeuronSynapse;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class NeuronLayerCreator {
    private static ObjectMapper mapper = new ObjectMapper();

    public static String getFirstLayer() {
        SimpleDoubleWeight fWeight = new SimpleDoubleWeight(1d);
        SimpleNeuron first = new SimpleNeuron();
        first.setId(0l);
        Axon firstAxon = new Axon();
        NeuronSynapse<SimpleSignal> neuronSynapseF = new NeuronSynapse<SimpleSignal>(1, 0, 0l, 0l, fWeight, "test");
        NeuronSynapse neuronSynapseS = new NeuronSynapse<SimpleSignal>(1, 0, 1l, 0l, fWeight, "test");
        NeuronSynapse neuronSynapseT = new NeuronSynapse<SimpleSignal>(1, 0, 2l, 0l, fWeight, "test");
        firstAxon.putConnection(SimpleSignal.class, neuronSynapseF);
        firstAxon.putConnection(SimpleSignal.class, neuronSynapseS);
        firstAxon.putConnection(SimpleSignal.class, neuronSynapseT);
        first.setBias(1d);
        first.setBiasWeight(1d);
        first.setAxon(firstAxon);

        SimpleNeuron second = new SimpleNeuron();
        Axon secondAxon = new Axon();
        second.setId(1l);
        NeuronSynapse neuronSynapseFS = new NeuronSynapse<SimpleSignal>(1, 0, 0L, 1l, fWeight, "test");
        NeuronSynapse neuronSynapseSS = new NeuronSynapse<SimpleSignal>(1, 0, 1l, 1l, fWeight, "test");
        NeuronSynapse neuronSynapseTS = new NeuronSynapse<SimpleSignal>(1, 0, 2l, 1l, fWeight, "test");
        secondAxon.putConnection(SimpleSignal.class, neuronSynapseFS);
        secondAxon.putConnection(SimpleSignal.class, neuronSynapseSS);
        secondAxon.putConnection(SimpleSignal.class, neuronSynapseTS);
        second.setBias(1d);
        second.setBiasWeight(1d);
        second.setAxon(secondAxon);


        SimpleNeuron third = new SimpleNeuron();
        Axon thirdAxon = new Axon();
        third.setId(2l);
        NeuronSynapse<SimpleSignal> neuronSynapseFT = new NeuronSynapse<SimpleSignal>(1, 0, 0l, 2l, fWeight, "test");
        NeuronSynapse<SimpleSignal> neuronSynapseST = new NeuronSynapse<SimpleSignal>(1, 0, 1l, 2l, fWeight, "test");
        NeuronSynapse<SimpleSignal> neuronSynapseTT = new NeuronSynapse<SimpleSignal>(1, 0, 2l, 2l, fWeight, "test");
        thirdAxon.putConnection(SimpleSignal.class, neuronSynapseFT);
        thirdAxon.putConnection(SimpleSignal.class, neuronSynapseST);
        thirdAxon.putConnection(SimpleSignal.class, neuronSynapseTT);
        third.setBias(1d);
        third.setBiasWeight(1d);
        third.setAxon(thirdAxon);

        first.addSignalMerger(SimpleSignal.class, new SimpleSignalMerger());
        second.addSignalMerger(SimpleSignal.class, new SimpleSignalMerger());
        third.addSignalMerger(SimpleSignal.class, new SimpleSignalMerger());

        first.addSignalProcessor(SimpleSignal.class, new SimpleSignalProcessor());
        second.addSignalProcessor(SimpleSignal.class, new SimpleSignalProcessor());
        third.addSignalProcessor(SimpleSignal.class, new SimpleSignalProcessor());


        first.setProcessingChain(new SimpleSignalChain());
        second.setProcessingChain(new SimpleSignalChain());
        third.setProcessingChain(new SimpleSignalChain());

        SimpleNeuron[] result = {first, second, third};
        try {
            return mapper.writeValueAsString(Arrays.asList(result));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getSecondLayerLayer() {
        SimpleDoubleWeight fWeight = new SimpleDoubleWeight(1d);
        SimpleNeuron first = new SimpleNeuron();
        first.setId(0l);
        Axon firstAxon = new Axon();
        NeuronSynapse<SimpleSignal> neuronSynapseF = new NeuronSynapse<SimpleSignal>(2, 1, 0l, 0l, fWeight, "test");
        NeuronSynapse<SimpleSignal> neuronSynapseS = new NeuronSynapse<SimpleSignal>(2, 1, 1l, 0l, fWeight, "test");
        NeuronSynapse<SimpleSignal> neuronSynapseT = new NeuronSynapse<SimpleSignal>(2, 1, 2l, 0l, fWeight, "test");
        firstAxon.putConnection(SimpleSignal.class, neuronSynapseF);
        firstAxon.putConnection(SimpleSignal.class, neuronSynapseS);
        firstAxon.putConnection(SimpleSignal.class, neuronSynapseT);
        first.setBias(1d);
        first.setBiasWeight(1d);
        first.setAxon(firstAxon);

        SimpleNeuron second = new SimpleNeuron();
        Axon secondAxon = new Axon();
        second.setId(1l);
        NeuronSynapse<SimpleSignal> neuronSynapseFS = new NeuronSynapse<SimpleSignal>(2, 1, 0L, 1l, fWeight, "test");
        NeuronSynapse<SimpleSignal> neuronSynapseSS = new NeuronSynapse<SimpleSignal>(2, 1, 1l, 1l, fWeight, "test");
        NeuronSynapse<SimpleSignal> neuronSynapseTS = new NeuronSynapse<SimpleSignal>(2, 1, 2l, 1l, fWeight, "test");
        secondAxon.putConnection(SimpleSignal.class, neuronSynapseFS);
        secondAxon.putConnection(SimpleSignal.class, neuronSynapseSS);
        secondAxon.putConnection(SimpleSignal.class, neuronSynapseTS);
        second.setBias(1d);
        second.setBiasWeight(1d);
        second.setAxon(secondAxon);


        SimpleNeuron third = new SimpleNeuron();
        Axon thirdAxon = new Axon();
        third.setId(2l);
        NeuronSynapse<SimpleSignal> neuronSynapseFT = new NeuronSynapse<SimpleSignal>(2, 1, 0l, 2l, fWeight, "test");
        NeuronSynapse<SimpleSignal> neuronSynapseST = new NeuronSynapse<SimpleSignal>(2, 1, 1l, 2l, fWeight, "test");
        NeuronSynapse<SimpleSignal> neuronSynapseTT = new NeuronSynapse<SimpleSignal>(2, 1, 2l, 2l, fWeight, "test");
        thirdAxon.putConnection(SimpleSignal.class, neuronSynapseFT);
        thirdAxon.putConnection(SimpleSignal.class, neuronSynapseST);
        thirdAxon.putConnection(SimpleSignal.class, neuronSynapseTT);
        third.setBias(1d);
        third.setBiasWeight(1d);
        third.setAxon(thirdAxon);

        first.addSignalMerger(SimpleSignal.class, new SimpleSignalMerger());
        second.addSignalMerger(SimpleSignal.class, new SimpleSignalMerger());
        third.addSignalMerger(SimpleSignal.class, new SimpleSignalMerger());
        first.addSignalProcessor(SimpleSignal.class, new SimpleSignalProcessor());

        second.addSignalProcessor(SimpleSignal.class, new SimpleSignalProcessor());
        third.addSignalProcessor(SimpleSignal.class, new SimpleSignalProcessor());

        first.setProcessingChain(new SimpleSignalChain());
        second.setProcessingChain(new SimpleSignalChain());
        third.setProcessingChain(new SimpleSignalChain());

        SimpleNeuron[] result = {first, second, third};
        try {
            return mapper.writeValueAsString(Arrays.asList(result));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "";

    }

    public static String getResultLayerLayer() {
        SimpleDoubleWeight fWeight = new SimpleDoubleWeight(1d);
        SimpleResultNeuron first = new SimpleResultNeuron();
        first.setId(0l);
        Axon firstAxon = new Axon();
        first.setBias(1d);
        first.setBiasWeight(1d);
        first.setAxon(firstAxon);

        SimpleResultNeuron second = new SimpleResultNeuron();
        Axon secondAxon = new Axon();
        second.setId(1l);
        second.setBias(1d);
        second.setBiasWeight(1d);
        second.setAxon(secondAxon);


        SimpleResultNeuron third = new SimpleResultNeuron();
        Axon thirdAxon = new Axon();
        third.setId(2l);
        third.setBias(1d);
        third.setBiasWeight(1d);
        third.setAxon(thirdAxon);

        first.addSignalMerger(SimpleSignal.class, new SimpleSignalMerger());
        second.addSignalMerger(SimpleSignal.class, new SimpleSignalMerger());
        third.addSignalMerger(SimpleSignal.class, new SimpleSignalMerger());

        first.addSignalProcessor(SimpleSignal.class, new SimpleSignalProcessor());
        second.addSignalProcessor(SimpleSignal.class, new SimpleSignalProcessor());
        third.addSignalProcessor(SimpleSignal.class, new SimpleSignalProcessor());

        first.setProcessingChain(new SimpleSignalChain());
        second.setProcessingChain(new SimpleSignalChain());
        third.setProcessingChain(new SimpleSignalChain());

        SimpleNeuron[] result = {first, second, third};
        try {
            return mapper.writeValueAsString(Arrays.asList(result));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String createInput() {
        HashMap<Long, List<ISignal>> signals = new HashMap<>();
        List<ISignal> sListF = new LinkedList<>();
        sListF.add(new SimpleSignal(1d, 1, 0, 0L));
        sListF.add(new SimpleSignal(1d, 1, 0, 0L));
        List<ISignal> sListS = new LinkedList<>();
        sListS.add(new SimpleSignal(1d, 1, 0, 1L));
        sListS.add(new SimpleSignal(1d, 1, 0, 1L));
        List<ISignal> sListT = new LinkedList<>();
        sListT.add(new SimpleSignal(1d, 1, 0, 2L));
        sListT.add(new SimpleSignal(1d, 1, 0, 2L));
        signals.put(0l, sListF);
        signals.put(1l, sListS);
        signals.put(2l, sListT);
        StringBuilder resultJson = new StringBuilder();
        ObjectMapper mapper = new ObjectMapper();
        resultJson.append("{\"inputs\":[");
        for (Long nrId : signals.keySet()) {
            StringBuilder signal = new StringBuilder();
            signal.append("{\"neuronId\":\"");
            signal.append(nrId);
            signal.append("\",\"signal\":[");
            for (ISignal s : signals.get(nrId)) {
                String serializedSignal = null;
                try {
                    serializedSignal = mapper.writeValueAsString(s);
                    if (serializedSignal != null) {
                        signal.append(serializedSignal);
                        signal.append(",");
                    }
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    //TODO: add logging
                }

            }
            if (signals.get(nrId).size() > 0) {
                signal.deleteCharAt(signal.length() - 1);
            }
            signal.append("]},");
            resultJson.append(signal.toString());
        }
        resultJson.deleteCharAt(resultJson.length() - 1);
        resultJson.append("]}");
        return resultJson.toString();
    }

    public static String getDesiredResult() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(new SimpleResult());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

}
