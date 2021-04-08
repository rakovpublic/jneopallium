package com.rakovpublic.jneuropallium.worker.synchronizer.utils;

public interface ISerializerHelper {

    <K extends Object> String serialize(K object);
}
