package com.rakovpublic.jneuropallium.master.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.master.model.CreateNeuronRequest;
import com.rakovpublic.jneuropallium.master.model.DeleteNeuronRequest;
import com.rakovpublic.jneuropallium.master.services.ILayerService;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/layer")
public class LayerController {
    //TODO: add service implementation
    private ILayerService layerService;

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
            //TODO: add connections update
            layerService.deleteLayer(layerId);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e);
        }
        return ResponseEntity.ok().build();
    }




}
