package net.neuron;

public interface IRule {
    Boolean validate(INeuron neuron);
    String getDescription();
}
