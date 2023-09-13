/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.master.configs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyHolder {
    private static final Logger logger = LogManager.getLogger(PropertyHolder.class);
    private static PropertyHolder propertyHolder = new PropertyHolder();
    private Properties prop;

    private PropertyHolder() {
        init();
    }

    public synchronized static PropertyHolder getPropertyHolder() {
        return propertyHolder;
    }

    public synchronized String getProp(String propertyName) {
        return prop.getProperty(propertyName);
    }

    public synchronized void updateConfig(String path) {
        try {
            InputStream input = new FileInputStream(path);
            prop = new Properties();
            prop.load(input);

        } catch (IOException ex) {
            logger.error("cannot read properties from path " + path, ex);
        }
    }

    private void init() {
        try {
            InputStream input = getClass()
                    .getClassLoader().getResourceAsStream("config.properties");
            prop = new Properties();
            prop.load(input);

        } catch (IOException ex) {
            logger.error("cannot read default properties", ex);
        }

    }
}
