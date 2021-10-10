package com.rakovpublic.jneuropallium.worker.net.storages;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;

public class InitInputDeserializer extends StdDeserializer<IInitInput> {
    public InitInputDeserializer(Class<?> vc) {
        super(vc);
    }

    public InitInputDeserializer() {
        this(null);
    }

    @Override
    public IInitInput deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonElement jelement = new com.google.gson.JsonParser().parse(jsonParser.readValueAsTree().toString());
        JsonObject jobject = jelement.getAsJsonObject();
        try {
            return (IInitInput) mapper.readValue(jobject.getAsJsonObject("initInput").toString(), Class.forName(jobject.getAsJsonPrimitive("clazz").getAsString()));
        } catch (ClassNotFoundException e) {
            //TODO: add logger
            e.printStackTrace();
        }
        return null;
    }
}
