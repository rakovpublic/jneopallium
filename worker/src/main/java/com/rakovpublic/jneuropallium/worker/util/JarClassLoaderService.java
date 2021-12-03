package com.rakovpublic.jneuropallium.worker.util;

import com.rakovpublic.jneuropallium.worker.application.LocalApplication;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.net.URLClassLoader;

public class JarClassLoaderService extends URLClassLoader {
    private static final Logger logger = LogManager.getLogger(JarClassLoaderService.class);

    private URLClassLoader urlClassLoader;
    private Boolean initiated;

    public JarClassLoaderService(URL[] path) {
        super(path);
    }

    public Boolean containsClass(String name) {
        try {
            this.findClass(name);
            return true;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            logger.error("Class loader cannot find user defined class. Please check Your jar or upload it.",e);
            return false;
        }

    }
}
