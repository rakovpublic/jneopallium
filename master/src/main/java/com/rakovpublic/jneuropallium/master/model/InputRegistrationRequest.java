package com.rakovpublic.jneuropallium.master.model;

public class InputRegistrationRequest {
    private String iInputSourceJson;
    private String iInputSourceClass;
    private Boolean isMandatory;
    private String initStrategy;
    private String initStrategyClass;
    private Integer amountOfRunsToUpdate;

    public String getiInputSourceJson() {
        return iInputSourceJson;
    }

    public void setiInputSourceJson(String iInputSourceJson) {
        this.iInputSourceJson = iInputSourceJson;
    }

    public String getiInputSourceClass() {
        return iInputSourceClass;
    }

    public void setiInputSourceClass(String iInputSourceClass) {
        this.iInputSourceClass = iInputSourceClass;
    }

    public Boolean getMandatory() {
        return isMandatory;
    }

    public void setMandatory(Boolean mandatory) {
        isMandatory = mandatory;
    }

    public String getInitStrategy() {
        return initStrategy;
    }

    public void setInitStrategy(String initStrategy) {
        this.initStrategy = initStrategy;
    }

    public String getInitStrategyClass() {
        return initStrategyClass;
    }

    public void setInitStrategyClass(String initStrategyClass) {
        this.initStrategyClass = initStrategyClass;
    }

    public Integer getAmountOfRunsToUpdate() {
        return amountOfRunsToUpdate;
    }

    public void setAmountOfRunsToUpdate(Integer amountOfRunsToUpdate) {
        this.amountOfRunsToUpdate = amountOfRunsToUpdate;
    }
}
