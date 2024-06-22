/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class SignalDeserializer extends StdDeserializer<SignalWrapper> {
    private static final Logger logger = LogManager.getLogger(SignalDeserializer.class);

    public SignalDeserializer() {
        super(SignalWrapper.class);
    }

    protected SignalDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public SignalWrapper deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonElement jelement = new com.google.gson.JsonParser().parse(jsonParser.readValueAsTree().toString());
        JsonObject jobject = jelement.getAsJsonObject();
        try {
            return new SignalWrapper((IInputSignal) mapper.readValue(jobject.toString(), Class.forName(jobject.getAsJsonPrimitive("name").getAsString())));
        } catch (ClassNotFoundException e) {
            logger.error("cannot parse init input from json", e);
        }
        return null;
    }
}
