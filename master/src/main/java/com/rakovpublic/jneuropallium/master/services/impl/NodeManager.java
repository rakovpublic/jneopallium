package com.rakovpublic.jneuropallium.master.services.impl;

import com.rakovpublic.jneuropallium.master.services.INodeManager;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NodeManager implements INodeManager {

    //private static NodeManager nodeManager = new NodeManager();
    private ConcurrentHashMap<String, NodeStatus> nodes;

    public NodeManager() {
        nodes = new ConcurrentHashMap<>();
    }

    @Override
    public Set<String> getNames() {
        return nodes.keySet();
    }

    @Override
    public NodeStatus getNodeStatus(String name) {
        return nodes.get(name);
    }

    @Override
    public void setNodeStatus(String name, NodeStatus status) {
        nodes.put(name, status);
    }

    @Override
    public void register(String name) {
        nodes.put(name, NodeStatus.STARTING);
    }

    /*public static NodeManager getNodeManager() {
        return nodeManager;
    }*/
}
