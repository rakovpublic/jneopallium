package com.rakovpublic.jneuropallium.worker.net.signals;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public interface IInputSignal<T> extends ISignal<T> {
    String getInputName();
}
