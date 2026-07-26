package com.rakovpublic.jneuropallium.worker.net.neuron.impl;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class MapDeserializer extends KeyDeserializer {
    private static final Logger logger = LogManager.getLogger(MapDeserializer.class);

    @Override
    public Object deserializeKey(String s, DeserializationContext deserializationContext) throws IOException {
        try {
            return Class.forName(s);
        } catch (ClassNotFoundException e) {
            logger.error("Cannot find class for key: " + s, e);
            throw new IOException("Cannot find class for deserialization key: " + s, e);
        }
    }
}
