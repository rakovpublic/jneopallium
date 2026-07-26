package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UavSingleAutonomyTest {
    @Test
    void searchCoveragePlansWaypointsInsideSpecifiedArea() {
        UavSingleConfig config = new UavSingleConfig();
        SearchArea area = new SearchArea("inspection-sector-a", 0.0, 160.0, -40.0, 40.0,
                35.0, 80.0, 100.0);

        List<SearchWaypointSignal> waypoints = new NavigationSearchNeuron().plan(area, config, 12L);

        assertFalse(waypoints.isEmpty());
        assertEquals("inspection-sector-a", waypoints.get(0).getAreaId());
        assertTrue(waypoints.stream().allMatch(waypoint -> area.contains(waypoint.getX(), waypoint.getY())));
        assertTrue(waypoints.stream().allMatch(waypoint -> config.geofence.contains(waypoint.getX(), waypoint.getY())));
    }

    @Test
    void imageRecognitionClassifiesTargetFromPixelMatrixValues() {
        UavSingleConfig config = new UavSingleConfig();
        ObservationTarget target = new ObservationTarget("target-pixels",
                TargetClassification.COMMUNICATION_TOWER, 40.0, 10.0);

        CameraFrameSignal frame = SyntheticCameraFrameFactory.fromTarget(config, 4L, target, 1);
        RecognitionResultSignal result = new ImageRecognitionNeuron().recognize(frame);

        assertEquals("target-pixels", result.getTargetId());
        assertEquals(TargetClassification.COMMUNICATION_TOWER, result.getClassification());
        assertTrue(result.getConfidence() > 0.45);
        assertEquals("CONVOLUTIONAL_PERCEPTRON_NETWORK", result.getAttributes().get("source"));
        assertTrue(((Number) result.getAttributes().get("pixelPatchSignals")).intValue() > 0);
        assertTrue(((Number) result.getAttributes().get("conv1FeatureSignals")).intValue() > 0);
        assertTrue(((Number) result.getAttributes().get("conv2FeatureSignals")).intValue() > 0);
        assertTrue(result.getImageFeatures().get("classificationMargin") > 0.0);
    }

    @Test
    void cameraPatchInitInputSplitsFrameBeforeNeuronLayer() {
        UavSingleConfig config = new UavSingleConfig();
        ObservationTarget target = new ObservationTarget("target-init",
                TargetClassification.EMERGENCY_MARKER, 20.0, 10.0);
        CameraFrameSignal frame = SyntheticCameraFrameFactory.fromTarget(config, 1L, target, 1);

        List<IInputSignal> signals = new CameraFramePatchInitInput(
                "uav-fpv-3x3-patches", frame, RecognitionNetworkConfig.fpv1080p()).readSignals();

        assertEquals(16, signals.size());
        assertTrue(signals.stream().allMatch(PixelPatchSignal.class::isInstance));
        assertTrue(signals.stream().map(PixelPatchSignal.class::cast)
                .allMatch(signal -> signal.getPixels().length == 9));
    }

    @Test
    void recognitionNetworkConfigCovers1080pFpvFrame() {
        RecognitionNetworkConfig config = RecognitionNetworkConfig.fpv1080p();

        assertEquals(1920, config.getCameraWidth());
        assertEquals(1080, config.getCameraHeight());
        assertEquals(3, config.getPatchSize());
        assertEquals(1, config.getPatchStride());
        assertEquals(2067604L, config.firstLayerPatchCount());
        assertTrue(config.firstLayerSignalCapacity() > config.firstLayerPatchCount());
        assertTrue(config.secondLayerSignalCapacity() > config.firstLayerSignalCapacity());
    }

    @Test
    void convolutionalRecognitionToleratesNoisyNonExactPixels() {
        UavSingleConfig config = new UavSingleConfig();
        ObservationTarget target = new ObservationTarget("target-noisy",
                TargetClassification.BLOCKED_ROAD, 80.0, -5.0);
        CameraFrameSignal frame = SyntheticCameraFrameFactory.fromTarget(config, 4L, target, 1);
        int[][] noisy = frame.getPixels();
        for (int y = 0; y < noisy.length; y++) {
            for (int x = 0; x < noisy[y].length; x++) {
                noisy[y][x] = Math.max(0, Math.min(255, noisy[y][x] + ((x + y) % 2 == 0 ? 11 : -9)));
            }
        }
        frame.setPixels(noisy);

        RecognitionResultSignal result = new ImageRecognitionNeuron().recognize(frame);

        assertEquals(TargetClassification.BLOCKED_ROAD, result.getClassification());
        assertTrue(result.getConfidence() > 0.45);
    }

    @Test
    void imageRecognitionClassifiesInfantryAndVehiclePixelMatrices() {
        UavSingleConfig config = new UavSingleConfig();
        RecognitionResultSignal infantry = new ImageRecognitionNeuron().recognize(
                SyntheticCameraFrameFactory.fromTarget(config, 3L,
                        new ObservationTarget("target-infantry", TargetClassification.INFANTRY, 32.0, 12.0), 1));
        RecognitionResultSignal vehicle = new ImageRecognitionNeuron().recognize(
                SyntheticCameraFrameFactory.fromTarget(config, 4L,
                        new ObservationTarget("target-vehicle", TargetClassification.VEHICLE_TO_INSPECT, 70.0, -20.0), 2));

        assertEquals(TargetClassification.INFANTRY, infantry.getClassification());
        assertEquals(TargetClassification.VEHICLE_TO_INSPECT, vehicle.getClassification());
        assertTrue(infantry.getConfidence() > 0.45);
        assertTrue(vehicle.getConfidence() > 0.45);
    }

    @Test
    void convolutionalPerceptronConsumesExactlyNineInputs() {
        assertThrows(IllegalArgumentException.class, () -> new PixelPatchSignal(
                "mission-uav-single", "uav-1", 1L, "bad-frame", "conv1", 0, 0,
                new double[]{0.1, 0.2, 0.3}));

        ConvolutionalPerceptronNeuron neuron = new ConvolutionalPerceptronNeuron("conv1", "sum",
                new double[]{
                        0.10, 0.10, 0.10,
                        0.10, 0.10, 0.10,
                        0.10, 0.10, 0.10
                }, -0.10);
        PixelPatchSignal patch = new PixelPatchSignal("mission-uav-single", "uav-1", 1L,
                "frame-nine", "conv1", 0, 0,
                new double[]{
                        1.0, 0.8, 0.6,
                        0.4, 0.2, 0.1,
                        0.0, 0.3, 0.5
                });

        ConvolutionFeatureSignal signal = neuron.fire(patch);

        assertTrue(neuron instanceof IPixelPatchConvolutionNeuron);
        assertTrue(neuron instanceof IFeaturePatchConvolutionNeuron);
        assertEquals(IPixelPatchConvolutionNeuron.class, new PixelPatchConvolutionProcessor().getNeuronClass());
        assertEquals(IFeaturePatchConvolutionNeuron.class, new FeaturePatchConvolutionProcessor().getNeuronClass());
        assertEquals("NINE_INPUT_CONVOLUTIONAL_PERCEPTRON", signal.getAttributes().get("neuronType"));
        assertEquals("conv1", signal.getLayerName());
        assertEquals("sum", signal.getFilterName());
        assertTrue(signal.getActivation() > 0.0);
    }

    @Test
    void reinforcementFeedbackUpdatesClassifierPrototypeMatrix() {
        ConvolutionalRecognitionNetwork network = new ConvolutionalRecognitionNetwork();
        ImageRecognitionNeuron recognizer = new ImageRecognitionNeuron(network);
        RecognitionLearningNeuron learner = new RecognitionLearningNeuron(network);
        UavSingleConfig config = new UavSingleConfig();
        ObservationTarget target = new ObservationTarget("target-learn",
                TargetClassification.BLOCKED_ROAD, 45.0, 10.0);
        RecognitionResultSignal recognition = recognizer.recognize(
                SyntheticCameraFrameFactory.fromTarget(config, 1L, target, 1));
        ClassificationNeuron classifier = network.getClassifierNeurons().get(TargetClassification.BLOCKED_ROAD);
        double before = classifier.getPrototype().get("image.meanIntensity");
        Map<String, Double> shiftedFeatures = new LinkedHashMap<>(recognition.getImageFeatures());
        shiftedFeatures.put("image.meanIntensity", before + 0.20);

        RecognitionFeedbackSignal feedback = new RecognitionFeedbackSignal();
        feedback.setMissionId("mission-uav-single");
        feedback.setUavId("uav-1");
        feedback.setTick(2L);
        feedback.setTargetId("target-learn");
        feedback.setFrameId(recognition.getFrameId());
        feedback.setPredictedClassification(TargetClassification.VEHICLE_TO_INSPECT);
        feedback.setExpectedClassification(TargetClassification.BLOCKED_ROAD);
        feedback.setOutcome(RecognitionFeedbackOutcome.WRONG_TARGET);
        feedback.setReward(-1.0);
        feedback.setLearningRate(network.getConfig().getLearningRate());
        feedback.setImageFeatures(shiftedFeatures);

        RecognitionLearningResultSignal learning = learner.learn(feedback);

        assertTrue(learning.getUpdatedMatrices() >= 2);
        assertEquals("CLASSIFIER_PROTOTYPE_MATRIX_UPDATED", learning.getReason());
        assertNotEquals(before, classifier.getPrototype().get("image.meanIntensity"));
        assertTrue(learner.isChanged());
    }

    @Test
    void imageRecognitionCanClassifyUnknownObjectPixelMatrix() {
        CameraFrameSignal frame = new CameraFrameSignal();
        frame.setMissionId("mission-uav-single");
        frame.setUavId("uav-1");
        frame.setTick(1L);
        frame.setFrameId("frame-flat");
        frame.setTrackId("flat-track");
        frame.setFrameCenterX(0.0);
        frame.setFrameCenterY(0.0);
        frame.setPixels(new int[][]{
                {127, 127, 127, 127, 127, 127},
                {127, 127, 127, 127, 127, 127},
                {127, 127, 127, 127, 127, 127},
                {127, 127, 127, 127, 127, 127},
                {127, 127, 127, 127, 127, 127},
                {127, 127, 127, 127, 127, 127}
        });

        RecognitionResultSignal result = new ImageRecognitionNeuron().recognize(frame);

        assertEquals(TargetClassification.UNKNOWN_OBJECT, result.getClassification());
        assertTrue(result.getConfidence() > 0.45);
        assertEquals("CONVOLUTIONAL_PERCEPTRON_NETWORK", result.getAttributes().get("source"));
    }
}
