package web.neuron;

public interface IRule {
    Boolean validate(INeuron neuron);
    String getDescription();
}
