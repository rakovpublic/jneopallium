package com.rakovpublic.jneuropallium.worker.net.signals;

public interface IInputSignal<T> extends ISignal<T> {
    String getInputName();
}
