package com.rakovpublic.jneuropallium.worker.synchronizer.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class JSONHelper implements IDeserializationHelper {

    public String extractField(String json, String fieldName) {
        JsonElement jelement = new JsonParser().parse(json);
        JsonObject jobject = jelement.getAsJsonObject();
        return jobject.getAsJsonPrimitive(fieldName).getAsString();

    }


}
