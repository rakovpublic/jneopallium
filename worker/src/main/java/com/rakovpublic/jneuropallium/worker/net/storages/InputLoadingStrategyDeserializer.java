package com.rakovpublic.jneuropallium.worker.net.storages;

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

public class InputLoadingStrategyDeserializer extends StdDeserializer<IInputLoadingStrategy> {
    private final static Logger logger = LogManager.getLogger(InputInitStrategyDeserializer.class);

    public InputLoadingStrategyDeserializer() {
        this(null);
    }

    @Override
    public IInputLoadingStrategy deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonElement jelement = new com.google.gson.JsonParser().parse(jsonParser.readValueAsTree().toString());
        JsonObject jobject = jelement.getAsJsonObject();
        try {
            return (IInputLoadingStrategy) mapper.readValue(jobject.getAsJsonObject("iInputLoadingStrategy").toString(), Class.forName(jobject.getAsJsonPrimitive("clazz").getAsString()));
        } catch (ClassNotFoundException e) {
            logger.error("Cannot find class " + jobject.getAsJsonPrimitive("clazz").getAsString(), e);
        }
        return null;
    }

    public InputLoadingStrategyDeserializer(Class<?> vc) {
        super(vc);
    }

}
