package com.rakovpublic.jneuropallium.worker.application;

import com.rakovpublic.jneuropallium.worker.util.IContext;

public interface IRunner {
    IContext getContext();

    void runNet(String mode, String jarPath);


}
