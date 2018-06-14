package web.study;

import web.neuron.INeuron;

import java.io.Serializable;

/**
 * Created by Rakovskyi Dmytro on 27.10.2017.
 */
public interface IStudyingRequest<N extends INeuron> extends Serializable {
    Class<? extends N> getTargetClass();
    String toJSON();
}
