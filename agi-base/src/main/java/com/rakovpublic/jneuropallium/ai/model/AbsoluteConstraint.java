package com.rakovpublic.jneuropallium.ai.model;

import com.rakovpublic.jneuropallium.ai.enums.HarmVerdict;
import com.rakovpublic.jneuropallium.ai.signals.fast.HarmAssessmentSignal;

import java.util.function.Predicate;

/**
 * An immutable hard ethical constraint.
 * Can only be instantiated by EthicalPriorityNeuron.
 * Constraints can only escalate verdicts — never downgrade.
 */
public final class AbsoluteConstraint {
    final String constraintId;
    final String description;
    final Predicate<HarmAssessmentSignal> trigger;
    final HarmVerdict escalateTo;

    /** Package-private constructor — only EthicalPriorityNeuron (same package via neurons.harm) may create instances. */
    AbsoluteConstraint(String constraintId, String description,
                       Predicate<HarmAssessmentSignal> trigger, HarmVerdict escalateTo) {
        this.constraintId = constraintId;
        this.description = description;
        this.trigger = trigger;
        this.escalateTo = escalateTo;
    }

    public String getConstraintId() { return constraintId; }
    public String getDescription() { return description; }
    public Predicate<HarmAssessmentSignal> getTrigger() { return trigger; }
    public HarmVerdict getEscalateTo() { return escalateTo; }
}
