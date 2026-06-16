package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

public class UavSingleDemoInput {
    public String scenarioId;
    public String scenarioPath;
    public String outputDir;
    public int ticks;
    public long seed;
    public int cursor;
    public String name;

    public String getInputName() {
        return name == null || name.isBlank() ? scenarioId + "-input" : name;
    }
}

