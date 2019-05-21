package net.storages;

public interface ISerializer<I, R> {
    R serialize(I input);

    I deserialize(R input);

    Class<I> getDeserializedClass();
    Class<R> getSerializedClass();

}
