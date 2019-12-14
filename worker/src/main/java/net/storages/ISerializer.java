package net.storages;

import java.io.Serializable;

public interface ISerializer<I, R> extends Serializable {
    R serialize(I input);

    I deserialize(R input);

    Class<I> getDeserializedClass();
    Class<R> getSerializedClass();

}
