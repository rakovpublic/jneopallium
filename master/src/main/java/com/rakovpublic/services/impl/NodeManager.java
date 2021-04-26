package com.rakovpublic.services.impl;

import com.rakovpublic.services.INodeManager;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NodeManager implements INodeManager {

    private static NodeManager nodeManager = new NodeManager();
    private ConcurrentHashMap<String, NodeStatus> nodes;

    private NodeManager(){
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

    public static NodeManager getNodeManager(){
        return nodeManager;
    }
}
