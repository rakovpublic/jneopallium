package synchronizer.utils;

//TODO: change to using this interface
public interface IDeserializationHelper {
    String extractField(String input, String fieldName);

    DeserializationHelperResult getNextObject(String input, Integer index);

}
