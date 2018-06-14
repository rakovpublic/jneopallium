package synchronizer.utils;

import web.signals.ISignal;

/**
 * Created by Rakovskyi Dmytro on 29.10.2017.
 */
public interface ISignalBuilder {
    ISignal buildSignal(String json);
}
