package com.rakovpublic.jneuropallium.worker.model;

import com.rakovpublic.jneuropallium.worker.net.storages.ISplitInput;

public class SplitInputResponse {
    private String className;
    private ISplitInput splitInput;

    public SplitInputResponse(ISplitInput splitInput) {
        this.splitInput = splitInput;
        this.className = splitInput.getClass().getCanonicalName();
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public ISplitInput getSplitInput() {
        return splitInput;
    }

    public void setSplitInput(ISplitInput splitInput) {
        this.splitInput = splitInput;
    }
}
