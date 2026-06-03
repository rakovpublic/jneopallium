# Demo 08 Report: Adaptive Tutoring LTI/xAPI Advisory

## Executive Summary

This demo shows advisory learner-state processing for an LTI/xAPI-like tutoring system. It runs Jneopallium in local mode with generated model packaging, generated layer metadata, deterministic learner events, and JSONL aggregation.

The default full-run verification completed with status `PASS`, processed `100` ticks, and wrote `100` output rows.

## Application Story

The scenario compares two synthetic learners on a fractions topic. `learner-struggling` repeatedly answers incorrectly with long response times and occasional hint requests. `learner-strong` answers correctly with fast response times. The network recommends hints or lower difficulty for the struggling learner and harder exercises for the strong learner.

## Full-Run Execution Path

Generated artifacts are written under:

```text
target/jneopallium-fullrun-demos/demo-08-adaptive-tutoring-lti/
```

The worker is started through `Entry` in local mode with a generated model JAR and generated context JSON.

## Network Structure

Layer configuration:

- Layer 0, size 4: learner event input.
- Layer 1, size 3: mastery estimation.
- Layer 2, size 2: affect and tutoring policy.
- Layer 3, size 2: result conversion.

Typed signal classes:

- `LearnerEventSignal`
- `MasterySignal`
- `AffectSignal`
- `TutorAdvisorySignal`

Neuron and processor classes:

- `AdaptiveTutoringNeuron`
- `AdaptiveTutoringResultNeuron`
- `AdaptiveTutoringProcessor`
- `AdaptiveTutoringDemoInput`

## I/O Logic

Input is deterministic learner-event data: answer correctness, response time, hint request, and topic id. Output is JSONL tutor advisory records with decisions and reasons.

## Deterministic Behavior

Expected behavior:

- Struggling learner emits `RECOMMEND_HINT_AND_LOWER_DIFFICULTY`.
- Strong learner emits `RECOMMEND_HARDER_EXERCISE`.
- Output remains advisory and does not directly modify an LMS record.

## Safety Ceiling

Mode is `ADVISORY`. The demo recommends tutoring actions but does not autonomously change learner records or grades.

## Verification Evidence

Default run evidence from `summary.json`:

- Status: `PASS`
- Ticks: `100`
- Output rows: `100`
- Assertions passed: local mode, JSONL output exists, aggregator called, struggling learner hint/lower difficulty, strong learner harder exercise.

## Real-Bridge Extension

A real LTI/xAPI bridge can replace the synthetic input. Preserve pseudonymization, advisory output, and educator-visible explanations for any real learner-facing use.
