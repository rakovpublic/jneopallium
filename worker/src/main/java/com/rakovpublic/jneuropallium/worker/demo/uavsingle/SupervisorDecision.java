package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SupervisorDecision {
    public final boolean accepted;
    public final List<String> reasons;

    public SupervisorDecision(boolean accepted, List<String> reasons) {
        this.accepted = accepted;
        this.reasons = reasons == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(reasons));
    }

    public static SupervisorDecision accepted() {
        return new SupervisorDecision(true, List.of());
    }

    public static SupervisorDecision rejected(List<String> reasons) {
        return new SupervisorDecision(false, reasons);
    }
}

