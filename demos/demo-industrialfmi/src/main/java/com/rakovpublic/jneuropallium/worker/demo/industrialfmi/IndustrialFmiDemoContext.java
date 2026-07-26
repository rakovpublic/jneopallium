/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrialfmi;

import com.rakovpublic.jneuropallium.worker.util.IContext;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class IndustrialFmiDemoContext implements IContext {
    private final Map<String, String> properties = new HashMap<>();

    public IndustrialFmiDemoContext(String scenario, Path outputDir) {
        properties.put("scenario", scenario);
        properties.put("outputDir", outputDir.toString());
    }

    @Override
    public String getProperty(String propertyName) {
        return properties.get(propertyName);
    }

    @Override
    public void update(String path) {
        properties.put("lastUpdatePath", path);
    }
}
