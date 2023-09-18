package com.rakovpublic.jneuropallium.master.controllers;

import com.rakovpublic.jneuropallium.master.model.InputRegistrationRequest;
import com.rakovpublic.jneuropallium.master.model.UploadSignalsRequest;
import com.rakovpublic.jneuropallium.master.services.ConfigurationService;
import com.rakovpublic.jneuropallium.master.services.IInputService;
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
            configurationService.getInputService().uploadWorkerResult(request.getName(), request.getSignals());
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
