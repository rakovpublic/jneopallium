package com.rakovpublic.jneuropallium.worker.model;

public class ConfigurationUpdateRequest {
    private String layersMetaJson;
    private String layersMetaClass;
    private String splitInputClass;
    private String splitInputJson;
    private String inputLoadingStrategyClass;
    private String inputLoadingStrategyJson;
    private Integer partitions;
    private Integer defaultLoopsCount;
    private String signalsPersistClass;
    private String signalsPersistJson;
    private String historyClass;
    private String historyJson;
    private Long iterationsToStore;
    private Integer loopsToStore;
    private String resultRunnerClass;
    private String resultRunnerJson;
    private String reconnectStrategyClass;
    private String reconnectStrategyJson;
    private String layersMetaPath;


    public ConfigurationUpdateRequest(String layersMetaJson, String layersMetaClass, String splitInputClass, String splitInputJson, String inputLoadingStrategyClass, String inputLoadingStrategyJson, Integer partitions, Integer defaultLoopsCount, String signalsPersistClass, String signalsPersistJson, String historyClass, String historyJson, Long iterationsToStore, Integer loopsToStore, String resultRunnerClass, String resultRunnerJson, String reconnectStrategyClass, String reconnectStrategyJson, String layersMetaPath) {
        this.layersMetaJson = layersMetaJson;
        this.layersMetaClass = layersMetaClass;
        this.splitInputClass = splitInputClass;
        this.splitInputJson = splitInputJson;
        this.inputLoadingStrategyClass = inputLoadingStrategyClass;
        this.inputLoadingStrategyJson = inputLoadingStrategyJson;
        this.partitions = partitions;
        this.defaultLoopsCount = defaultLoopsCount;
        this.signalsPersistClass = signalsPersistClass;
        this.signalsPersistJson = signalsPersistJson;
        this.historyClass = historyClass;
        this.historyJson = historyJson;
        this.iterationsToStore = iterationsToStore;
        this.loopsToStore = loopsToStore;
        this.resultRunnerClass = resultRunnerClass;
        this.resultRunnerJson = resultRunnerJson;
        this.reconnectStrategyClass = reconnectStrategyClass;
        this.reconnectStrategyJson = reconnectStrategyJson;
        this.layersMetaPath = layersMetaPath;
    }

    public String getInputLoadingStrategyJson() {
        return inputLoadingStrategyJson;
    }

    public void setInputLoadingStrategyJson(String inputLoadingStrategyJson) {
        this.inputLoadingStrategyJson = inputLoadingStrategyJson;
    }

    public String getSignalsPersistJson() {
        return signalsPersistJson;
    }

    public void setSignalsPersistJson(String signalsPersistJson) {
        this.signalsPersistJson = signalsPersistJson;
    }

    public String getHistoryJson() {
        return historyJson;
    }

    public void setHistoryJson(String historyJson) {
        this.historyJson = historyJson;
    }

    public String getResultRunnerJson() {
        return resultRunnerJson;
    }

    public void setResultRunnerJson(String resultRunnerJson) {
        this.resultRunnerJson = resultRunnerJson;
    }

    public String getReconnectStrategyJson() {
        return reconnectStrategyJson;
    }

    public void setReconnectStrategyJson(String reconnectStrategyJson) {
        this.reconnectStrategyJson = reconnectStrategyJson;
    }

    public String getLayersMetaJson() {
        return layersMetaJson;
    }

    public void setLayersMetaJson(String layersMetaJson) {
        this.layersMetaJson = layersMetaJson;
    }

    public Integer getLoopsToStore() {
        return loopsToStore;
    }

    public void setLoopsToStore(Integer loopsToStore) {
        this.loopsToStore = loopsToStore;
    }

    public ConfigurationUpdateRequest() {
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

    public String getReconnectStrategyClass() {
        return reconnectStrategyClass;
    }

    public void setReconnectStrategyClass(String reconnectStrategyClass) {
        this.reconnectStrategyClass = reconnectStrategyClass;
    }

    public String getLayersMetaPath() {
        return layersMetaPath;
    }

    public void setLayersMetaPath(String layersMetaPath) {
        this.layersMetaPath = layersMetaPath;
    }

    public String getSplitInputJson() {
        return splitInputJson;
    }

    public void setSplitInputJson(String splitInputJson) {
        this.splitInputJson = splitInputJson;
    }
}
