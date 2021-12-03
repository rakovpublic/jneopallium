package com.rakovpublic.jneuropallium.master.controllers;

import com.rakovpublic.jneuropallium.master.model.NodeCompleteRequest;
import com.rakovpublic.jneuropallium.master.services.IInputService;
import com.rakovpublic.jneuropallium.master.services.MasterContext;
import com.rakovpublic.jneuropallium.master.services.impl.NodeManager;
import com.rakovpublic.jneuropallium.master.services.impl.NodeStatus;
import com.rakovpublic.jneuropallium.worker.net.storages.ISplitInput;
import org.springframework.beans.factory.annotation.Autowired;
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
    public void nodeComplete(@RequestBody NodeCompleteRequest request){
        nodeManager.setNodeStatus(request.getNodeName(), NodeStatus.IDLE);
        //TODO: add response
    }
    @PostMapping("/nextRun")
    public void getNextRun(@RequestBody NodeCompleteRequest request){
      ISplitInput splitInput = inputService.getNext(request.getNodeName());
      nodeManager.setNodeStatus(request.getNodeName(),NodeStatus.RUNNING);
        //TODO: add response
    }
    @PostMapping("/register")
    public void registerNode(@RequestBody NodeCompleteRequest request){
        nodeManager.register(request.getNodeName());
        //TODO: add response
    }

}
