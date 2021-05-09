package com.rakovpublic.jneuropallium.master.services.impl;

import com.rakovpublic.jneuropallium.worker.net.storages.IInitInput;

public class InputStatusMeta {
    private Boolean status;
    private Boolean mandatory;

    public InputStatusMeta(Boolean status, Boolean mandatory) {
        this.status = status;
        this.mandatory = mandatory;
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

}
