package services;

import model.IInputSource;

import java.util.List;

public interface IInputService {
    void register(String name, IInputSource iInputSource);
    void inputUpdated(String name);
    IInputSource getNext(String name);
    IInputSource getLatest(String name);
    List<IInputSource> getNextComplete();
    boolean hasNextComplete();
    List<IInputSource> getNextAny();
}
