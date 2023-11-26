package com.rakovpublic.jneuropallium.worker.application;

import com.rakovpublic.jneuropallium.worker.synchronizer.IContext;
import com.rakovpublic.jneuropallium.worker.util.JarClassLoaderService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;

public abstract class Runner implements IRunner {
    private IContext context;
    private final static Logger logger = LogManager.getLogger(Runner.class);

    public Runner() {
        context = getContext();
    }

    @Override
    public void runNet(String mode, String jarPath) {
        URL[] uris = new URL[0];
        try {
            uris = new URL[]{new URL(jarPath)};
        } catch (MalformedURLException e) {
            logger.error("Incorrect url", e);
            throw new RuntimeException(e);
        }
        JarClassLoaderService classLoaderService = new JarClassLoaderService(uris);
        IApplication application;
        if (mode.equals("local")) {
            application = new LocalApplication();
        } else if (mode.equals("http")) {
            application = new HttpClusterApplication();
        } else {
            application = new GRPCBasedApplication();
        }
        try {
            application.startApplication(context, classLoaderService);
        } catch (Exception e) {
            logger.error(e);
        }

    }
}
