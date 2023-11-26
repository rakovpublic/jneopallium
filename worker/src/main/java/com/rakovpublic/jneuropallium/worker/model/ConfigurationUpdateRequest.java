package com.rakovpublic.jneuropallium.worker.model;

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
    private String resultInterpreterClass;
    private String resultInterpreterJson;
    private List<String> discriminators;
    private HashMap<String, ConfigurationRecord> discriminatorsLoadingStrategies;
    private HashMap<String, ConfigurationRecord> discriminatorsSignalStorage;
    private HashMap<String, ConfigurationRecord> discriminatorsSignalStorageHistory;
    private HashMap<String, ConfigurationRecord> inputDiscriminatorStatuses;
    private HashMap<String, ConfigurationRecord> discriminatorsLayers;
    private String discriminatorSplitInput;
    private String discriminatorSplitInputJson;
    private HashMap<String, ConfigurationRecord> discriminatorsInitStrategySource;
    private HashMap<String, ConfigurationRecord> discriminatorsInitStrategyInputs;
    private HashMap<String, ConfigurationRecord> discriminatorsInitStrategyInputsCallback;


    public ConfigurationUpdateRequest(String layersMetaJson, String layersMetaClass, String splitInputClass, String splitInputJson, String inputLoadingStrategyClass, String inputLoadingStrategyJson, Integer partitions, Integer defaultLoopsCount, String signalsPersistClass, String signalsPersistJson, String historyClass, String historyJson, Long iterationsToStore, Integer loopsToStore, String resultRunnerClass, String resultRunnerJson, String reconnectStrategyClass, String reconnectStrategyJson, String resultInterpreterClass, String resultInterpreterJson, List<String> discriminators, HashMap<String, ConfigurationRecord> discriminatorsLoadingStrategies, HashMap<String, ConfigurationRecord> discriminatorsSignalStorage, HashMap<String, ConfigurationRecord> discriminatorsSignalStorageHistory, HashMap<String, ConfigurationRecord> inputDiscriminatorStatuses, HashMap<String, ConfigurationRecord> discriminatorsLayers, String discriminatorSplitInput, String discriminatorSplitInputJson, HashMap<String, ConfigurationRecord> discriminatorsInitStrategySource, HashMap<String, ConfigurationRecord> discriminatorsInitStrategyInputs, HashMap<String, ConfigurationRecord> discriminatorsInitStrategyInputsCallback, String layersMetaPath, Long nodeTimeout) {
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
        this.resultInterpreterClass = resultInterpreterClass;
        this.resultInterpreterJson = resultInterpreterJson;
        this.discriminators = discriminators;
        this.discriminatorsLoadingStrategies = discriminatorsLoadingStrategies;
        this.discriminatorsSignalStorage = discriminatorsSignalStorage;
        this.discriminatorsSignalStorageHistory = discriminatorsSignalStorageHistory;
        this.inputDiscriminatorStatuses = inputDiscriminatorStatuses;
        this.discriminatorsLayers = discriminatorsLayers;
        this.discriminatorSplitInput = discriminatorSplitInput;
        this.discriminatorSplitInputJson = discriminatorSplitInputJson;
        this.discriminatorsInitStrategySource = discriminatorsInitStrategySource;
        this.discriminatorsInitStrategyInputs = discriminatorsInitStrategyInputs;
        this.discriminatorsInitStrategyInputsCallback = discriminatorsInitStrategyInputsCallback;
        this.layersMetaPath = layersMetaPath;
        this.nodeTimeout = nodeTimeout;
    }

    public String getResultInterpreterClass() {
        return resultInterpreterClass;
    }

    public void setResultInterpreterClass(String resultInterpreterClass) {
        this.resultInterpreterClass = resultInterpreterClass;
    }

    public String getResultInterpreterJson() {
        return resultInterpreterJson;
    }

    public void setResultInterpreterJson(String resultInterpreterJson) {
        this.resultInterpreterJson = resultInterpreterJson;
    }

    public HashMap<String, ConfigurationRecord> getDiscriminatorsInitStrategySource() {
        return discriminatorsInitStrategySource;
    }

    public void setDiscriminatorsInitStrategySource(HashMap<String, ConfigurationRecord> discriminatorsInitStrategySource) {
        this.discriminatorsInitStrategySource = discriminatorsInitStrategySource;
    }

    public HashMap<String, ConfigurationRecord> getDiscriminatorsInitStrategyInputs() {
        return discriminatorsInitStrategyInputs;
    }

    public void setDiscriminatorsInitStrategyInputs(HashMap<String, ConfigurationRecord> discriminatorsInitStrategyInputs) {
        this.discriminatorsInitStrategyInputs = discriminatorsInitStrategyInputs;
    }

    public HashMap<String, ConfigurationRecord> getDiscriminatorsInitStrategyInputsCallback() {
        return discriminatorsInitStrategyInputsCallback;
    }

    public void setDiscriminatorsInitStrategyInputsCallback(HashMap<String, ConfigurationRecord> discriminatorsInitStrategyInputsCallback) {
        this.discriminatorsInitStrategyInputsCallback = discriminatorsInitStrategyInputsCallback;
    }

    public HashMap<String, ConfigurationRecord> getInputDiscriminatorStatuses() {
        return inputDiscriminatorStatuses;
    }

    public void setInputDiscriminatorStatuses(HashMap<String, ConfigurationRecord> inputDiscriminatorStatuses) {
        this.inputDiscriminatorStatuses = inputDiscriminatorStatuses;
    }

    public HashMap<String, ConfigurationRecord> getDiscriminatorsLayers() {
        return discriminatorsLayers;
    }

    public void setDiscriminatorsLayers(HashMap<String, ConfigurationRecord> discriminatorsLayers) {
        this.discriminatorsLayers = discriminatorsLayers;
    }

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


    public String getDiscriminatorSplitInput() {
        return discriminatorSplitInput;
    }

    public void setDiscriminatorSplitInput(String discriminatorSplitInput) {
        this.discriminatorSplitInput = discriminatorSplitInput;
    }

    public String getDiscriminatorSplitInputJson() {
        return discriminatorSplitInputJson;
    }

    public void setDiscriminatorSplitInputJson(String discriminatorSplitInputJson) {
        this.discriminatorSplitInputJson = discriminatorSplitInputJson;
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


    @Override
    public String toString() {
        return "ConfigurationUpdateRequest{" +
                "layersMetaJson='" + layersMetaJson + '\'' +
                ", layersMetaClass='" + layersMetaClass + '\'' +
                ", splitInputClass='" + splitInputClass + '\'' +
                ", splitInputJson='" + splitInputJson + '\'' +
                ", inputLoadingStrategyClass='" + inputLoadingStrategyClass + '\'' +
                ", inputLoadingStrategyJson='" + inputLoadingStrategyJson + '\'' +
                ", partitions=" + partitions +
                ", defaultLoopsCount=" + defaultLoopsCount +
                ", signalsPersistClass='" + signalsPersistClass + '\'' +
                ", signalsPersistJson='" + signalsPersistJson + '\'' +
                ", historyClass='" + historyClass + '\'' +
                ", historyJson='" + historyJson + '\'' +
                ", iterationsToStore=" + iterationsToStore +
                ", loopsToStore=" + loopsToStore +
                ", resultRunnerClass='" + resultRunnerClass + '\'' +
                ", resultRunnerJson='" + resultRunnerJson + '\'' +
                ", reconnectStrategyClass='" + reconnectStrategyClass + '\'' +
                ", reconnectStrategyJson='" + reconnectStrategyJson + '\'' +
                ", discriminators=" + discriminators.toString() +
                ", discriminatorsLoadingStrategies=" + discriminatorsLoadingStrategies.toString() +
                ", discriminatorsSignalStorage=" + discriminatorsSignalStorage.toString() +
                ", discriminatorsSignalStorageHistory=" + discriminatorsSignalStorageHistory.toString() +
                ", inputDiscriminatorStatuses=" + inputDiscriminatorStatuses.toString() +
                ", discriminatorsLayers=" + discriminatorsLayers.toString() +
                ", discriminatorSplitInput=" + discriminatorSplitInput +
                ", layersMetaPath='" + layersMetaPath + '\'' +
                ", nodeTimeout=" + nodeTimeout +
                '}';
    }
}
