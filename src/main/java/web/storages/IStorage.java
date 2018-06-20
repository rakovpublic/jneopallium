package web.storages;

public interface IStorage {
    <M extends IStorageMeta,R> M store(R input);
    <M extends IStorageMeta,R>R load(M storageMeta);
    <K extends IStorageMeta,R>void addSerializer(ISerializer<R,K> serializer);

}
