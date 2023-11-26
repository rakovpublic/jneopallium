package com.rakovpublic.jneuropallium.master.controllers;

import com.rakovpublic.jneuropallium.master.configs.PropertyHolder;
import com.rakovpublic.jneuropallium.master.services.ConfigurationService;
import com.rakovpublic.jneuropallium.master.services.StorageService;
import com.rakovpublic.jneuropallium.worker.model.ConfigurationUpdateRequest;
import com.rakovpublic.jneuropallium.worker.model.UploadSignalsRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/configuration")
public class ConfigurationController {

    private ConfigurationService configurationService;
    private StorageService storageService;

    @Autowired
    public ConfigurationController(ConfigurationService configurationService, StorageService storageService) {
        this.configurationService = configurationService;
        this.storageService = storageService;
    }


    @PostMapping("/update")
    public void update(@RequestParam("config") ConfigurationUpdateRequest configurationUpdateRequest, @RequestParam("layersMetaPath") MultipartFile layersMetaPath) {
        String configurationPath = storageService.store(layersMetaPath);
        configurationUpdateRequest.setLayersMetaPath(configurationPath);
        configurationService.update(configurationUpdateRequest);
    }

    @PostMapping("/configapp")
    public void configApp(@RequestParam("config") MultipartFile config) {
        String configurationPath = storageService.store(config);
        PropertyHolder.getPropertyHolder().updateConfig(configurationPath);
    }

    @PostMapping("/callback")
    public ResponseEntity<?> persistCallback(@RequestBody UploadSignalsRequest request) {
        try {
            configurationService.getInputService().processCallBackFromUpstream(request.getSignals());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e);
        }
        return ResponseEntity.ok().build();

    }
}
