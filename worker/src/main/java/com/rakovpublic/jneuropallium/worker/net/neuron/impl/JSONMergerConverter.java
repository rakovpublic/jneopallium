package com.rakovpublic.jneuropallium.worker.net.neuron.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalMerger;

import java.io.IOException;

public class JSONMergerConverter extends StdDeserializer<ISignalMerger> {

    protected JSONMergerConverter(Class<?> vc) {
        super(vc);
    }

    public JSONMergerConverter(JavaType valueType) {
        super(valueType);
    }

    public JSONMergerConverter(StdDeserializer<?> src) {
        super(src);
    }

    public JSONMergerConverter() {
        super(ISignalMerger.class);

    }

    @Override
    public ISignalMerger deserialize(com.fasterxml.jackson.core.JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        String s = jsonParser.getText();
        JsonElement jelement = new JsonParser().parse(s);
        JsonObject jobject = jelement.getAsJsonObject();
        String cl = jobject.getAsJsonPrimitive("signalMergerClass").getAsString();
        ObjectMapper mapper = new ObjectMapper();
        try {
            return (ISignalMerger) mapper.readValue(s, Class.forName(cl));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        //TODO: add logger
        return null;
    }
}
