package com.rakovpublic.jneuropallium.worker.net.signals;

/***
 * Created by Rakovskyi Dmytro on 29.10.2017.
 */
public interface IResultSignal<K> extends ISignal {
    K getResultObject();

    Class<K> getResultObjectClass();

}
