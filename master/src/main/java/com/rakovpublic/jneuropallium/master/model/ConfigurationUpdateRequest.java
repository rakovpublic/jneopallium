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

    public ConfigurationUpdateRequest(String layersMetaPath, String layersMetaClass, String splitInputClass, String inputLoadingStrategyClass, Integer partitions, Integer defaultLoopsCount, String signalsPersistClass, String historyClass, Long iterationsToStore, String resultRunnerClass) {
        this.layersMetaPath = layersMetaPath;
        this.layersMetaClass = layersMetaClass;
        this.splitInputClass = splitInputClass;
        this.inputLoadingStrategyClass = inputLoadingStrategyClass;
        this.partitions = partitions;
        this.defaultLoopsCount = defaultLoopsCount;
        this.signalsPersistClass = signalsPersistClass;
        this.historyClass = historyClass;
        this.iterationsToStore = iterationsToStore;
        this.resultRunnerClass = resultRunnerClass;
    }

    public ConfigurationUpdateRequest() {
    }

    public String getLayersMetaPath() {
        return layersMetaPath;
    }

    public void setLayersMetaPath(String layersMetaPath) {
        this.layersMetaPath = layersMetaPath;
    }

    public String getLayersMetaClass() {
        return layersMetaClass;
    }

    public void setLayersMetaClass(String layersMetaClass) {
        this.layersMetaClass = layersMetaClass;
    }

    public String getSplitInputClass() {
        return splitInputClass;
    }

    public void setSplitInputClass(String splitInputClass) {
        this.splitInputClass = splitInputClass;
    }

    public String getInputLoadingStrategyClass() {
        return inputLoadingStrategyClass;
    }

    public void setInputLoadingStrategyClass(String inputLoadingStrategyClass) {
        this.inputLoadingStrategyClass = inputLoadingStrategyClass;
    }

    public Integer getPartitions() {
        return partitions;
    }

    public void setPartitions(Integer partitions) {
        this.partitions = partitions;
    }

    public Integer getDefaultLoopsCount() {
        return defaultLoopsCount;
    }

    public void setDefaultLoopsCount(Integer defaultLoopsCount) {
        this.defaultLoopsCount = defaultLoopsCount;
    }

    public String getSignalsPersistClass() {
        return signalsPersistClass;
    }

    public void setSignalsPersistClass(String signalsPersistClass) {
        this.signalsPersistClass = signalsPersistClass;
    }

    public String getHistoryClass() {
        return historyClass;
    }

    public void setHistoryClass(String historyClass) {
        this.historyClass = historyClass;
    }

    public Long getIterationsToStore() {
        return iterationsToStore;
    }

    public void setIterationsToStore(Long iterationsToStore) {
        this.iterationsToStore = iterationsToStore;
    }

    public String getResultRunnerClass() {
        return resultRunnerClass;
    }

    public void setResultRunnerClass(String resultRunnerClass) {
        this.resultRunnerClass = resultRunnerClass;
    }
}
