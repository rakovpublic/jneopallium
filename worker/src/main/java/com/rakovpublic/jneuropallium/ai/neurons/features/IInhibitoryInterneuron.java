package com.rakovpublic.jneuropallium.ai.neurons.features;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;

public interface IInhibitoryInterneuron extends INeuron {
    public String getLayerId() ;
    public void setLayerId(String layerId) ;

    public double getInhibitionStrength() ;
    public void setInhibitionStrength(double inhibitionStrength) ;
}

