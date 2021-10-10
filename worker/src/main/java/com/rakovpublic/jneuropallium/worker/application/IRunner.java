package com.rakovpublic.jneuropallium.worker.application;

import com.rakovpublic.jneuropallium.worker.synchronizer.IContext;

public interface IRunner {
    IContext getContext();

    void runNet(String mode);


}
