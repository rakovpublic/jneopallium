package com.rakovpublic.jneuropallium.master.controllers;

import com.rakovpublic.jneuropallium.master.configs.PropertyHolder;
import com.rakovpublic.jneuropallium.master.model.ConfigurationUpdateRequest;
import com.rakovpublic.jneuropallium.master.model.UploadSignalsRequest;
import com.rakovpublic.jneuropallium.master.services.IInputService;
import com.rakovpublic.jneuropallium.master.services.StorageService;
import com.rakovpublic.jneuropallium.master.services.impl.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/configuration")
public class ConfigurationController {

    private IInputService inputService;
    private StorageService storageService;

    @Autowired
    public ConfigurationController(IInputService inputService, StorageService storageService) {
        this.inputService = inputService;
        this.storageService = storageService;
    }

    @PostMapping("/update")
    public void update(@RequestParam("file") MultipartFile file) {
        String configurationPath = storageService.store(file);
        PropertyHolder.getPropertyHolder().updateConfig(configurationPath);
    }

    @PostMapping("/callback")
    public ResponseEntity<?> persistCallback(@RequestBody UploadSignalsRequest request) {
        try {
            inputService.processCallBackFromUpstream(request.getSignals());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e);
        }
        return ResponseEntity.ok().build();

    }
}
