package com.rakovpublic.jneuropallium.master.controllers;

import com.rakovpublic.jneuropallium.master.model.ConfigurationUpdateRequest;
import com.rakovpublic.jneuropallium.master.model.UploadSignalsRequest;
import com.rakovpublic.jneuropallium.master.services.IInputService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/configuration")
public class ConfigurationController {
    @Autowired
    private IInputService inputService;
    @PostMapping("/update")
    public void update(@RequestBody ConfigurationUpdateRequest request){


    }

    @PostMapping("/callback")
    public ResponseEntity<?> persistCallback(@RequestBody UploadSignalsRequest request){
        try {
            inputService.processCallBackFromUpstream(request.getSignals());
        }catch (Exception e){
            return ResponseEntity.internalServerError().body(e);
        }
        return ResponseEntity.ok().build();

    }
}
