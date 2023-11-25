package com.rakovpublic.jneuropallium.worker.model;

import com.rakovpublic.jneuropallium.worker.net.DiscriminatorSplitInput;
import com.rakovpublic.jneuropallium.worker.net.storages.*;

import java.util.HashMap;
import java.util.List;

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

    public Long getNodeTimeout() {
        return nodeTimeout;
    }

    public void setNodeTimeout(Long nodeTimeout) {
        this.nodeTimeout = nodeTimeout;
    }

    private String layersMetaPath;
    private Long nodeTimeout;

    public List<String> getDiscriminators() {
        return discriminators;
    }

    public void setDiscriminators(List<String> discriminators) {
        this.discriminators = discriminators;
    }

    public HashMap<String, ConfigurationRecord> getDiscriminatorsLoadingStrategies() {
        return discriminatorsLoadingStrategies;
    }

    public void setDiscriminatorsLoadingStrategies(HashMap<String, ConfigurationRecord> discriminatorsLoadingStrategies) {
        this.discriminatorsLoadingStrategies = discriminatorsLoadingStrategies;
    }

    public HashMap<String, ConfigurationRecord> getDiscriminatorsSignalStorage() {
        return discriminatorsSignalStorage;
    }

    public void setDiscriminatorsSignalStorage(HashMap<String, ConfigurationRecord> discriminatorsSignalStorage) {
        this.discriminatorsSignalStorage = discriminatorsSignalStorage;
    }

    public HashMap<String, ConfigurationRecord> getDiscriminatorsSignalStorageHistory() {
        return discriminatorsSignalStorageHistory;
    }

    public void setDiscriminatorsSignalStorageHistory(HashMap<String, ConfigurationRecord> discriminatorsSignalStorageHistory) {
        this.discriminatorsSignalStorageHistory = discriminatorsSignalStorageHistory;
    }

    public HashMap<String, HashMap<IInitInput, InputStatusMeta>> getInputDiscriminatorStatuses() {
        return inputDiscriminatorStatuses;
    }

    public void setInputDiscriminatorStatuses(HashMap<String, HashMap<IInitInput, InputStatusMeta>> inputDiscriminatorStatuses) {
        this.inputDiscriminatorStatuses = inputDiscriminatorStatuses;
    }

    public DiscriminatorSplitInput getDiscriminatorSplitInput() {
        return discriminatorSplitInput;
    }

    public void setDiscriminatorSplitInput(DiscriminatorSplitInput discriminatorSplitInput) {
        this.discriminatorSplitInput = discriminatorSplitInput;
    }

    private List<String> discriminators;
    private HashMap<String, ConfigurationRecord> discriminatorsLoadingStrategies;
    private HashMap<String, ConfigurationRecord> discriminatorsSignalStorage;
    private HashMap<String, ConfigurationRecord> discriminatorsSignalStorageHistory;
    private HashMap<String,HashMap<IInitInput, InputStatusMeta>> inputDiscriminatorStatuses;
    private DiscriminatorSplitInput discriminatorSplitInput;


    public ConfigurationUpdateRequest(String layersMetaJson, String layersMetaClass, String splitInputClass, String splitInputJson, String inputLoadingStrategyClass, String inputLoadingStrategyJson, Integer partitions, Integer defaultLoopsCount, String signalsPersistClass, String signalsPersistJson, String historyClass, String historyJson, Long iterationsToStore, Integer loopsToStore, String resultRunnerClass, String resultRunnerJson, String reconnectStrategyClass, String reconnectStrategyJson, String layersMetaPath, Long nodeTimeout, List<String> discriminators, HashMap<String, ConfigurationRecord> discriminatorsLoadingStrategies, HashMap<String, ConfigurationRecord> discriminatorsSignalStorage, HashMap<String, ConfigurationRecord> discriminatorsSignalStorageHistory, HashMap<String, HashMap<IInitInput, InputStatusMeta>> inputDiscriminatorStatuses, DiscriminatorSplitInput discriminatorSplitInput) {
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
        this.nodeTimeout = nodeTimeout;
        this.discriminators = discriminators;
        this.discriminatorsLoadingStrategies = discriminatorsLoadingStrategies;
        this.discriminatorsSignalStorage = discriminatorsSignalStorage;
        this.discriminatorsSignalStorageHistory = discriminatorsSignalStorageHistory;
        this.inputDiscriminatorStatuses = inputDiscriminatorStatuses;
        this.discriminatorSplitInput = discriminatorSplitInput;
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
