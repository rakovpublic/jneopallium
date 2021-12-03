package com.rakovpublic.jneuropallium.master.services;

import com.rakovpublic.jneuropallium.master.model.CreateNeuronRequest;
import com.rakovpublic.jneuropallium.master.model.DeleteNeuronRequest;
import com.rakovpublic.jneuropallium.master.model.InputRegistrationRequest;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;
import org.springframework.stereotype.Component;

@Component
//TODO: refactore to services
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
    public void updateNeuron(CreateNeuronRequest request){}
    public void deleteNeuron(DeleteNeuronRequest request){}
    public void addNeuron(CreateNeuronRequest request){}
    public void loadClass(){}
    public void getJars(){}
    public void registerInput(InputRegistrationRequest request){
        //TODO: add request parsing
        //inputService.register();
    }
}
