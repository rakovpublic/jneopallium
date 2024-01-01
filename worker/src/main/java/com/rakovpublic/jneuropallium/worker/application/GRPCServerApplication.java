/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.application;

import com.rakovpublic.jneuropallium.worker.util.IContext;
import com.rakovpublic.jneuropallium.worker.util.JarClassLoaderService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//TODO: add implementation
public class GRPCServerApplication implements IApplication {
    private static final Logger logger = LogManager.getLogger(GRPCServerApplication.class);
    @Override
    public void startApplication(IContext context, JarClassLoaderService classLoaderService) {

    }
}
