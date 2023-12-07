package com.rakovpublic.jneuropallium.worker.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.net.study.IDirectLearningAlgorithm;
import com.rakovpublic.jneuropallium.worker.util.IContext;
import com.rakovpublic.jneuropallium.worker.util.JarClassLoaderService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;

public  class Runner implements IRunner {

    private final static Logger logger = LogManager.getLogger(Runner.class);


    @Override
    public void runNet(String mode, String jarPath, String contextClass, String contextJson) {
        URL[] uris = new URL[0];
        IContext context =null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            if (contextJson != null) {
                context = (IContext) mapper.readValue(contextJson, Class.forName(contextClass));
            } else {
                context = (IContext) Class.forName(contextClass).newInstance();
            }
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            logger.error("Cannot create instance of IContext for class " + contextClass, e);
        } catch (JsonProcessingException e) {
            logger.error("Cannot create instance of IContext for json " + contextJson, e);
        } catch (NullPointerException e) {
            logger.error("Wrong configuration for IContext " + contextClass + " config " + contextJson);
        }
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
