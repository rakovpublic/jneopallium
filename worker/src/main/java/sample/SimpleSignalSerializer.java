package sample;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.storages.ISerializer;

import java.io.IOException;

public class SimpleSignalSerializer implements ISerializer<SimpleSignal, String> {
    @Override
    public String serialize(SimpleSignal input) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            //TODO:add logging
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public SimpleSignal deserialize(String input) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(input, SimpleSignal.class);
        } catch (IOException e) {
            //TODO:add logging
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Class<SimpleSignal> getDeserializedClass() {
        return SimpleSignal.class;
    }

    @Override
    public Class<String> getSerializedClass() {
        return String.class;
    }
}
