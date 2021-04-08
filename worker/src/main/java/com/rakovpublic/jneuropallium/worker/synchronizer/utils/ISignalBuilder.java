package com.rakovpublic.jneuropallium.worker.synchronizer.utils;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/***
 * Created by Rakovskyi Dmytro on 29.10.2017.
 */
public interface ISignalBuilder {
    ISignal buildSignal(String json);
}
