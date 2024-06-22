package com.rakovpublic.jneuropallium.worker.net.neuron.impl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rakovpublic.jneuropallium.worker.exceptions.JSONParsingException;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class JSONProcessorConverter extends StdDeserializer<ISignalProcessor> {
    private static final Logger logger = LogManager.getLogger(JSONProcessorConverter.class);

    protected JSONProcessorConverter(Class<?> vc) {
        super(vc);
    }

    public JSONProcessorConverter(JavaType valueType) {
        super(valueType);
    }

    public JSONProcessorConverter(StdDeserializer<?> src) {
        super(src);
    }

    public JSONProcessorConverter() {
        super(ISignalProcessor.class);


    }

    @Override
    public ISignalProcessor deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        String s = jsonParser.getText();
        JsonElement jelement = new com.google.gson.JsonParser().parse(s);
        JsonObject jobject = jelement.getAsJsonObject();
        String cl = jobject.getAsJsonPrimitive("signalProcessorClass").getAsString();
        ObjectMapper mapper = new ObjectMapper();
        try {
            return (ISignalProcessor) mapper.readValue(s, Class.forName(cl));
        } catch (IOException e) {
            logger.error(e);
            throw new JSONParsingException(e.getMessage());
        } catch (ClassNotFoundException e) {
            logger.error(e);
            throw new JSONParsingException(e.getMessage());
        }

    }
}
