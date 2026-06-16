package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CameraFramePatchInitInput implements IInitInput {
    private final String name;
    private final List<CameraFrameSignal> frames;
    private final RecognitionNetworkConfig config;

    public CameraFramePatchInitInput(String name, CameraFrameSignal frame, RecognitionNetworkConfig config) {
        this(name, List.of(frame), config);
    }

    public CameraFramePatchInitInput(String name, List<CameraFrameSignal> frames, RecognitionNetworkConfig config) {
        this.name = name == null ? "uav-camera-patch-init" : name;
        this.frames = frames == null ? List.of() : new ArrayList<>(frames);
        this.config = config == null ? RecognitionNetworkConfig.fpv1080p() : config;
    }

    @Override
    public List<IInputSignal> readSignals() {
        List<IInputSignal> signals = new ArrayList<>();
        for (CameraFrameSignal frame : frames) {
            double[][] normalized = normalize(frame.getPixels());
            int height = normalized.length;
            int width = height == 0 ? 0 : normalized[0].length;
            int stride = config.getPatchStride();
            for (int y = 0; y <= height - 3; y += stride) {
                for (int x = 0; x <= width - 3; x += stride) {
                    PixelPatchSignal signal = new PixelPatchSignal(frame.getMissionId(), frame.getUavId(),
                            frame.getTick(), frame.getFrameId(), "conv1", x, y, patch(normalized, x, y));
                    signal.setInputName(name);
                    signals.add(signal);
                }
            }
        }
        return signals;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public HashMap<String, List<IResultSignal>> getDesiredResults() {
        return new HashMap<>();
    }

    @Override
    public ProcessingFrequency getDefaultProcessingFrequency() {
        return new ProcessingFrequency(1L, 1);
    }

    static double[][] normalize(int[][] pixels) {
        if (pixels == null || pixels.length == 0 || pixels[0].length == 0) {
            return new double[0][0];
        }
        double[][] normalized = new double[pixels.length][pixels[0].length];
        for (int y = 0; y < pixels.length; y++) {
            for (int x = 0; x < pixels[y].length; x++) {
                normalized[y][x] = Math.max(0, Math.min(255, pixels[y][x])) / 255.0;
            }
        }
        return normalized;
    }

    private static double[] patch(double[][] values, int x, int y) {
        double[] patch = new double[9];
        int index = 0;
        for (int dy = 0; dy < 3; dy++) {
            for (int dx = 0; dx < 3; dx++) {
                patch[index++] = values[y + dy][x + dx];
            }
        }
        return patch;
    }
}
