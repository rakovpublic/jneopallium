package com.rakovpublic.jneuropallium.master.controllers;

import com.rakovpublic.jneuropallium.worker.model.NodeCompleteRequest;
import com.rakovpublic.jneuropallium.worker.model.SplitInputResponse;
import com.rakovpublic.jneuropallium.master.services.ConfigurationService;
import com.rakovpublic.jneuropallium.master.services.impl.NodeManager;
import com.rakovpublic.jneuropallium.master.services.impl.NodeStatus;
import com.rakovpublic.jneuropallium.worker.net.storages.ISplitInput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/nodeManager")
public class NodeManagerController {
    private NodeManager nodeManager;
    private ConfigurationService configurationService;

    @Autowired
    public NodeManagerController(NodeManager nodeManager, ConfigurationService configurationService) {
        this.nodeManager = nodeManager;
        this.configurationService = configurationService;
    }

    @PostMapping("/completeRun")
    public ResponseEntity<?> nodeComplete(@RequestBody NodeCompleteRequest request) {
        try {
            nodeManager.setNodeStatus(request.getNodeName(), NodeStatus.IDLE);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/nextRun")
    public ResponseEntity<?> getNextRun(@RequestBody NodeCompleteRequest request) {
        ISplitInput splitInput = null;
        try {
            if(configurationService.getInputService().runCompleted()){
                //TODO: add result saving
                configurationService.getInputService().prepareResults();

                configurationService.getInputService().nextRun();
                configurationService.getInputService().prepareInputs();
            }
            // TODO:
            if(!configurationService.getInputService().hasPrepared()){
                configurationService.getInputService().prepareInputs();
            }

            splitInput = configurationService.getInputService().getNext(request.getNodeName());
            nodeManager.setNodeStatus(request.getNodeName(), NodeStatus.RUNNING);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e);
        }
        return ResponseEntity.ok(new SplitInputResponse(splitInput));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerNode(@RequestBody NodeCompleteRequest request) {
        try {
            nodeManager.register(request.getNodeName());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e);
        }
        return ResponseEntity.ok().build();
    }

}
