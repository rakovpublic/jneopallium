package web.storages;

public interface ISerializer<I,R extends IStorageMeta> {
    R serialize(I input);
    I deserialize(R input);

}
