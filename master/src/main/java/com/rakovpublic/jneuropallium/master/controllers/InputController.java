package com.rakovpublic.jneuropallium.master.controllers;

import com.rakovpublic.jneuropallium.master.model.InputRegistrationRequest;
import com.rakovpublic.jneuropallium.master.model.UploadSignalsRequest;
import com.rakovpublic.jneuropallium.master.services.MasterContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InputController {
    @Autowired
    private MasterContext context;
    @PostMapping("/callback")
    public void persistCallback(@RequestParam UploadSignalsRequest request){
        context.getInputService().uploadWorkerResult(request.getName(),request.getSignals());

    }
    @PostMapping("/registerInput")
    public void registerInput(@RequestParam InputRegistrationRequest request){
        context.registerInput(request);

    }


}
