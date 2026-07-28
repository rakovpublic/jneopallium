/*
 * Copyright (c) 2026. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Serialises signal lists for the shared stores.
 * <p>
 * Each element is wrapped in an envelope that names the concrete class:
 * {@code [{"signalClass":"...","signal":{...}}, ...]}. Signals are polymorphic and not every
 * implementation fills the optional {@code currentClassName} property, so relying on the
 * payload to describe itself loses signals silently; the envelope always knows.
 */
public final class SignalJson {
    private static final Logger logger = LogManager.getLogger(SignalJson.class);
    /**
     * Signals expose derived, read-only properties - {@code currentSignalClass},
     * {@code paramClass}, {@code resultObjectClass} and friends - which are written out but have
     * no setter to read back. Ignoring unknown properties is what makes a signal survive a
     * round trip through the store.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private SignalJson() {
    }

    public static String write(List<ISignal> signals) {
        JsonArray array = new JsonArray();
        if (signals != null) {
            for (ISignal signal : signals) {
                if (signal == null) {
                    continue;
                }
                try {
                    JsonObject envelope = new JsonObject();
                    envelope.addProperty("signalClass", signal.getClass().getName());
                    envelope.add("signal", JsonParser.parseString(MAPPER.writeValueAsString(signal)));
                    array.add(envelope);
                } catch (JsonProcessingException e) {
                    logger.error("Cannot serialize signal of type " + signal.getClass().getName(), e);
                }
            }
        }
        return array.toString();
    }

    public static CopyOnWriteArrayList<ISignal> read(String json) {
        CopyOnWriteArrayList<ISignal> signals = new CopyOnWriteArrayList<>();
        if (json == null || json.isBlank()) {
            return signals;
        }
        JsonElement parsed = JsonParser.parseString(json);
        if (!parsed.isJsonArray()) {
            return signals;
        }
        for (JsonElement element : parsed.getAsJsonArray()) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject envelope = element.getAsJsonObject();
            JsonElement signalClass = envelope.get("signalClass");
            JsonElement payload = envelope.get("signal");
            if (signalClass == null || payload == null) {
                logger.error("Malformed signal envelope, skipping: " + envelope);
                continue;
            }
            try {
                signals.add((ISignal) MAPPER.readValue(payload.toString(), Class.forName(signalClass.getAsString())));
            } catch (JsonProcessingException e) {
                logger.error("Cannot parse signal " + payload, e);
            } catch (ClassNotFoundException e) {
                logger.error("Cannot find signal class " + signalClass.getAsString(), e);
            }
        }
        return signals;
    }
}
