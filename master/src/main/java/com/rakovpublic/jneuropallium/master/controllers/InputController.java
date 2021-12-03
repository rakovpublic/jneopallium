package com.rakovpublic.jneuropallium.master.controllers;

import com.rakovpublic.jneuropallium.master.model.InputRegistrationRequest;
import com.rakovpublic.jneuropallium.master.model.UploadSignalsRequest;
import com.rakovpublic.jneuropallium.master.services.MasterContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

//TODO: add status code sending

@RestController
@RequestMapping("/input")
public class InputController {
    @Autowired
    private MasterContext context;
    @PostMapping("/callback")
    public void persistCallback(@RequestBody UploadSignalsRequest request){
        context.getInputService().uploadWorkerResult(request.getName(),request.getSignals());

    }
    @PostMapping("/register")
    public void registerInput(@RequestBody InputRegistrationRequest request){
        context.registerInput(request);

    }


}
