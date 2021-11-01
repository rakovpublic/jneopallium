package com.rakovpublic.jneuropallium.worker.application;

import com.rakovpublic.jneuropallium.worker.synchronizer.IContext;

public abstract class Runner implements IRunner {
    private IContext context;

    public Runner() {
        context = getContext();
    }

    @Override
    public void runNet(String mode) {
        IApplication application;
        if (mode.equals("local")) {
            application = new LocalApplication();
        } else {
            application = new HttpClusterApplication();
        }
        application.startApplication(context);
    }
}
