package net.neuron;
/**
* This class represents rule for neuron validation
* **/
public interface IRule {
    /**
    * @return true if neuron valid
    * */
    Boolean validate(INeuron neuron);

    /**
    * @return description
    * **/
    String getDescription();
}
