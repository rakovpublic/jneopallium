package net.neuron.impl;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

import java.io.IOException;

public class MapDeserializer extends KeyDeserializer {
    @Override
    public Object deserializeKey(String s, DeserializationContext deserializationContext) throws IOException {
        try {
            return Class.forName(s);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
