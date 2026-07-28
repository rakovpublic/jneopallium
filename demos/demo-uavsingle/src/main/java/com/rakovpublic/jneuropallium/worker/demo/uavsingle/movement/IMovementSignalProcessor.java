package com.rakovpublic.jneuropallium.worker.demo.uavsingle.movement;

/**
 * Heterogeneous movement signal processor.
 *
 * <p>Each implementation consumes one kind of navigation evidence exposed by the public simulator
 * observation (coverage, target geometry, occlusion feedback, obstacle ray-casts, area bounds,
 * altitude band, search progress) and emits a single bounded scalar feature for a candidate
 * {@link MovementAction}. The action neurons only ever see processor-produced features, never the
 * raw observation, which keeps the policy able to mix structurally different ("heterogeneous")
 * signals through a uniform interface — exactly the processor/interface contract used elsewhere in
 * the jneopallium worker.
 */
public interface IMovementSignalProcessor {
    /** Stable processor id, recorded in the exported model and decision artifacts. */
    String processorId();

    /** Name of the navigation signal interface this processor consumes. */
    String interfaceName();

    /** Name of the feature (dendrite key) this processor contributes. */
    String featureName();

    /** One-line human description of the heuristic. */
    String description();

    /**
     * @param network the policy network (provides mutable coverage / altitude / occlusion state and
     *                geometry helpers, analogous to the Python {@code controller})
     * @param observation the public simulator observation for the current control tick
     * @param action the candidate movement action being scored
     * @return bounded feature contribution, typically in [-1, 1]
     */
    double consume(MovementPolicyNetwork network, MovementObservationSignal observation, MovementAction action);
}
