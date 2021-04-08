package com.rakovpublic.jneuropallium.worker.synchronizer.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

//TODO: refactor this class to interface and implementations for different serialization formats
public class JSONHelper implements IDeserializationHelper {
    public DeserializationHelperResult getNextObject(String json, Integer index) {

        DeserializationHelperResult res = null;
        String result = null;
        index = json.indexOf(index, '{');
        if (index == -1 || index >= json.length()) {
            return null;
        }
        int open = 0;
        int startIndex = index;
        for (int i = index; i < json.length(); i++) {
            if (json.charAt(i) == '{') {
                open += 1;
            } else if (json.charAt(i) == '}') {
                open -= 1;
            }
            if (open == 0) {

                result = json.substring(startIndex, i) + "}";
                res = new DeserializationHelperResult(result, i, extractField(result, "className"));
            }
        }
        return res;
    }

    public String extractField(String json, String fieldName) {

        /*int index = json.indexOf(fieldName);
        index = json.indexOf(':', index);
        int endIndex = json.indexOf('"', json.indexOf('"', index) + 1);
        if (index + 2 < endIndex - 1) {
            return json.substring(index + 2, endIndex);
        }
        return "";*/
        JsonElement jelement = new JsonParser().parse(json);
        JsonObject jobject = jelement.getAsJsonObject();
        return jobject.getAsJsonPrimitive(fieldName).getAsString();

    }


}
