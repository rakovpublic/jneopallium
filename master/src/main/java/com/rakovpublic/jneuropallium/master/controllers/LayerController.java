package com.rakovpublic.jneuropallium.master.controllers;

import com.rakovpublic.jneuropallium.master.model.CreateNeuronRequest;
import com.rakovpublic.jneuropallium.master.model.DeleteNeuronRequest;
import com.rakovpublic.jneuropallium.master.services.ILayerService;
import com.rakovpublic.jneuropallium.master.services.MasterContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//TODO: add status code sending
@RestController
@RequestMapping("/layer")
public class LayerController {
    @Autowired
    private MasterContext context;
    @PostMapping("/updateNeuron")
    public void updateNeuron(@RequestBody CreateNeuronRequest request){
        context.updateNeuron(request);
    }
    @PostMapping("/deleteNeuron")
    public void deleteNeuron(@RequestBody DeleteNeuronRequest request){
        context.deleteNeuron(request);
    }
    @PostMapping("/createNeuron")
    public void addNeuron(@RequestBody CreateNeuronRequest request){
        context.addNeuron(request);
    }
}
