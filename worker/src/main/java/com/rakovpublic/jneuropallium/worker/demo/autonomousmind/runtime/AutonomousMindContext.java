package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.util.IContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class AutonomousMindContext implements IContext {
    private final Map<String, String> properties = new LinkedHashMap<>();
    private String contextPath;

    public AutonomousMindContext() {
    }

    public AutonomousMindContext(Map<String, String> properties) {
        setProperties(properties);
    }

    @Override
    public String getProperty(String propertyName) {
        return properties.get(propertyName);
    }

    @Override
    public void update(String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        try {
            AutonomousMindContext updated = new ObjectMapper()
                    .readValue(Files.readString(Path.of(path)), AutonomousMindContext.class);
            properties.clear();
            properties.putAll(updated.properties);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot reload AutonomousMind context from " + path, e);
        }
    }

    @JsonProperty("properties")
    public Map<String, String> getProperties() {
        return properties;
    }

    @JsonProperty("properties")
    public void setProperties(Map<String, String> values) {
        properties.clear();
        if (values != null) {
            properties.putAll(values);
        }
    }

    @JsonAnyGetter
    @JsonIgnore
    public Map<String, String> any() {
        return properties;
    }

    @JsonAnySetter
    public void put(String key, Object value) {
        if ("properties".equals(key) && value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                properties.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
        } else if ("contextPath".equals(key)) {
            setContextPath(value == null ? null : String.valueOf(value));
        } else {
            properties.put(key, value == null ? null : String.valueOf(value));
        }
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
        update(contextPath);
    }
}
