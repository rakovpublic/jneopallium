package com.rakovpublic.jneuropallium.master.controllers;

import com.rakovpublic.jneuropallium.master.services.ConfigurationService;
import com.rakovpublic.jneuropallium.worker.net.core.IInputService;
import com.rakovpublic.jneuropallium.master.services.impl.NodeManager;
import com.rakovpublic.jneuropallium.master.services.impl.NodeStatus;
import com.rakovpublic.jneuropallium.worker.model.NodeCompleteRequest;
import com.rakovpublic.jneuropallium.worker.model.SplitInputResponse;
import com.rakovpublic.jneuropallium.worker.net.neuron.IResultNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.ISplitInput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/nodeManager")
public class NodeManagerController {
    private static final Logger logger = LogManager.getLogger(NodeManagerController.class);
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
        IInputService inputService = configurationService.getInputService();
        ISplitInput splitInput = null;
        try {
            splitInput = inputService.getNext(request.getNodeName());
            if (splitInput == null && inputService.runCompleted()) {
                if (inputService.hasDiscriminators() && !inputService.isDiscriminatorsDone()) {
                    inputService.prepareDiscriminatorsInputs();
                    splitInput = inputService.getNextDiscriminators(request.getNodeName());
                } else {
                    if (inputService.isResultValid()) {
                        inputService.prepareResults();
                    }
                    inputService.nextRun();
                    inputService.nextRunDiscriminator();
                    splitInput = inputService.getNext(request.getNodeName());
                }
            }
            if (splitInput == null) {
                // Nothing to hand out right now - other nodes are still finishing the current
                // layer. The worker backs off and asks again; do not report this as an error.
                nodeManager.setNodeStatus(request.getNodeName(), NodeStatus.IDLE);
                return ResponseEntity.noContent().build();
            }
            nodeManager.setNodeStatus(request.getNodeName(), NodeStatus.RUNNING);
        } catch (Exception e) {
            logger.error("Cannot resolve next partition for node " + request.getNodeName(), e);
            return ResponseEntity.internalServerError().body(e.getMessage());
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

    @GetMapping("/getResults")
    public ResponseEntity<?> getResults(@RequestParam Integer loop, @RequestParam Long epoch) {
        List<IResultNeuron> resultNeurons = configurationService.getInputService().getResults(loop, epoch);
        if (configurationService.getResultInterpreter() != null && resultNeurons != null && resultNeurons.size() > 0) {
            return ResponseEntity.ok().body(configurationService.getResultInterpreter().getResult(resultNeurons));
        } else if (resultNeurons != null && resultNeurons.size() > 0) {
            return ResponseEntity.ok().body(resultNeurons);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/isProcessing")
    public ResponseEntity<?> getResults(@RequestParam String name) {
        return ResponseEntity.ok(configurationService.getInputService().isProcessing(name));
    }

}
