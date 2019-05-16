package net.signals;

import java.io.Serializable;

/***
 * Created by Rakovskyi Dmytro on 27.10.2017.
 */
public interface ISignal<T> extends Serializable {
    /**
     * @return value of signal
     * */
    T getValue();

    /**
    * @return Signal class
     * */
    Class<? extends ISignal<T>> getCurrentClass();

    /**
     * @return value class
     * */
    Class<T> getParamClass();

    String toJSON();
    /**
     * @return  description
     * */
     String getDescription();

     /**
      * @return true if processor for parent signals can be used for processing this signal
      * */
     boolean canUseProcessorForParent();

}