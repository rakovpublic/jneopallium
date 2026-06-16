package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Neuron;

import java.util.Arrays;

public class ConvolutionalPerceptronNeuron extends Neuron
        implements IPixelPatchConvolutionNeuron, IFeaturePatchConvolutionNeuron {
    private String layerName;
    private String filterName;
    private double[] weights = new double[9];
    private double bias;

    public ConvolutionalPerceptronNeuron() {
        super();
        this.currentNeuronClass = ConvolutionalPerceptronNeuron.class;
        this.resultClasses.add(ConvolutionFeatureSignal.class);
        addSignalProcessor(PixelPatchSignal.class, new PixelPatchConvolutionProcessor());
        addSignalProcessor(FeaturePatchSignal.class, new FeaturePatchConvolutionProcessor());
    }

    public ConvolutionalPerceptronNeuron(String layerName, String filterName, double[] weights, double bias) {
        this();
        this.layerName = layerName;
        this.filterName = filterName;
        setWeights(weights);
        this.bias = bias;
    }

    public String getLayerName() { return layerName; }
    public void setLayerName(String layerName) { this.layerName = layerName; }
    public String getFilterName() { return filterName; }
    public void setFilterName(String filterName) { this.filterName = filterName; }
    public double[] getWeights() { return weights.clone(); }
    public void setWeights(double[] weights) {
        if (weights == null || weights.length != 9) {
            throw new IllegalArgumentException("ConvolutionalPerceptronNeuron requires exactly 9 weights");
        }
        this.weights = Arrays.copyOf(weights, 9);
    }
    public double getBias() { return bias; }
    public void setBias(double bias) { this.bias = bias; }

    public ConvolutionFeatureSignal fire(PixelPatchSignal patch) {
        return fire(patch.getMissionId(), patch.getUavId(), patch.getTick(), patch.getFrameId(),
                patch.getPatchX(), patch.getPatchY(), patch.getPixels());
    }

    public ConvolutionFeatureSignal fire(FeaturePatchSignal patch) {
        return fire(patch.getMissionId(), patch.getUavId(), patch.getTick(), patch.getFrameId(),
                patch.getPatchX(), patch.getPatchY(), patch.getActivations());
    }

    private ConvolutionFeatureSignal fire(String missionId, String uavId, long tick, String frameId,
                                          int patchX, int patchY, double[] inputs) {
        double preActivation = bias;
        for (int i = 0; i < 9; i++) {
            preActivation += weights[i] * inputs[i];
        }
        ConvolutionFeatureSignal signal = new ConvolutionFeatureSignal();
        signal.setMissionId(missionId);
        signal.setUavId(uavId);
        signal.setTick(tick);
        signal.setFrameId(frameId);
        signal.setLayerName(layerName);
        signal.setFilterName(filterName);
        signal.setPatchX(patchX);
        signal.setPatchY(patchY);
        signal.setPreActivation(preActivation);
        signal.setActivation(relu(preActivation));
        signal.setSourceNeuronId(getId() == null ? -1L : getId());
        signal.attribute("neuronType", "NINE_INPUT_CONVOLUTIONAL_PERCEPTRON");
        return signal;
    }

    private static double relu(double value) {
        return Math.max(0.0, value);
    }
}
