package com.rakovpublic.jneuropallium.worker.net.storages;

import java.io.Serializable;


/**
 * Interface for object seriliazation/ deseriliazation
 */
public interface ISerializer<I, R> extends Serializable {
    /**
     * @param input -object to serialize
     * @return serialization result
     */
    R serialize(I input);

    /**
     * @param input -object to deserialize
     * @return deserialization result
     */
    I deserialize(R input);

    /**
     * @return deserialized object class
     */
    Class<I> getDeserializedClass();

    /**
     * @return serialized object class
     */
    Class<R> getSerializedClass();

}
