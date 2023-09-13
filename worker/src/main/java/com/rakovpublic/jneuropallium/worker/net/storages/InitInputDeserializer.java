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

//TODO: add annotation fro json parser
public class InitInputDeserializer extends StdDeserializer<IInitInput> {
    private static final Logger logger = LogManager.getLogger(InitInputDeserializer.class);

    public InitInputDeserializer(Class<?> vc) {
        super(vc);
    }

    public InitInputDeserializer() {
        this(IInitInput.class);
    }

    @Override
    public IInitInput deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonElement jelement = new com.google.gson.JsonParser().parse(jsonParser.readValueAsTree().toString());
        JsonObject jobject = jelement.getAsJsonObject();
        try {
            return (IInitInput) mapper.readValue(jobject.getAsJsonObject("initInput").toString(), Class.forName(jobject.getAsJsonPrimitive("clazz").getAsString()));
        } catch (ClassNotFoundException e) {
            logger.error("cannot parse init input from json", e);
        }
        return null;
    }
}
