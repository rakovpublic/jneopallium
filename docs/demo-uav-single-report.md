# Simulation-Only Single UAV Report

## Executive Summary

The single-UAV demo implements a Java 17 deterministic simulator for photography, reconnaissance, inspection, and search-and-rescue style observation targets. It now covers a specified search area, recognizes targets from `int[][]` camera pixel matrices before target evaluation, and can continue a mission to photograph both infantry and vehicle targets. It supports `FULLY_AUTONOMOUS` and `TARGET_CONFIRM`, writes reproducible evidence under `target/jneopallium-uav-single/<scenario>/`, and keeps all command output behind `SimUavMissionSupervisor`.

## Code Map

- `UavOperatingMode`, `UavMissionState`, `UavActionType`: mode and mission vocabulary.
- `ObservationTarget`, `TargetClassification`, `TargetPriority`: target model and transparent scoring.
- `SearchArea`, `SearchAreaSignal`, `SearchWaypointSignal`: specified search-sector model and route signals.
- `CameraFrameSignal`, `CameraFramePatchInitInput`, `PixelPatchSignal`, `FeaturePatchSignal`, `RecognitionResultSignal`: pixel-matrix camera input, init-input 3x3 splitting, feature windows, and recognition result.
- `ConvolutionFeatureSignal`, `PooledFeatureSignal`, `FeatureVectorSignal`, `ClassificationScoreSignal`: convolution-layer outputs, pooled feature vectors, and classifier scores.
- `RecognitionFeedbackSignal`, `RecognitionLearningResultSignal`: simulator feedback and model-update evidence.
- `TargetDetectionSignal`, `TargetCandidateSignal`, `TargetSelectedSignal`: typed target events.
- `TargetConfirmationRequestSignal`, `TargetConfirmationResponseSignal`: simulator-safe confirmation protocol.
- `PhotographRequestSignal`, `PhotographResultSignal`: simulated camera request/result records.
- `UavIntentSignal`: high-level intent passed to the mission supervisor.
- `SearchCoverageNeuron`, `NavigationSearchNeuron`: deterministic coverage planning for the configured area.
- `ImageRecognitionNeuron`, `ConvolutionalRecognitionProcessor`, `ConvolutionalPerceptronNeuron`: Jneopallium-style convolutional recognition path over pixel values.
- `IPixelPatchConvolutionNeuron`, `IFeaturePatchConvolutionNeuron`, `IFeatureClassificationNeuron`, `IRecognitionNetworkNeuron`, `IRecognitionLearningNeuron`: processor-facing neuron capability interfaces.
- `PixelPatchConvolutionProcessor`, `FeaturePatchConvolutionProcessor`, `ClassificationNeuron`, `ClassificationProcessor`: 3x3 patch processors and feature-vector classifier neurons.
- `RecognitionNetworkConfig`, `ConvolutionalRecognitionNetwork`, `RecognitionLearningNeuron`, `RecognitionLearningProcessor`: 1080p network layout and simulator reinforcement feedback.
- `SyntheticCameraFrameFactory`: deterministic camera-frame generation for replayable simulator scenarios.
- `SimUavMissionSupervisor`: safety boundary for every intent.
- `SimUavCommandGateway`: simulator adapter that emits non-executable `MotorCommandSignal`.
- `UavSingleSimulation`, `UavSingleDemoLauncher`: deterministic scenario runner and artifact writer.

## Mission Modes

`FULLY_AUTONOMOUS` covers the configured search area, recognizes targets from camera pixels, scores recognized candidates, selects the highest priority target, approaches a safe observation point, photographs if quality constraints pass, and returns home.

`TARGET_CONFIRM` covers the configured search area, recognizes and scores candidates autonomously, then enters `HOLDING_FOR_CONFIRMATION`. It only approaches or photographs after a confirmation response matches request ID, target ID, mission ID, UAV ID, action, and expiry tick.

## Acceptance Evidence

The built-in scenario suite covers:

- target priority and deterministic replanning
- search-area waypoint coverage
- convolutional-style image recognition from `int[][]` pixel matrices
- init-input 3x3 patch generation before neuron processing
- noisy non-exact pixel classification through 3x3 perceptron feature layers
- multi-observation autonomous photography of infantry and vehicle targets
- reinforcement feedback from accepted/rejected photos, missed targets, and wrong target classes
- approved, denied, stale, and duplicate confirmation responses
- geofence and no-go-zone rejection
- low battery return-to-home
- heartbeat loss recovery
- poor visibility retry limits
- no executable real-vehicle command path

Each manifest includes `assertions` for the scenario-specific acceptance checks. The root launcher returns nonzero if any manifest status is not `PASS`.

## Test Surface

Tests added under:

```text
worker/src/test/java/com/rakovpublic/jneuropallium/worker/demo/uavsingle/
```

Coverage includes:

- priority formula and tie-breaking
- search coverage planning
- pixel-matrix recognition, 3x3 perceptron input validation, and unknown-object classification
- interface-bound processor contracts and 1080p first-layer coverage
- recognition feedback updating classifier prototype matrices
- confirmation matching, expiry, and duplicate rejection
- illegal mission transition rejection
- geofence, no-go-zone, battery, and heartbeat vetoes
- simulator command audit generation and `execute=false`
- photograph quality validation
- deterministic replay
- all built-in scenario manifests

The tests do not require Gazebo, ROS 2, MAVLink, network access, or SITL.

## One-Command Execution

```bash
scripts/demo-uav-single/run_demo.sh all
```

```powershell
scripts/demo-uav-single/run_demo.ps1 all
```

The scripts build Maven modules, run `UavSingle*Test`, execute scenarios, validate artifacts, print the launcher CSV table, and fail on any assertion failure.

## Artifact Contract

For each scenario:

- `manifest.json`: status, mode, metrics, artifact paths, assertions.
- `summary.json`: scenario, mode, status, ticks, target/confirmation/photo/safety counts, battery and distance minima, search-area metrics, camera-frame counts, recognition counts, deterministic seed.
- `mission-events.jsonl`: state transitions and mission-level decisions.
- `search-events.jsonl`: search-area definition, planned waypoints, visited waypoints, and search-route safety rejections.
- `target-events.jsonl`: detections, candidates, target selection, target appearances.
- `recognition-events.jsonl`: captured camera frames, pixel hashes, 3x3 patch counts, convolution feature counts, classifier scores, and recognition results.
- `learning-events.jsonl`: reinforcement feedback outcome, reward, expected/predicted classes, and updated classifier matrices.

The recognizer pipeline is:

```text
CameraFrameSignal int[][] pixels
-> CameraFramePatchInitInput
-> PixelPatchSignal 3x3 normalized pixels
-> conv1 ConvolutionalPerceptronNeuron
-> FeaturePatchSignal 3x3 feature activations
-> conv2 ConvolutionalPerceptronNeuron
-> PooledFeatureSignal / FeatureVectorSignal
-> ClassificationNeuron
-> RecognitionResultSignal
```

Each convolution neuron is a perceptron with nine inputs and a bias. Processors are typed to capability interfaces, so a neuron that supports pixel and feature patch processing implements both interfaces. Class templates initialize deterministic feature prototypes, but runtime classification is by pooled feature-vector distance rather than exact pixel equality.

The default `RecognitionNetworkConfig.fpv1080p()` uses 1920x1080, 3x3 patches, and stride 1, giving 2,067,604 first-layer patch positions. Simulator feedback from `RecognitionFeedbackSignal` updates classifier prototype matrices through `RecognitionLearningNeuron`; accepted photos reinforce the expected class, wrong/missed targets move the expected class toward the observed feature vector and repel the incorrect prediction.

The generated resource model is `worker/src/main/resources/model/uav-single-fpv/`. Its descriptor declares exactly 10,000 real neuron entries: 4,096 Conv1 pixel-patch perceptrons, 4,096 Conv2 feature-patch perceptrons, 1,800 classifier prototype neurons, 7 reinforcement-learning feedback neurons, and 1 recognition-result neuron. The classifier layer is split across ten target classes at 180 neurons per class, including `INFANTRY` and `VEHICLE_TO_INSPECT`.
- `confirmation-events.jsonl`: requests and confirmation decisions.
- `photograph-events.jsonl`: accepted/rejected simulated photographs and metadata.
- `supervisor-audit.jsonl`: every supervisor decision row.
- `transparency.jsonl`: target priority factors and scores.
- `safety-summary.json`: simulator-only safety summary.

## Remaining Integration Limits

The implementation intentionally defaults to an in-memory simulator. `mavlinkEndpoint` and `ros2Endpoint` are present in versioned config so a future adapter can connect ArduPilot SITL or Gazebo without changing the mission logic. That adapter should preserve the current high-level intent boundary, scenario-independent supervisor checks, audit rows, vehicle allowlists, and confirmation gate.
