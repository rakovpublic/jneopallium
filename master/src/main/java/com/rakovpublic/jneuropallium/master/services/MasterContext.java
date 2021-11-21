package com.rakovpublic.jneuropallium.master.services;

import com.rakovpublic.jneuropallium.worker.neuron.INeuron;

public class MasterContext {
    private static MasterContext masterContext = new MasterContext();
    private MasterContext(){

    }
    public static MasterContext getMasterContext(){
        return masterContext;
    }

    public IInputService getInputService(){
        return null;
    }
    public INeuron getNeuron(Integer layerId,Long neuronId){
        return null;
    }
    public void updateNeuron(){}
    public void deleteNeuron(){}
    public void addNeuron(){}
    public void loadClass(){}
    public void getJars(){}
}
