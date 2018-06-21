package exceptions;

public class SerializerForClassIsNotRegistered extends NullPointerException {
    public SerializerForClassIsNotRegistered(String s) {
        super(s);
    }
}
