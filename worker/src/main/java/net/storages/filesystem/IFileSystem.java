package net.storages.filesystem;

import java.util.List;

public interface IFileSystem<S extends IFileSystemItem> {
    S getItem(String path);

    String read(S path);

    boolean writeCreate(String content, S path);

    boolean rewrite(String content, S path);

    boolean writeUpdate(String content, S path, int startFrom);

    boolean writeUpdateObject(Object content, S path);

    boolean writeUpdateObjects(Object[] content, S path);

    boolean writeUpdateToEnd(String content, S path);

    boolean delete(String path);

    boolean delete(S path);

    boolean deleteContent(S path, int startFrom, int amount);

    void copy(S source, S destination);

    List<S> listFiles(S file);

    String getFolderSeparator();

    void deleteFilesFromDirectory(S path);

}
