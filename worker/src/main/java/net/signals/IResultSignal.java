package net.signals;

/**
 * Created by Rakovskyi Dmytro on 29.10.2017.
 */
public interface IResultSignal<T, K> extends ISignal<T> {
    K getResultObject();

    Class<K> getResultObjectClass();

}
