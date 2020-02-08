package sample;

import net.storages.ISerializer;

public class SimpleSignalSerializer implements ISerializer<SimpleSignal,String> {
    @Override
    public String serialize(SimpleSignal input) {
        return null;
    }

    @Override
    public SimpleSignal deserialize(String input) {
        return null;
    }

    @Override
    public Class<SimpleSignal> getDeserializedClass() {
        return null;
    }

    @Override
    public Class<String> getSerializedClass() {
        return null;
    }
}
