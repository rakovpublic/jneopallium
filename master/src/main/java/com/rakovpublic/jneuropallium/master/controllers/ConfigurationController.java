package com.rakovpublic.jneuropallium.master.controllers;

import com.rakovpublic.jneuropallium.master.configs.PropertyHolder;
import com.rakovpublic.jneuropallium.master.services.ConfigurationService;
import com.rakovpublic.jneuropallium.master.services.StorageService;
import com.rakovpublic.jneuropallium.worker.model.ConfigurationUpdateRequest;
import com.rakovpublic.jneuropallium.worker.model.UploadSignalsRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/configuration")
public class ConfigurationController {
    private static final Logger logger = LogManager.getLogger(ConfigurationController.class);

    private ConfigurationService configurationService;
    private StorageService storageService;

    @Autowired
    public ConfigurationController(ConfigurationService configurationService, StorageService storageService) {
        this.configurationService = configurationService;
        this.storageService = storageService;
    }


    @PostMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> update(@RequestPart("config") ConfigurationUpdateRequest configurationUpdateRequest,
                                    @RequestPart("layersMetaPath") MultipartFile layersMetaPath) {
        try {
            String configurationPath = storageService.store(layersMetaPath);
            configurationUpdateRequest.setLayersMetaPath(configurationPath);
            configurationService.update(configurationUpdateRequest);
        } catch (Exception e) {
            logger.error("Cannot apply configuration", e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Configuration without any uploaded file. A net whose layers live in a shared store - Redis
     * for instance - carries connection coordinates instead of layer documents, so there is
     * nothing to upload and the multipart variant above only gets in the way.
     */
    @PostMapping(value = "/update", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> update(@RequestBody ConfigurationUpdateRequest configurationUpdateRequest) {
        try {
            configurationService.update(configurationUpdateRequest);
        } catch (Exception e) {
            logger.error("Cannot apply configuration", e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
        return ResponseEntity.ok().build();
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
