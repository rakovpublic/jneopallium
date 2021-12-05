package com.rakovpublic.jneuropallium.master.controllers;

import com.rakovpublic.jneuropallium.master.model.InputRegistrationRequest;
import com.rakovpublic.jneuropallium.master.model.UploadSignalsRequest;
import com.rakovpublic.jneuropallium.master.services.IInputService;
import com.rakovpublic.jneuropallium.master.services.MasterContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//TODO: add status code sending

@RestController
@RequestMapping("/input")
public class InputController {
    @Autowired
    private IInputService inputService;
    @PostMapping("/callback")
    public ResponseEntity<?> persistCallback(@RequestBody UploadSignalsRequest request){
        try {
            inputService.uploadWorkerResult(request.getName(),request.getSignals());
        }catch (Exception e){
            return ResponseEntity.internalServerError().body(e);
        }
        return ResponseEntity.ok().build();

    }
    @PostMapping("/register")
    public ResponseEntity<?> registerInput(@RequestBody InputRegistrationRequest request){
        try {
            inputService.register(request);
        }catch (Exception e){
            return ResponseEntity.internalServerError().body(e);
        }
        return ResponseEntity.ok().build();

    }


}
