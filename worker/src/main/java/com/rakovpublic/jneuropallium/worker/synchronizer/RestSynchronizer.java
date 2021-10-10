package com.rakovpublic.jneuropallium.worker.synchronizer;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class RestSynchronizer implements ISynchronizer {
    private IContext context;
    private final String USER_AGENT = "Mozilla/5.0";
    private final String isProcessedEndPointName = "rest.isprocessed";

    //private final String USER_AGENT = "Mozilla/5.0";
    //private final String USER_AGENT = "Mozilla/5.0";
    @Override
    public IContext getContext(int nodeId) {
        return null;
    }

    public RestSynchronizer() {
    }

    @Override
    public void syncSignal(ISignal signal, int layerId, long neuronId) {

    }

    @Override
    public boolean isLayerProcessed(int layerId) {
        String requestResult = "false";
        try {
            URL obj = new URL(context.getProperty(isProcessedEndPointName) + "?layer=" + layerId);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // optional default is GET
            con.setRequestMethod("GET");

            //add request header
            con.setRequestProperty("User-Agent", USER_AGENT);

            int responseCode = con.getResponseCode();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            if (responseCode == 200) {
                requestResult = response.toString();
            } else {
                //TODO: add logging
            }
        } catch (Exception e) {

        }

        return requestResult.contains("true");
    }

    @Override
    public int getNextLayerId() {
        return 0;
    }

    @Override
    public ILayerMeta getNextBatch() {
        return null;
    }

    @Override
    public void syncNeurons(List<? extends INeuron> neurons, int layerId) {

    }

    //TODO:Move next 4 methods and studying to master
    @Override
    public void removeNeuron(int layerId, long neuronId) {

    }

    @Override
    public void updateNeuron(INeuron neuron, int layerId) {

    }

    @Override
    public void addNeuron(INeuron neuron, long layerId) {

    }

    @Override
    public void addLayer(int afterLayerId) {

    }

    @Override
    public void putSignals() {

    }
}
