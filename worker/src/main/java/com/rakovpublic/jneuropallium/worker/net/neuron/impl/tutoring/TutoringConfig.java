/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Configuration for the adaptive-tutoring use case. Mirrors the
 * {@code tutoring:} section in the project YAML (see
 * {@code use-case-adaptive-tutoring.md} §8). Defaults to
 * {@code enabled=false} so the tutor must be explicitly switched on.
 */
public class TutoringConfig {

    private boolean enabled = false;
    private boolean instancePerLearner = true;

    private String curriculumSource = "curriculum.yaml";
    private String prerequisiteGraphPath = "prereq.json";

    private double zpdTargetSuccessRate = 0.75;
    private int zpdWindowItems = 10;

    private int hintMaxLevels = 3;
    private int hintDelayBetweenSeconds = 15;

    private int pacingFastSlowRatioMin = 5;
    private int pacingFastSlowRatioMax = 20;

    private int wellbeingMaxFrustrationTicks = 400;
    private int wellbeingMandatoryBreakMinutes = 45;

    private boolean affectEnabled = true;
    private int affectValenceDecayTicks = 300;

    private boolean curiosityEnabled = true;
    private double curiosityBetaNovelty = 0.15;

    private boolean sleepEnabled = true;
    private boolean sleepReplaySelectsLowWeight = true;

    private boolean llmEnabled = true;
    private String llmMode = "hints-and-examples";
    private String llmVerificationStrictness = "high";

    private List<String> fairnessAccommodationFlags = new ArrayList<>(
            Arrays.asList("extra-time", "screen-reader", "reduced-animation"));
    private boolean fairnessResponseTimePenalty = false;

    public TutoringConfig() {}

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isInstancePerLearner() { return instancePerLearner; }
    public void setInstancePerLearner(boolean v) { this.instancePerLearner = v; }

    public String getCurriculumSource() { return curriculumSource; }
    public void setCurriculumSource(String s) { this.curriculumSource = s; }

    public String getPrerequisiteGraphPath() { return prerequisiteGraphPath; }
    public void setPrerequisiteGraphPath(String s) { this.prerequisiteGraphPath = s; }

    public double getZpdTargetSuccessRate() { return zpdTargetSuccessRate; }
    public void setZpdTargetSuccessRate(double v) { this.zpdTargetSuccessRate = v; }

    public int getZpdWindowItems() { return zpdWindowItems; }
    public void setZpdWindowItems(int v) { this.zpdWindowItems = v; }

    public int getHintMaxLevels() { return hintMaxLevels; }
    public void setHintMaxLevels(int v) { this.hintMaxLevels = v; }

    public int getHintDelayBetweenSeconds() { return hintDelayBetweenSeconds; }
    public void setHintDelayBetweenSeconds(int v) { this.hintDelayBetweenSeconds = v; }

    public int getPacingFastSlowRatioMin() { return pacingFastSlowRatioMin; }
    public void setPacingFastSlowRatioMin(int v) { this.pacingFastSlowRatioMin = v; }

    public int getPacingFastSlowRatioMax() { return pacingFastSlowRatioMax; }
    public void setPacingFastSlowRatioMax(int v) { this.pacingFastSlowRatioMax = v; }

    public int getWellbeingMaxFrustrationTicks() { return wellbeingMaxFrustrationTicks; }
    public void setWellbeingMaxFrustrationTicks(int v) { this.wellbeingMaxFrustrationTicks = v; }

    public int getWellbeingMandatoryBreakMinutes() { return wellbeingMandatoryBreakMinutes; }
    public void setWellbeingMandatoryBreakMinutes(int v) { this.wellbeingMandatoryBreakMinutes = v; }

    public boolean isAffectEnabled() { return affectEnabled; }
    public void setAffectEnabled(boolean v) { this.affectEnabled = v; }

    public int getAffectValenceDecayTicks() { return affectValenceDecayTicks; }
    public void setAffectValenceDecayTicks(int v) { this.affectValenceDecayTicks = v; }

    public boolean isCuriosityEnabled() { return curiosityEnabled; }
    public void setCuriosityEnabled(boolean v) { this.curiosityEnabled = v; }

    public double getCuriosityBetaNovelty() { return curiosityBetaNovelty; }
    public void setCuriosityBetaNovelty(double v) { this.curiosityBetaNovelty = v; }

    public boolean isSleepEnabled() { return sleepEnabled; }
    public void setSleepEnabled(boolean v) { this.sleepEnabled = v; }

    public boolean isSleepReplaySelectsLowWeight() { return sleepReplaySelectsLowWeight; }
    public void setSleepReplaySelectsLowWeight(boolean v) { this.sleepReplaySelectsLowWeight = v; }

    public boolean isLlmEnabled() { return llmEnabled; }
    public void setLlmEnabled(boolean v) { this.llmEnabled = v; }

    public String getLlmMode() { return llmMode; }
    public void setLlmMode(String s) { this.llmMode = s; }

    public String getLlmVerificationStrictness() { return llmVerificationStrictness; }
    public void setLlmVerificationStrictness(String s) { this.llmVerificationStrictness = s; }

    public List<String> getFairnessAccommodationFlags() { return fairnessAccommodationFlags; }
    public void setFairnessAccommodationFlags(List<String> v) { this.fairnessAccommodationFlags = v; }

    public boolean isFairnessResponseTimePenalty() { return fairnessResponseTimePenalty; }
    public void setFairnessResponseTimePenalty(boolean v) { this.fairnessResponseTimePenalty = v; }
}
