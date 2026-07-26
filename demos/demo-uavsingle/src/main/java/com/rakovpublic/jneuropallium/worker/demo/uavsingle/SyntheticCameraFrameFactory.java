package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

public final class SyntheticCameraFrameFactory {
    private SyntheticCameraFrameFactory() {
    }

    public static CameraFrameSignal fromTarget(UavSingleConfig config, long tick, ObservationTarget target, int frameSequence) {
        CameraFrameSignal frame = new CameraFrameSignal();
        frame.setMissionId(config.missionId);
        frame.setUavId(config.uavId);
        frame.setTick(tick);
        frame.setFrameId("frame-" + config.missionId + "-" + frameSequence + "-" + target.targetId);
        frame.setTrackId(target.targetId);
        frame.setFrameCenterX(target.x);
        frame.setFrameCenterY(target.y);
        frame.setPixels(degradeForVisibility(ImageRecognitionNeuron.templateFor(target.classification),
                target.visibility, target.motionBlurEstimate));
        frame.attribute("simulatedCamera", true);
        frame.attribute("pixelHash", ImageRecognitionNeuron.pixelHash(frame.getPixels()));
        return frame;
    }

    private static int[][] degradeForVisibility(int[][] pixels, double visibility, double motionBlurEstimate) {
        int[][] copy = CameraFrameSignal.copyPixels(pixels);
        double clarity = TargetPriorityProcessor.clamp(0.55 + 0.45 * visibility - 0.25 * motionBlurEstimate);
        for (int y = 0; y < copy.length; y++) {
            for (int x = 0; x < copy[y].length; x++) {
                int original = copy[y][x];
                int background = 110 + ((x + y) % 3) * 8;
                copy[y][x] = (int) Math.round(background * (1.0 - clarity) + original * clarity);
            }
        }
        return copy;
    }
}
