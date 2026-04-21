package com.rakovpublic.jneuropallium.ai.model;

import com.rakovpublic.jneuropallium.ai.enums.HarmVerdict;

/**
 * Factory accessible only by EthicalPriorityNeuron to create AbsoluteConstraint instances.
 * This keeps the package-private constructor accessible only through this factory.
 */
public final class AbsoluteConstraintFactory {

    private AbsoluteConstraintFactory() {}

    private static final double PHYSICAL_INTEGRITY_CATASTROPHIC = 0.05;
    private static final double INFORMATION_DECEPTION_THRESHOLD = 0.3;

    public static AbsoluteConstraint physicalFatalityConstraint() {
        return new AbsoluteConstraint(
            "CONSTRAINT_1",
            "Any action causing human fatality (physicalIntegrity -> 0) is always CATASTROPHIC",
            assessment -> {
                double[] scores = assessment.getHarmScores();
                return scores != null && scores.length > 0 && scores[0] >= (1.0 - PHYSICAL_INTEGRITY_CATASTROPHIC);
            },
            HarmVerdict.CATASTROPHIC
        );
    }

    public static AbsoluteConstraint informationDeceptionConstraint() {
        return new AbsoluteConstraint(
            "CONSTRAINT_2",
            "Any action involving deception of human operator is always HARMFUL",
            assessment -> {
                double[] scores = assessment.getHarmScores();
                return scores != null && scores.length > 3 && scores[3] >= INFORMATION_DECEPTION_THRESHOLD;
            },
            HarmVerdict.HARMFUL
        );
    }

    public static AbsoluteConstraint safetySystemTamperingConstraint() {
        return new AbsoluteConstraint(
            "CONSTRAINT_3",
            "Any action modifying safety thresholds or EthicalPriorityNeuron is always CATASTROPHIC",
            assessment -> {
                String[] conditions = assessment.getTriggeringConditions();
                if (conditions == null) return false;
                for (String c : conditions) {
                    if (c != null && (c.contains("dimensionThresholds") ||
                        c.contains("conservatismBias") || c.contains("EthicalPriority"))) {
                        return true;
                    }
                }
                return false;
            },
            HarmVerdict.CATASTROPHIC
        );
    }
}
