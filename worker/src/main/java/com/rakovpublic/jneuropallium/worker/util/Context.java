/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/***
 * Created by Rakovskyi Dmytro on 08.06.2018.
 */

public class Context implements IContext {
    private static final Logger logger = LogManager.getLogger(Context.class);
    private Properties prop;
    private String path;

    public Context(String path) {
        this.path = path;
        init();
    }

    public Context() {
        init();
    }


    @Override
    public String getProperty(String propertyName) {
        if (prop == null) {
            init();
        }
        return prop.getProperty(propertyName, null);
    }

    @Override
    public void update(String path) {
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
            InputStream input = new FileInputStream(path);
            prop = new Properties();
            prop.load(input);

        } catch (IOException ex) {
            logger.error("cannot read properties from path " + path, ex);
        }
    }


}
