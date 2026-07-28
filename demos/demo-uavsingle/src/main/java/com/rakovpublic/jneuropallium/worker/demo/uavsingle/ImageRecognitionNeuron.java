package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Neuron;

public class ImageRecognitionNeuron extends Neuron implements IRecognitionNetworkNeuron {
    private final ConvolutionalRecognitionNetwork network;
    private final ConvolutionalRecognitionProcessor processor = new ConvolutionalRecognitionProcessor();

    public ImageRecognitionNeuron() {
        this(new ConvolutionalRecognitionNetwork());
    }

    public ImageRecognitionNeuron(ConvolutionalRecognitionNetwork network) {
        super();
        this.network = network == null ? new ConvolutionalRecognitionNetwork() : network;
        this.currentNeuronClass = ImageRecognitionNeuron.class;
        this.resultClasses.add(RecognitionResultSignal.class);
    }

    public RecognitionResultSignal recognize(CameraFrameSignal frame) {
        return processor.recognize(frame, this);
    }

    @Override
    public ConvolutionalRecognitionNetwork getNetwork() {
        return network;
    }

    public static int[][] templateFor(TargetClassification classification) {
        return ConvolutionalRecognitionProcessor.templateFor(classification);
    }

    public static String pixelHash(int[][] pixels) {
        return ConvolutionalRecognitionProcessor.pixelHash(pixels);
    }
}
