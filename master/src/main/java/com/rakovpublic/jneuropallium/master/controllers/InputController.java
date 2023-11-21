package com.rakovpublic.jneuropallium.master.controllers;

import com.rakovpublic.jneuropallium.worker.model.InputRegistrationRequest;
import com.rakovpublic.jneuropallium.worker.model.UploadSignalsRequest;
import com.rakovpublic.jneuropallium.master.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/input")
public class InputController {
    private ConfigurationService configurationService;

    @Autowired
    public InputController(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @PostMapping("/callback")
    public ResponseEntity<?> persistCallback(@RequestBody UploadSignalsRequest request) {
        try {
            if(!request.isDiscriminator()){
                configurationService.getInputService().uploadWorkerResult(request.getName(), request.getSignals());
            }else {

            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e);
        }
        return ResponseEntity.ok().build();

    }

    @PostMapping("/register")
    public ResponseEntity<?> registerInput(@RequestBody InputRegistrationRequest request) {
        try {
            configurationService.getInputService().register(request);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e);
        }
        return ResponseEntity.ok().build();

    }
}
