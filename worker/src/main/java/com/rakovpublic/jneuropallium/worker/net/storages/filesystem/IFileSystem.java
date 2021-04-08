package com.rakovpublic.jneuropallium.worker.net.storages.filesystem;

import java.util.List;

public interface IFileSystem<S extends IFileSystemItem> {
    S getItem(String path);

    String read(S path);
    boolean createFolder(S path);

    boolean writeCreate(String content, S path);

    boolean rewrite(String content, S path);

    boolean writeUpdate(String content, S path, int startFrom);

    boolean writeUpdateObject(String content, S path, String idFieldName);

    boolean writeUpdateObjects(String[] content, S path, String idFieldName);

    boolean deleteObject(String[] content, S path, String idFieldName);

    boolean writeUpdateToEnd(String content, S path);

    boolean delete(String path);

    boolean delete(S path);

    boolean deleteContent(S path, int startFrom, int amount);

    void copy(S source, S destination);

    List<S> listFiles(S file);

    String getFolderSeparator();

    void deleteFilesFromDirectory(S path);

}
