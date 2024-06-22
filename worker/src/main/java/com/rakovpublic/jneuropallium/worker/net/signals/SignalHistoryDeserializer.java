/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.inmemory.InMemorySignalHistoryStorage;

import java.io.IOException;

public class SignalHistoryDeserializer  extends StdDeserializer<ISignalHistoryStorage> {
    protected SignalHistoryDeserializer(Class<?> vc) {
        super(vc);
    }

    public SignalHistoryDeserializer() {
        super(ISignalHistoryStorage.class);
    }

    @Override
    public ISignalHistoryStorage deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        return new InMemorySignalHistoryStorage();
    }
}
