package web.storages;

public interface IStorage<M extends IStorageMeta,R> {
    M store(R input);
    R load(M storageMeta);
    <K>void addSerializer(ISerializer<R,K> serializer);

}
