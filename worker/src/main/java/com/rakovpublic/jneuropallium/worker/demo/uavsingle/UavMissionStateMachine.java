package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UavMissionStateMachine {
    private static final Map<UavMissionState, Set<UavMissionState>> ALLOWED = new EnumMap<>(UavMissionState.class);

    static {
        allow(UavMissionState.INITIALIZING, UavMissionState.PREFLIGHT_CHECK);
        allow(UavMissionState.PREFLIGHT_CHECK, UavMissionState.TAKEOFF_REQUESTED);
        allow(UavMissionState.TAKEOFF_REQUESTED, UavMissionState.SEARCHING);
        allow(UavMissionState.SEARCHING, UavMissionState.TARGET_CANDIDATE_FOUND, UavMissionState.RETURNING_HOME);
        allow(UavMissionState.TARGET_CANDIDATE_FOUND, UavMissionState.TARGET_EVALUATION);
        allow(UavMissionState.TARGET_EVALUATION, UavMissionState.TARGET_SELECTED, UavMissionState.SEARCHING);
        allow(UavMissionState.TARGET_SELECTED, UavMissionState.APPROACHING_SAFE_OBSERVATION_POINT,
                UavMissionState.HOLDING_FOR_CONFIRMATION);
        allow(UavMissionState.HOLDING_FOR_CONFIRMATION, UavMissionState.CONFIRMED, UavMissionState.DENIED,
                UavMissionState.CONFIRMATION_TIMEOUT);
        allow(UavMissionState.CONFIRMED, UavMissionState.APPROACHING_SAFE_OBSERVATION_POINT);
        allow(UavMissionState.DENIED, UavMissionState.SEARCHING, UavMissionState.RETURNING_HOME);
        allow(UavMissionState.CONFIRMATION_TIMEOUT, UavMissionState.SEARCHING, UavMissionState.RETURNING_HOME);
        allow(UavMissionState.APPROACHING_SAFE_OBSERVATION_POINT, UavMissionState.OBSERVING);
        allow(UavMissionState.OBSERVING, UavMissionState.PHOTOGRAPHING, UavMissionState.RETURNING_HOME);
        allow(UavMissionState.PHOTOGRAPHING, UavMissionState.VERIFYING_PHOTO);
        allow(UavMissionState.VERIFYING_PHOTO, UavMissionState.SEARCHING, UavMissionState.RETURNING_HOME);
        allow(UavMissionState.SAFETY_HOLD, UavMissionState.RETURNING_HOME, UavMissionState.ABORTED);
        allow(UavMissionState.RETURNING_HOME, UavMissionState.LANDED);
        allow(UavMissionState.LANDED, UavMissionState.COMPLETED);
        for (UavMissionState state : UavMissionState.values()) {
            if (!EnumSet.of(UavMissionState.ABORTED, UavMissionState.LANDED, UavMissionState.COMPLETED).contains(state)) {
                allow(state, UavMissionState.SAFETY_HOLD, UavMissionState.RETURNING_HOME, UavMissionState.ABORTED);
            }
        }
    }

    private UavMissionState state = UavMissionState.INITIALIZING;

    public UavMissionState state() {
        return state;
    }

    public UavMissionState transition(UavMissionState next) {
        if (!canTransition(state, next)) {
            throw new IllegalStateException("Illegal UAV mission transition " + state + " -> " + next);
        }
        state = next;
        return state;
    }

    public static boolean canTransition(UavMissionState from, UavMissionState to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    private static void allow(UavMissionState from, UavMissionState... to) {
        ALLOWED.computeIfAbsent(from, ignored -> EnumSet.noneOf(UavMissionState.class)).addAll(List.of(to));
    }
}
