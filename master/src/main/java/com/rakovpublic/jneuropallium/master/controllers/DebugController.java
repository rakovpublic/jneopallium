package com.rakovpublic.jneuropallium.master.controllers;

import com.rakovpublic.jneuropallium.master.services.ConfigurationService;
import com.rakovpublic.jneuropallium.master.services.impl.NodeManager;
import com.rakovpublic.jneuropallium.worker.net.core.IInputService;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayersMeta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read only view of the scheduler. Answers the questions that come up while watching a
 * cluster run: which node holds which partition, are the partitions disjoint, which layer is
 * the cluster on, and has anybody gone quiet.
 */
@RestController
@RequestMapping("/debug")
public class DebugController {

    private final ConfigurationService configurationService;
    private final NodeManager nodeManager;

    @Autowired
    public DebugController(ConfigurationService configurationService, NodeManager nodeManager) {
        this.configurationService = configurationService;
        this.nodeManager = nodeManager;
    }

    @GetMapping("/state")
    public ResponseEntity<?> state() {
        Map<String, Object> state = new LinkedHashMap<>();
        IInputService inputService;
        try {
            inputService = configurationService.getInputService();
        } catch (Exception e) {
            state.put("configured", false);
            state.put("reason", e.getMessage());
            return ResponseEntity.ok(state);
        }
        state.put("configured", true);
        state.put("runCompleted", inputService.runCompleted());
        state.put("hasPreparedPartitions", inputService.hasPrepared());
        state.put("nodes", nodeStates(inputService));
        state.put("layers", layerStates(inputService.getLayersMeta()));
        return ResponseEntity.ok(state);
    }

    private List<Map<String, Object>> nodeStates(IInputService inputService) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (Map.Entry<String, com.rakovpublic.jneuropallium.worker.model.NodeMeta> entry : inputService.getNodeMetas().entrySet()) {
            com.rakovpublic.jneuropallium.worker.model.NodeMeta meta = entry.getValue();
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("name", entry.getKey());
            node.put("idle", meta.getStatus());
            node.put("currentLayerPosition", meta.getCurrentLayer());
            node.put("managerStatus", nodeManager.getNodeStatus(entry.getKey()));
            if (meta.getCurrentInput() != null) {
                node.put("layerId", meta.getCurrentInput().getLayerId());
                node.put("start", meta.getCurrentInput().getStart());
                node.put("end", meta.getCurrentInput().getEnd());
            }
            if (meta.getTimestamp() != null) {
                node.put("lastAssignmentAgeMs", System.currentTimeMillis() - meta.getTimestamp());
            }
            nodes.add(node);
        }
        return nodes;
    }

    private List<Map<String, Object>> layerStates(ILayersMeta layersMeta) {
        List<Map<String, Object>> layers = new ArrayList<>();
        if (layersMeta == null) {
            return layers;
        }
        int position = 0;
        for (ILayerMeta layerMeta : layersMeta.getLayers()) {
            layers.add(layerState(position++, layerMeta));
        }
        layers.add(layerState(position, layersMeta.getResultLayer()));
        return layers;
    }

    private Map<String, Object> layerState(int position, ILayerMeta layerMeta) {
        Map<String, Object> layer = new LinkedHashMap<>();
        layer.put("position", position);
        layer.put("layerId", layerMeta.getID());
        layer.put("size", layerMeta.getSize());
        return layer;
    }
}
