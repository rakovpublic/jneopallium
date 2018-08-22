package synchronizer.utils;
//TODO: change to using this interface
public interface IDeserializationHelper {
      String extractField(String json, String fieldName);
     DeserializationHelperResult getNextObject(String json, Integer index);
}
