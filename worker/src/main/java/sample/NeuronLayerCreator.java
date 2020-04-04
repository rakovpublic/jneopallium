package sample;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.neuron.impl.Axon;
import net.neuron.impl.NeuronConnection;
import net.signals.ISignal;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class NeuronLayerCreator {
    private static ObjectMapper mapper= new ObjectMapper();
    public static String getFirstLayer(){
        SimpleDoubleWeight fWeight= new SimpleDoubleWeight(1d);
        SimpleNeuron first= new SimpleNeuron();
        first.setId(0l);
        Axon firstAxon= new Axon();
        NeuronConnection<SimpleSignal> neuronConnectionF= new NeuronConnection<SimpleSignal>(1,0,0l,0l,fWeight,"test");
        NeuronConnection neuronConnectionS= new NeuronConnection<SimpleSignal>(1,0,1l,0l,fWeight,"test");
        NeuronConnection neuronConnectionT= new NeuronConnection<SimpleSignal>(1,0,2l,0l,fWeight,"test");
        firstAxon.putConnection(SimpleSignal.class,neuronConnectionF);
        firstAxon.putConnection(SimpleSignal.class,neuronConnectionS);
        firstAxon.putConnection(SimpleSignal.class,neuronConnectionT);
        first.setBias(1d);
        first.setBiasWeight(1d);
        first.setAxon(firstAxon);

        SimpleNeuron second= new SimpleNeuron();
        Axon secondAxon= new Axon();
        second.setId(1l);
        NeuronConnection neuronConnectionFS= new NeuronConnection<SimpleSignal>(1,0, 0L,1l,fWeight,"test");
        NeuronConnection neuronConnectionSS= new NeuronConnection<SimpleSignal>(1,0,1l,1l,fWeight,"test");
        NeuronConnection neuronConnectionTS= new NeuronConnection<SimpleSignal>(1,0,2l,1l,fWeight,"test");
        secondAxon.putConnection(SimpleSignal.class,neuronConnectionFS);
        secondAxon.putConnection(SimpleSignal.class,neuronConnectionSS);
        secondAxon.putConnection(SimpleSignal.class,neuronConnectionTS);
        second.setBias(1d);
        second.setBiasWeight(1d);
        second.setAxon(secondAxon);


        SimpleNeuron third= new SimpleNeuron();
        Axon thirdAxon= new Axon();
        third.setId(2l);
        NeuronConnection<SimpleSignal>  neuronConnectionFT= new NeuronConnection<SimpleSignal>(1,0,0l,2l,fWeight,"test");
        NeuronConnection<SimpleSignal>  neuronConnectionST= new NeuronConnection<SimpleSignal>(1,0,1l,2l,fWeight,"test");
        NeuronConnection<SimpleSignal>  neuronConnectionTT= new NeuronConnection<SimpleSignal>(1,0,2l,2l,fWeight,"test");
        thirdAxon.putConnection(SimpleSignal.class,neuronConnectionFT);
        thirdAxon.putConnection(SimpleSignal.class,neuronConnectionST);
        thirdAxon.putConnection(SimpleSignal.class,neuronConnectionTT);
        third.setBias(1d);
        third.setBiasWeight(1d);
        third.setAxon(thirdAxon);

        first.addSignalMerger(SimpleSignal.class, new SimpleSignalMerger());
        second.addSignalMerger(SimpleSignal.class, new SimpleSignalMerger());
        third.addSignalMerger(SimpleSignal.class, new SimpleSignalMerger());

        SimpleNeuron[] result={first,second,third};
        try {
            return mapper.writeValueAsString(Arrays.asList(result));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getSecondLayerLayer(){
        SimpleDoubleWeight fWeight= new SimpleDoubleWeight(1d);
        SimpleNeuron first= new SimpleNeuron();
        first.setId(0l);
        Axon firstAxon= new Axon();
        NeuronConnection<SimpleSignal> neuronConnectionF= new NeuronConnection<SimpleSignal>(2,1,0l,0l,fWeight,"test");
        NeuronConnection<SimpleSignal>  neuronConnectionS= new NeuronConnection<SimpleSignal>(2,1,1l,0l,fWeight,"test");
        NeuronConnection<SimpleSignal>  neuronConnectionT= new NeuronConnection<SimpleSignal>(2,1,2l,0l,fWeight,"test");
        firstAxon.putConnection(SimpleSignal.class,neuronConnectionF);
        firstAxon.putConnection(SimpleSignal.class,neuronConnectionS);
        firstAxon.putConnection(SimpleSignal.class,neuronConnectionT);
        first.setBias(1d);
        first.setBiasWeight(1d);
        first.setAxon(firstAxon);

        SimpleNeuron second= new SimpleNeuron();
        Axon secondAxon= new Axon();
        second.setId(1l);
        NeuronConnection<SimpleSignal>  neuronConnectionFS= new NeuronConnection<SimpleSignal>(2,1, 0L,1l,fWeight,"test");
        NeuronConnection<SimpleSignal>  neuronConnectionSS= new NeuronConnection<SimpleSignal>(2,1,1l,1l,fWeight,"test");
        NeuronConnection<SimpleSignal>  neuronConnectionTS= new NeuronConnection<SimpleSignal>(2,1,2l,1l,fWeight,"test");
        secondAxon.putConnection(SimpleSignal.class,neuronConnectionFS);
        secondAxon.putConnection(SimpleSignal.class,neuronConnectionSS);
        secondAxon.putConnection(SimpleSignal.class,neuronConnectionTS);
        second.setBias(1d);
        second.setBiasWeight(1d);
        second.setAxon(secondAxon);


        SimpleNeuron third= new SimpleNeuron();
        Axon thirdAxon= new Axon();
        third.setId(2l);
        NeuronConnection<SimpleSignal>  neuronConnectionFT= new NeuronConnection<SimpleSignal>(2,1,0l,2l,fWeight,"test");
        NeuronConnection<SimpleSignal>  neuronConnectionST= new NeuronConnection<SimpleSignal>(2,1,1l,2l,fWeight,"test");
        NeuronConnection<SimpleSignal>  neuronConnectionTT= new NeuronConnection<SimpleSignal>(2,1,2l,2l,fWeight,"test");
        thirdAxon.putConnection(SimpleSignal.class,neuronConnectionFT);
        thirdAxon.putConnection(SimpleSignal.class,neuronConnectionST);
        thirdAxon.putConnection(SimpleSignal.class,neuronConnectionTT);
        third.setBias(1d);
        third.setBiasWeight(1d);
        third.setAxon(thirdAxon);

        first.addSignalMerger(SimpleSignal.class, new SimpleSignalMerger());
        second.addSignalMerger(SimpleSignal.class, new SimpleSignalMerger());
        third.addSignalMerger(SimpleSignal.class, new SimpleSignalMerger());

        SimpleNeuron[] result={first,second,third};
        try {
            return mapper.writeValueAsString(Arrays.asList(result));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "";

    }
    public static String getResultLayerLayer(){
        SimpleDoubleWeight fWeight= new SimpleDoubleWeight(1d);
        SimpleResultNeuron first= new SimpleResultNeuron();
        first.setId(0l);
        Axon firstAxon= new Axon();
        first.setBias(1d);
        first.setBiasWeight(1d);
        first.setAxon(firstAxon);

        SimpleResultNeuron second= new SimpleResultNeuron();
        Axon secondAxon= new Axon();
        second.setId(1l);
        second.setBias(1d);
        second.setBiasWeight(1d);
        second.setAxon(secondAxon);


        SimpleResultNeuron third= new SimpleResultNeuron();
        Axon thirdAxon= new Axon();
        third.setId(2l);
        third.setBias(1d);
        third.setBiasWeight(1d);
        third.setAxon(thirdAxon);

        first.addSignalMerger(SimpleSignal.class, new SimpleSignalMerger());
        second.addSignalMerger(SimpleSignal.class, new SimpleSignalMerger());
        third.addSignalMerger(SimpleSignal.class, new SimpleSignalMerger());

        SimpleNeuron[] result={first,second,third};
        try {
            return mapper.writeValueAsString(Arrays.asList(result));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String createInput(){
        HashMap<Long, List<ISignal>> signals= new HashMap<>();
        List<ISignal> sListF= new LinkedList<>();
        sListF.add(new SimpleSignal(1d,1,0,0L));
        sListF.add(new SimpleSignal(1d,1,0,0L));
        List<ISignal> sListS= new LinkedList<>();
        sListS.add(new SimpleSignal(1d,1,0,1L));
        sListS.add(new SimpleSignal(1d,1,0,1L));
        List<ISignal> sListT= new LinkedList<>();
        sListT.add(new SimpleSignal(1d,1,0,2L));
        sListT.add(new SimpleSignal(1d,1,0,2L));
        signals.put(0l,sListF);
        signals.put(1l,sListS);
        signals.put(2l,sListT);
        StringBuilder resultJson = new StringBuilder();
        ObjectMapper mapper= new ObjectMapper();
        resultJson.append("{\"inputs\":[");
        for (Long nrId : signals.keySet()) {
            StringBuilder signal = new StringBuilder();
            signal.append("{\"neuronId\":\"");
            signal.append(nrId);
            signal.append("\",\"signal\":[");
            for (ISignal s : signals.get(nrId)) {
                String serializedSignal= null;
                try {
                    serializedSignal = mapper.writeValueAsString(s);
                    if(serializedSignal!=null){
                        signal.append(serializedSignal);
                        signal.append(",");
                    }
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    //TODO: add logging
                }

            }
            if(signals.get(nrId).size()>0){
                signal.deleteCharAt(signal.length() - 1);
            }
            signal.append("]},");
            resultJson.append(signal.toString());
        }
        resultJson.deleteCharAt(resultJson.length() - 1);
        resultJson.append("]}");
        return resultJson.toString();
    }

    public static String getDesiredResult(){
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(new SimpleResult());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

}
