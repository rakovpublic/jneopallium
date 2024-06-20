package com.rakovpublic.jneuropallium.worker.net.storages.filesystem;

public interface IStorageItem {
    boolean isDirectory();

    boolean exists();

    String getName();

    String getPath();

    void setPath(String path);


}
