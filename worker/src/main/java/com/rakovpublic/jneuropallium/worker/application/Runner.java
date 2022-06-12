package com.rakovpublic.jneuropallium.worker.application;

import com.rakovpublic.jneuropallium.worker.synchronizer.IContext;
import com.rakovpublic.jneuropallium.worker.util.JarClassLoaderService;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public abstract class Runner implements IRunner {
    private IContext context;

    public Runner() {
        context = getContext();
    }

    @Override
    public void runNet(String mode, String jarPath) {
        URL[] uris = new URL[0];
        try {
            uris = new URL[]{new URL(jarPath)};
        } catch (MalformedURLException e) {
            //TODO: add logger
            throw new RuntimeException(e);
        }
        JarClassLoaderService classLoaderService = new JarClassLoaderService(uris);
        IApplication application;
        if (mode.equals("local")) {
            application = new LocalApplication();
        } else {
            application = new HttpClusterApplication();
        }
        application.startApplication(context, classLoaderService);
    }
}
