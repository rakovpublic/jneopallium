package com.rakovpublic.jneuropallium.master.services;

import com.rakovpublic.jneuropallium.master.model.InputRegistrationRequest;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;
import org.springframework.stereotype.Component;

@Component
public class MasterContext {
    //private static MasterContext masterContext = new MasterContext();
    private IInputService inputService;
    public MasterContext(){
        init();
    }
    private void init(){
        //TODO: add initialization
    }
    /*public static MasterContext getMasterContext(){
        return masterContext;
    }*/

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
    public void registerInput(InputRegistrationRequest request){
        //TODO: add request parsing
        //inputService.register();
    }
}
