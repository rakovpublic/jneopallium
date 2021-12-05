package com.rakovpublic.jneuropallium.master.model;

public class ConfigurationUpdateRequest {
    private String layersMetaPath;
    private String layersMetaClass;
    private String splitInputClass;
    private String inputLoadingStrategyClass;
    private Integer partitions;
    private Integer defaultLoopsCount;
    private String signalsPersistClass;
    private String historyClass;
    private Long iterationsToStore;
    private String resultRunnerClass;
}
