package synchronizer.utils;

public interface ISerializerHelper {

    <K extends Object> String serialize(K object);
}
