package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import java.util.ArrayList;
import java.util.List;

public class TargetEvaluationNeuron {
    private final TargetPriorityProcessor processor = new TargetPriorityProcessor();

    public List<TargetPriority> score(List<ObservationTarget> targets, UavPose pose, UavSingleConfig config) {
        List<TargetPriority> priorities = new ArrayList<>();
        for (ObservationTarget target : targets) {
            if (target.active) {
                priorities.add(processor.calculate(target, pose, config, 0.0));
            }
        }
        return priorities;
    }

    public TargetPriority select(List<TargetPriority> priorities) {
        return processor.selectBest(priorities);
    }
}

