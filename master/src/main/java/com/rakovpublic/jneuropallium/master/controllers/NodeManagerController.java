package com.rakovpublic.jneuropallium.master.controllers;

import com.rakovpublic.jneuropallium.master.model.NodeCompleteRequest;
import com.rakovpublic.jneuropallium.master.model.SplitInputResponse;
import com.rakovpublic.jneuropallium.master.services.IInputService;
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
    @Autowired
    private NodeManager nodeManager;

    private IInputService inputService;

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
            splitInput = inputService.getNext(request.getNodeName());
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
