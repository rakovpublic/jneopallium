/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.model;

public class ConfigurationRecord {
    private String className;
    private String json;

    public ConfigurationRecord(String className, String json) {
        this.className = className;
        this.json = json;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }
}
