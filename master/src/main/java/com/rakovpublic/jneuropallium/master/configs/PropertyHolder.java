/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.master.configs;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyHolder {
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

    public synchronized void updateConfig(String path){
        try {
            InputStream input = new FileInputStream(path);
            prop = new Properties();
            prop.load(input);

        } catch (IOException ex) {
            ex.printStackTrace();
            //TODO: add logger
        }
    }

    private void init() {
        try {
            InputStream input = getClass()
                    .getClassLoader().getResourceAsStream("config.properties");
            prop = new Properties();
            prop.load(input);

        } catch (IOException ex) {
            ex.printStackTrace();
            //TODO: add logger
        }

    }
}
