package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import java.util.LinkedHashMap;
import java.util.Map;

public class RecognitionNetworkConfig {
    private int cameraWidth = 1920;
    private int cameraHeight = 1080;
    private int patchSize = 3;
    private int patchStride = 1;
    private int firstLayerFilterCount = 6;
    private int secondLayerFilterCount = 4;
    private double learningRate = 0.08;

    public static RecognitionNetworkConfig fpv1080p() {
        return new RecognitionNetworkConfig();
    }

    public static RecognitionNetworkConfig fpv720p() {
        RecognitionNetworkConfig config = new RecognitionNetworkConfig();
        config.setCameraWidth(1280);
        config.setCameraHeight(720);
        return config;
    }

    public int getCameraWidth() { return cameraWidth; }
    public void setCameraWidth(int cameraWidth) { this.cameraWidth = Math.max(3, cameraWidth); }
    public int getCameraHeight() { return cameraHeight; }
    public void setCameraHeight(int cameraHeight) { this.cameraHeight = Math.max(3, cameraHeight); }
    public int getPatchSize() { return patchSize; }
    public void setPatchSize(int patchSize) {
        if (patchSize != 3) {
            throw new IllegalArgumentException("UAV recognition patch size must stay 3x3");
        }
        this.patchSize = patchSize;
    }
    public int getPatchStride() { return patchStride; }
    public void setPatchStride(int patchStride) { this.patchStride = Math.max(1, patchStride); }
    public int getFirstLayerFilterCount() { return firstLayerFilterCount; }
    public void setFirstLayerFilterCount(int firstLayerFilterCount) {
        this.firstLayerFilterCount = Math.max(1, firstLayerFilterCount);
    }
    public int getSecondLayerFilterCount() { return secondLayerFilterCount; }
    public void setSecondLayerFilterCount(int secondLayerFilterCount) {
        this.secondLayerFilterCount = Math.max(1, secondLayerFilterCount);
    }
    public double getLearningRate() { return learningRate; }
    public void setLearningRate(double learningRate) {
        this.learningRate = TargetPriorityProcessor.clamp(learningRate);
    }

    public int firstLayerPatchColumns() {
        return patchColumns(cameraWidth);
    }

    public int firstLayerPatchRows() {
        return patchRows(cameraHeight);
    }

    public long firstLayerPatchCount() {
        return (long) firstLayerPatchColumns() * firstLayerPatchRows();
    }

    public long firstLayerSignalCapacity() {
        return firstLayerPatchCount() * firstLayerFilterCount;
    }

    public long secondLayerSignalCapacity() {
        int conv1Width = firstLayerPatchColumns();
        int conv1Height = firstLayerPatchRows();
        long secondPatchesPerFeature = (long) patchColumns(conv1Width) * patchRows(conv1Height);
        return secondPatchesPerFeature * firstLayerFilterCount * secondLayerFilterCount;
    }

    public Map<String, Object> asArtifactMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("cameraWidth", cameraWidth);
        map.put("cameraHeight", cameraHeight);
        map.put("patchSize", patchSize);
        map.put("patchStride", patchStride);
        map.put("firstLayerPatchColumns", firstLayerPatchColumns());
        map.put("firstLayerPatchRows", firstLayerPatchRows());
        map.put("firstLayerPatchCount", firstLayerPatchCount());
        map.put("firstLayerSignalCapacity", firstLayerSignalCapacity());
        map.put("secondLayerSignalCapacity", secondLayerSignalCapacity());
        map.put("learningRate", learningRate);
        return map;
    }

    int patchColumns(int width) {
        return width < patchSize ? 0 : 1 + ((width - patchSize) / patchStride);
    }

    int patchRows(int height) {
        return height < patchSize ? 0 : 1 + ((height - patchSize) / patchStride);
    }
}
