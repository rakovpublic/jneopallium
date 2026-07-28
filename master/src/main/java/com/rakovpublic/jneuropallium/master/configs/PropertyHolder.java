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

    /**
     * System properties win over the packaged defaults, and {@code ${...}} placeholders are
     * resolved against them, so a deployment can point the master at its own folders without
     * repackaging.
     */
    public synchronized String getProp(String propertyName) {
        String value = System.getProperty(propertyName, prop.getProperty(propertyName));
        return value == null ? null : resolvePlaceholders(value);
    }

    private String resolvePlaceholders(String value) {
        String result = value;
        int start;
        while ((start = result.indexOf("${")) >= 0) {
            int end = result.indexOf('}', start);
            if (end < 0) {
                break;
            }
            String key = result.substring(start + 2, end);
            String replacement = System.getProperty(key, "");
            result = result.substring(0, start) + replacement + result.substring(end + 1);
        }
        return result;
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
        prop = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                logger.warn("No config.properties on the classpath, relying on system properties");
                return;
            }
            prop.load(input);
        } catch (IOException ex) {
            logger.error("cannot read default properties", ex);
        }

    }
}
