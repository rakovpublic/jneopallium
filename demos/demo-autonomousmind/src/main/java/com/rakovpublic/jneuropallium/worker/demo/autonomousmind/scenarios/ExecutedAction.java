package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.scenarios;

import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.ActionType;

public class ExecutedAction {
    public final ActionType action;
    public final HarmVerdict verdict;

    public ExecutedAction(ActionType action, HarmVerdict verdict) {
        this.action = action;
        this.verdict = verdict;
    }
}
