package com.rakovpublic.jneuropallium.master.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.master.model.CreateNeuronRequest;
import com.rakovpublic.jneuropallium.master.model.DeleteNeuronRequest;
import com.rakovpublic.jneuropallium.master.services.ConfigurationService;
import com.rakovpublic.jneuropallium.master.services.ILayerService;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/layer")
public class LayerController {

    private ILayerService layerService;
    private ConfigurationService configurationService;

    @Autowired
    public LayerController(ILayerService layerService, ConfigurationService configurationService) {
        this.layerService = layerService;
        this.configurationService = configurationService;
    }

    @PostMapping("/updateNeuron")
    public ResponseEntity<?> updateNeuron(@RequestBody CreateNeuronRequest request) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            INeuron neuron = (INeuron) mapper.readValue(request.getNeuronJson(), Class.forName(request.getNeuronClass()));
            layerService.deleteNeuron(request.getLayerId(), neuron.getId());
            layerService.addNeuron(neuron, request.getLayerId());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/deleteNeuron")
    public ResponseEntity<?> deleteNeuron(@RequestBody DeleteNeuronRequest request) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            layerService.deleteNeuron(request.getLayerId(), request.getNeuronId());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/createNeuron")
    public ResponseEntity<?> addNeuron(@RequestBody CreateNeuronRequest request) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            INeuron neuron = (INeuron) mapper.readValue(request.getNeuronJson(), Class.forName(request.getNeuronClass()));
            layerService.addNeuron(neuron, request.getLayerId());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/deletelayer")
    public ResponseEntity<?> deleteLayer(@RequestParam Integer layerId) {
        try {
            layerService.deleteLayer(layerId,configurationService.getReconnectionStrategy());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e);
        }
        return ResponseEntity.ok().build();
    }




}
