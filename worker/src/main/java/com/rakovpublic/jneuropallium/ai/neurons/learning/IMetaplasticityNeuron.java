package com.rakovpublic.jneuropallium.ai.neurons.learning;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import java.util.HashMap;
import java.util.Map;

public interface IMetaplasticityNeuron extends IModulatableNeuron {
    void reduceSlasticityRate(String regionId);
    void increaseSlasticityRate(String regionId);
    Map<String, Double> getActivityHistory();
    void setActivityHistory(Map<String, Double> activityHistory);
    Map<String, Double> getPlasticityRates();
    void setPlasticityRates(Map<String, Double> plasticityRates);
    double getOveractiveThreshold();
    void setOveractiveThreshold(double overactiveThreshold);
    double getUnderactiveThreshold();
    void setUnderactiveThreshold(double underactiveThreshold);
    double getPlasticityStep();
    void setPlasticityStep(double plasticityStep);
}
