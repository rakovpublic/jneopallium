package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.sim;

import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.OwnerTask;

import java.util.LinkedHashMap;
import java.util.Map;

public class HumanOwnerCommand {
    public String ownerId;
    public String commandText;
    public OwnerTask task;
    public boolean acceptedAsInput;

    public HumanOwnerCommand() {
    }

    public HumanOwnerCommand(OwnerTask task) {
        this.task = task;
        if (task != null) {
            this.ownerId = task.ownerId;
            this.commandText = task.goal;
            this.acceptedAsInput = true;
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("ownerId", ownerId);
        row.put("commandText", commandText);
        row.put("taskId", task == null ? null : task.taskId);
        row.put("acceptedAsInput", acceptedAsInput);
        return row;
    }
}
