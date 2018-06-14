package web.signals;

import java.io.Serializable;

/**
 * Created by Rakovskyi Dmytro on 27.10.2017.
 */
public interface ISignal<T> extends Serializable {
    T getValue();
    Class<? extends T> getParamClass();
    Class<? extends ISignal<T>> getCurrentClass();
    String toJSON();

}
