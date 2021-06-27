package com.rakovpublic.jneuropallium.worker.net.layers;

import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;

public interface IResult<K extends IResultSignal> {
    K getResult();
    Long getNeuronId();
}
