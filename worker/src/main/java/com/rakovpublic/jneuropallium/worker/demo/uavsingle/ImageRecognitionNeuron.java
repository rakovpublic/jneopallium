package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Neuron;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ImageRecognitionNeuron extends Neuron {
    private List<ConvolutionalPerceptronNeuron> firstLayerNeurons;
    private List<ConvolutionalPerceptronNeuron> secondLayerNeurons;
    private Map<TargetClassification, ClassificationNeuron> classifierNeurons = new EnumMap<>(TargetClassification.class);
    private final ConvolutionalRecognitionProcessor processor = new ConvolutionalRecognitionProcessor();

    public ImageRecognitionNeuron() {
        super();
        this.currentNeuronClass = ImageRecognitionNeuron.class;
        this.resultClasses.add(RecognitionResultSignal.class);
        addSignalProcessor(CameraFrameSignal.class, processor);
        this.firstLayerNeurons = ConvolutionalRecognitionProcessor.defaultFirstLayerNeurons();
        this.secondLayerNeurons = ConvolutionalRecognitionProcessor.defaultSecondLayerNeurons();
    }

    public RecognitionResultSignal recognize(CameraFrameSignal frame) {
        return processor.recognize(frame, this);
    }

    public List<ConvolutionalPerceptronNeuron> getFirstLayerNeurons() {
        return firstLayerNeurons;
    }

    public void setFirstLayerNeurons(List<ConvolutionalPerceptronNeuron> firstLayerNeurons) {
        this.firstLayerNeurons = firstLayerNeurons;
    }

    public List<ConvolutionalPerceptronNeuron> getSecondLayerNeurons() {
        return secondLayerNeurons;
    }

    public void setSecondLayerNeurons(List<ConvolutionalPerceptronNeuron> secondLayerNeurons) {
        this.secondLayerNeurons = secondLayerNeurons;
    }

    public Map<TargetClassification, ClassificationNeuron> getClassifierNeurons() {
        return classifierNeurons;
    }

    public void setClassifierNeurons(Map<TargetClassification, ClassificationNeuron> classifierNeurons) {
        this.classifierNeurons = new EnumMap<>(TargetClassification.class);
        if (classifierNeurons != null) {
            this.classifierNeurons.putAll(classifierNeurons);
        }
    }

    public static int[][] templateFor(TargetClassification classification) {
        return ConvolutionalRecognitionProcessor.templateFor(classification);
    }

    public static String pixelHash(int[][] pixels) {
        return ConvolutionalRecognitionProcessor.pixelHash(pixels);
    }
}
