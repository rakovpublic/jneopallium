package com.rakovpublic.jneuropallium.master.services.impl;

import com.rakovpublic.jneuropallium.worker.net.storages.IInitInput;

public class InputStatusMeta {
    private Boolean status;
    private Boolean mandatory;
    private IInitInput initInput;

    public InputStatusMeta(Boolean status, Boolean mandatory, IInitInput initInput) {
        this.status = status;
        this.mandatory = mandatory;
        this.initInput = initInput;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public Boolean getMandatory() {
        return mandatory;
    }

    public void setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
    }

    public IInitInput getInitInput() {
        return initInput;
    }

    public void setInitInput(IInitInput initInput) {
        this.initInput = initInput;
    }
}
