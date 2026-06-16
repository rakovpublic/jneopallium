package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConvolutionalRecognitionProcessor implements ISignalProcessor<CameraFrameSignal, ImageRecognitionNeuron> {
    private static final int TEMPLATE_SIZE = 6;
    private static final Map<TargetClassification, int[][]> TEMPLATES = templates();

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(CameraFrameSignal input, ImageRecognitionNeuron neuron) {
        List<I> result = new ArrayList<>();
        result.add((I) recognize(input, neuron));
        return result;
    }

    public RecognitionResultSignal recognize(CameraFrameSignal frame, ImageRecognitionNeuron neuron) {
        ensureClassifierNeurons(neuron);
        Extraction extraction = extract(frame, neuron, true);
        FeatureVectorSignal vector = new FeatureVectorSignal();
        vector.setMissionId(frame.getMissionId());
        vector.setUavId(frame.getUavId());
        vector.setTick(frame.getTick());
        vector.setFrameId(frame.getFrameId());
        vector.setFeatures(extraction.features);

        ClassificationScoreSignal best = null;
        ClassificationScoreSignal runnerUp = null;
        Map<String, Double> scores = new LinkedHashMap<>();
        for (ClassificationNeuron classifier : neuron.getClassifierNeurons().values()) {
            ClassificationScoreSignal score = classifier.score(vector);
            scores.put(score.getClassification().name(), score.getScore());
            if (best == null || score.getScore() > best.getScore()) {
                runnerUp = best;
                best = score;
            } else if (runnerUp == null || score.getScore() > runnerUp.getScore()) {
                runnerUp = score;
            }
        }

        double runnerScore = runnerUp == null ? 0.0 : runnerUp.getScore();
        double margin = best == null ? 0.0 : best.getScore() - runnerScore;
        double confidence = best == null ? 0.0
                : TargetPriorityProcessor.clamp(best.getScore()
                * (0.65 + 0.35 * TargetPriorityProcessor.clamp(margin * 3.0)));
        TargetClassification classification = best == null || confidence < 0.25
                ? TargetClassification.UNKNOWN_OBJECT
                : best.getClassification();

        RecognitionResultSignal result = new RecognitionResultSignal();
        result.setMissionId(frame.getMissionId());
        result.setUavId(frame.getUavId());
        result.setTick(frame.getTick());
        result.setFrameId(frame.getFrameId());
        result.setTargetId(frame.getTrackId());
        result.setClassification(classification);
        result.setConfidence(confidence);
        result.setX(frame.getFrameCenterX());
        result.setY(frame.getFrameCenterY());

        Map<String, Double> features = new LinkedHashMap<>(extraction.features);
        features.put("bestConvolutionalScore", best == null ? 0.0 : best.getScore());
        features.put("runnerUpConvolutionalScore", runnerScore);
        features.put("classificationMargin", margin);
        result.setImageFeatures(features);
        result.attribute("pixelHash", pixelHash(frame.getPixels()));
        result.attribute("source", "CONVOLUTIONAL_PERCEPTRON_NETWORK");
        result.attribute("architecture", "3x3 pixel patches -> conv1 perceptrons -> conv2 perceptrons -> pooling -> classifier neurons");
        result.attribute("pixelPatchSignals", extraction.pixelPatchSignals);
        result.attribute("conv1FeatureSignals", extraction.conv1Signals);
        result.attribute("conv2FeatureSignals", extraction.conv2Signals);
        result.attribute("pooledFeatureSignals", extraction.pooledSignals);
        result.attribute("classifierScores", scores);
        return result;
    }

    public static List<ConvolutionalPerceptronNeuron> defaultFirstLayerNeurons() {
        return List.of(
                neuron("conv1", "vertical-edge", weights(
                        -1, 0, 1,
                        -1, 0, 1,
                        -1, 0, 1), 0.0),
                neuron("conv1", "horizontal-edge", weights(
                        -1, -1, -1,
                         0,  0,  0,
                         1,  1,  1), 0.0),
                neuron("conv1", "diagonal-down", weights(
                         1,  0, -1,
                         0,  1,  0,
                        -1,  0,  1), 0.0),
                neuron("conv1", "diagonal-up", weights(
                        -1,  0,  1,
                         0,  1,  0,
                         1,  0, -1), 0.0),
                neuron("conv1", "center-surround", weights(
                        -0.5, -0.5, -0.5,
                        -0.5,  4.0, -0.5,
                        -0.5, -0.5, -0.5), -0.10),
                neuron("conv1", "bright-mass", weights(
                         0.12, 0.12, 0.12,
                         0.12, 0.12, 0.12,
                         0.12, 0.12, 0.12), -0.25));
    }

    public static List<ConvolutionalPerceptronNeuron> defaultSecondLayerNeurons() {
        return List.of(
                neuron("conv2", "local-cluster", weights(
                        0.12, 0.12, 0.12,
                        0.12, 0.12, 0.12,
                        0.12, 0.12, 0.12), -0.05),
                neuron("conv2", "center-peak", weights(
                       -0.05, -0.05, -0.05,
                       -0.05,  0.80, -0.05,
                       -0.05, -0.05, -0.05), 0.0),
                neuron("conv2", "left-right-pair", weights(
                        0.20, 0.00, 0.20,
                        0.20, 0.00, 0.20,
                        0.20, 0.00, 0.20), -0.02),
                neuron("conv2", "top-bottom-pair", weights(
                        0.20, 0.20, 0.20,
                        0.00, 0.00, 0.00,
                        0.20, 0.20, 0.20), -0.02));
    }

    public static int[][] templateFor(TargetClassification classification) {
        return CameraFrameSignal.copyPixels(TEMPLATES.getOrDefault(classification,
                TEMPLATES.get(TargetClassification.UNKNOWN_OBJECT)));
    }

    public static String pixelHash(int[][] pixels) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (int[] row : pixels == null ? new int[0][0] : pixels) {
                if (row == null) {
                    digest.update((byte) 0);
                    continue;
                }
                for (int value : row) {
                    digest.update((byte) clampPixel(value));
                }
                digest.update((byte) ';');
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static void ensureClassifierNeurons(ImageRecognitionNeuron neuron) {
        if (!neuron.getClassifierNeurons().isEmpty()) {
            return;
        }
        Map<TargetClassification, ClassificationNeuron> classifiers = new EnumMap<>(TargetClassification.class);
        for (Map.Entry<TargetClassification, int[][]> entry : TEMPLATES.entrySet()) {
            CameraFrameSignal frame = new CameraFrameSignal();
            frame.setMissionId("prototype");
            frame.setUavId("prototype");
            frame.setTick(0L);
            frame.setFrameId("prototype-" + entry.getKey().name());
            frame.setTrackId(entry.getKey().name());
            frame.setPixels(entry.getValue());
            Extraction prototype = extract(frame, neuron, false);
            classifiers.put(entry.getKey(), new ClassificationNeuron(entry.getKey(), prototype.features));
        }
        neuron.setClassifierNeurons(classifiers);
    }

    private static Extraction extract(CameraFrameSignal frame, ImageRecognitionNeuron neuron, boolean countSignals) {
        double[][] normalized = normalize(frame.getPixels());
        int height = normalized.length;
        int width = height == 0 ? 0 : normalized[0].length;
        List<PixelPatchSignal> pixelPatches = pixelPatches(frame, normalized);
        List<ConvolutionFeatureSignal> conv1 = new ArrayList<>();
        PixelPatchConvolutionProcessor pixelProcessor = new PixelPatchConvolutionProcessor();
        for (PixelPatchSignal patch : pixelPatches) {
            for (ConvolutionalPerceptronNeuron convNeuron : neuron.getFirstLayerNeurons()) {
                conv1.add((ConvolutionFeatureSignal) pixelProcessor.process(patch, convNeuron).get(0));
            }
        }
        Map<String, double[][]> conv1Maps = maps(conv1, Math.max(0, height - 2), Math.max(0, width - 2));
        List<FeaturePatchSignal> featurePatches = featurePatches(frame, conv1Maps);
        List<ConvolutionFeatureSignal> conv2 = new ArrayList<>();
        FeaturePatchConvolutionProcessor featureProcessor = new FeaturePatchConvolutionProcessor();
        for (FeaturePatchSignal patch : featurePatches) {
            for (ConvolutionalPerceptronNeuron convNeuron : neuron.getSecondLayerNeurons()) {
                ConvolutionFeatureSignal signal = (ConvolutionFeatureSignal) featureProcessor.process(patch, convNeuron).get(0);
                signal.setFilterName(patch.getSourceFeatureName() + "." + signal.getFilterName());
                conv2.add(signal);
            }
        }
        Map<String, double[][]> conv2Maps = maps(conv2,
                conv1Maps.values().stream().findFirst().map(map -> Math.max(0, map.length - 2)).orElse(0),
                conv1Maps.values().stream().findFirst().map(map -> map.length == 0 ? 0 : Math.max(0, map[0].length - 2)).orElse(0));

        Map<String, Double> features = new LinkedHashMap<>();
        features.put("image.meanIntensity", meanIntensity(frame.getPixels()));
        features.put("image.contrast", contrast(frame.getPixels()));
        List<PooledFeatureSignal> pooled = new ArrayList<>();
        pooled.addAll(pool(frame, "conv1", conv1Maps, features));
        pooled.addAll(pool(frame, "conv2", conv2Maps, features));
        return new Extraction(features,
                countSignals ? pixelPatches.size() : 0,
                countSignals ? conv1.size() : 0,
                countSignals ? conv2.size() : 0,
                countSignals ? pooled.size() : 0);
    }

    private static List<PixelPatchSignal> pixelPatches(CameraFrameSignal frame, double[][] normalized) {
        List<PixelPatchSignal> patches = new ArrayList<>();
        int height = normalized.length;
        int width = height == 0 ? 0 : normalized[0].length;
        for (int y = 0; y <= height - 3; y++) {
            for (int x = 0; x <= width - 3; x++) {
                patches.add(new PixelPatchSignal(frame.getMissionId(), frame.getUavId(), frame.getTick(),
                        frame.getFrameId(), "conv1", x, y, patch(normalized, x, y)));
            }
        }
        return patches;
    }

    private static List<FeaturePatchSignal> featurePatches(CameraFrameSignal frame, Map<String, double[][]> maps) {
        List<FeaturePatchSignal> patches = new ArrayList<>();
        for (Map.Entry<String, double[][]> entry : maps.entrySet()) {
            double[][] map = entry.getValue();
            int height = map.length;
            int width = height == 0 ? 0 : map[0].length;
            for (int y = 0; y <= height - 3; y++) {
                for (int x = 0; x <= width - 3; x++) {
                    patches.add(new FeaturePatchSignal(frame.getMissionId(), frame.getUavId(), frame.getTick(),
                            frame.getFrameId(), "conv1", entry.getKey(), "conv2", x, y, patch(map, x, y)));
                }
            }
        }
        return patches;
    }

    private static Map<String, double[][]> maps(List<ConvolutionFeatureSignal> signals, int height, int width) {
        Map<String, double[][]> result = new LinkedHashMap<>();
        for (ConvolutionFeatureSignal signal : signals) {
            double[][] map = result.computeIfAbsent(signal.getFilterName(), ignored -> new double[height][width]);
            if (signal.getPatchY() >= 0 && signal.getPatchY() < height
                    && signal.getPatchX() >= 0 && signal.getPatchX() < width) {
                map[signal.getPatchY()][signal.getPatchX()] = signal.getActivation();
            }
        }
        return result;
    }

    private static List<PooledFeatureSignal> pool(CameraFrameSignal frame, String layerName,
                                                   Map<String, double[][]> maps, Map<String, Double> features) {
        List<PooledFeatureSignal> pooled = new ArrayList<>();
        for (Map.Entry<String, double[][]> entry : maps.entrySet()) {
            PoolStats stats = stats(entry.getValue());
            PooledFeatureSignal signal = new PooledFeatureSignal();
            signal.setMissionId(frame.getMissionId());
            signal.setUavId(frame.getUavId());
            signal.setTick(frame.getTick());
            signal.setFrameId(frame.getFrameId());
            signal.setLayerName(layerName);
            signal.setFilterName(entry.getKey());
            signal.setMaxActivation(stats.max);
            signal.setAverageActivation(stats.average);
            signal.setActivePatchFraction(stats.activeFraction);
            pooled.add(signal);
            String prefix = layerName + "." + entry.getKey();
            features.put(prefix + ".max", stats.max);
            features.put(prefix + ".avg", stats.average);
            features.put(prefix + ".activeFraction", stats.activeFraction);
        }
        return pooled;
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

    private static double[][] normalize(int[][] pixels) {
        if (pixels == null || pixels.length == 0 || pixels[0].length == 0) {
            return new double[0][0];
        }
        double[][] normalized = new double[pixels.length][pixels[0].length];
        for (int y = 0; y < pixels.length; y++) {
            for (int x = 0; x < pixels[y].length; x++) {
                normalized[y][x] = clampPixel(pixels[y][x]) / 255.0;
            }
        }
        return normalized;
    }

    private static double meanIntensity(int[][] pixels) {
        double total = 0.0;
        int count = 0;
        for (int[] row : pixels == null ? new int[0][0] : pixels) {
            if (row == null) {
                continue;
            }
            for (int value : row) {
                total += clampPixel(value);
                count++;
            }
        }
        return count == 0 ? 0.0 : total / (255.0 * count);
    }

    private static double contrast(int[][] pixels) {
        if (pixels == null || pixels.length == 0 || pixels[0].length == 0) {
            return 0.0;
        }
        int min = 255;
        int max = 0;
        for (int[] row : pixels) {
            if (row == null) {
                continue;
            }
            for (int value : row) {
                int pixel = clampPixel(value);
                min = Math.min(min, pixel);
                max = Math.max(max, pixel);
            }
        }
        return (max - min) / 255.0;
    }

    private static PoolStats stats(double[][] map) {
        double max = 0.0;
        double total = 0.0;
        int active = 0;
        int count = 0;
        for (double[] row : map) {
            for (double value : row) {
                max = Math.max(max, value);
                total += value;
                if (value > 0.05) {
                    active++;
                }
                count++;
            }
        }
        return new PoolStats(max, count == 0 ? 0.0 : total / count, count == 0 ? 0.0 : active / (double) count);
    }

    private static int clampPixel(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static ConvolutionalPerceptronNeuron neuron(String layer, String name, double[] weights, double bias) {
        return new ConvolutionalPerceptronNeuron(layer, name, weights, bias);
    }

    private static double[] weights(double... weights) {
        return weights;
    }

    private static Map<TargetClassification, int[][]> templates() {
        Map<TargetClassification, int[][]> templates = new EnumMap<>(TargetClassification.class);
        templates.put(TargetClassification.DAMAGED_INFRASTRUCTURE, matrix(
                "220  40  40  40  40  40",
                " 40 220  40  40  40  40",
                " 40  40 220  40  40  40",
                " 40  40  40 220  40  40",
                " 40  40  40  40 220  40",
                " 40  40  40  40  40 220"));
        templates.put(TargetClassification.VEHICLE_TO_INSPECT, matrix(
                " 35  35  35  35  35  35",
                " 35  35  35  35  35  35",
                "210 210 210 210 210 210",
                "210 210 210 210 210 210",
                " 35  35  35  35  35  35",
                " 35  35  35  35  35  35"));
        templates.put(TargetClassification.WILDFIRE_HOTSPOT, matrix(
                "240 220 200 180 200 220",
                "220 250 230 210 230 200",
                "200 230 255 240 210 190",
                "180 210 240 255 230 200",
                "200 230 210 230 250 220",
                "220 200 190 200 220 240"));
        templates.put(TargetClassification.MISSING_PERSON_MARKER, matrix(
                " 30  30 230 230  30  30",
                " 30  30 230 230  30  30",
                "230 230 230 230 230 230",
                "230 230 230 230 230 230",
                " 30  30 230 230  30  30",
                " 30  30 230 230  30  30"));
        templates.put(TargetClassification.BLOCKED_ROAD, matrix(
                " 50  50  50  50  50  50",
                " 50  50  50  50  50  50",
                "230 230 230 230 230 230",
                " 50  50  50  50  50  50",
                "230 230 230 230 230 230",
                " 50  50  50  50  50  50"));
        templates.put(TargetClassification.UNKNOWN_OBJECT, matrix(
                " 90 120  95 125 100 130",
                "115  85 125  95 130 100",
                "100 130  90 120  95 125",
                "125  95 115  85 125  95",
                " 95 125 100 130  90 120",
                "130 100 125  95 115  85"));
        templates.put(TargetClassification.COMMUNICATION_TOWER, matrix(
                " 35  35 230 230  35  35",
                " 35  35 230 230  35  35",
                " 35  90 230 230  90  35",
                " 90  35 230 230  35  90",
                " 35  35 230 230  35  35",
                " 35  35 230 230  35  35"));
        templates.put(TargetClassification.EMERGENCY_MARKER, matrix(
                "230  40  40  40  40 230",
                " 40 230  40  40 230  40",
                " 40  40 230 230  40  40",
                " 40  40 230 230  40  40",
                " 40 230  40  40 230  40",
                "230  40  40  40  40 230"));
        templates.put(TargetClassification.OPERATOR_POINT, matrix(
                " 40  40  40 230  40  40",
                " 40  40 230 230 230  40",
                " 40 230  40 230  40 230",
                "230  40  40 230  40  40",
                " 40  40  40 230  40  40",
                " 40  40  40 230  40  40"));
        return templates;
    }

    private static int[][] matrix(String... rows) {
        int[][] pixels = new int[rows.length][TEMPLATE_SIZE];
        for (int y = 0; y < rows.length; y++) {
            String[] parts = rows[y].trim().split("\\s+");
            for (int x = 0; x < TEMPLATE_SIZE; x++) {
                pixels[y][x] = Integer.parseInt(parts[x]);
            }
        }
        return pixels;
    }

    @Override public String getDescription() { return "convolutional UAV image recognition processor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return ConvolutionalRecognitionProcessor.class; }
    @Override public Class<ImageRecognitionNeuron> getNeuronClass() { return ImageRecognitionNeuron.class; }
    @Override public Class<CameraFrameSignal> getSignalClass() { return CameraFrameSignal.class; }

    private record Extraction(Map<String, Double> features, int pixelPatchSignals, int conv1Signals,
                              int conv2Signals, int pooledSignals) {
    }

    private record PoolStats(double max, double average, double activeFraction) {
    }
}
