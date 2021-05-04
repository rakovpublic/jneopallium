package com.rakovpublic.jneuropallium.master.services;

import com.rakovpublic.jneuropallium.master.services.impl.NodeStatus;

import java.util.Set;

public interface INodeManager {
    Set<String> getNames();

    NodeStatus getNodeStatus(String name);

    void setNodeStatus(String name, NodeStatus status);

    void register(String name);
}
