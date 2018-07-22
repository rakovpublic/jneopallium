package web.storages.filesystem;

import java.util.List;

public interface IFileSystemItem {
    boolean isDirectory();
   boolean exists();
   String getName();
   String getPath();
}
