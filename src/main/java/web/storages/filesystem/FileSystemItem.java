package web.storages.filesystem;

import java.util.List;

public class FileSystemItem implements IFileSystemItem {
    private Boolean isDirectory;
    private Boolean exists;
    private String name;
    private String path;

    private FileSystemItem(Boolean isDirectory, Boolean exists, String name, String path) {
        this.isDirectory = isDirectory;
        this.exists = exists;
        this.name = name;
        this.path = path;
    }

    static FileSystemItem getFileSystemItem(Boolean isDirectory, Boolean exists, String name, String path){
        return new FileSystemItem( isDirectory,  exists,  name,  path);
    }

     void setExists(Boolean exists) {
        this.exists = exists;
    }

    @Override
    public boolean isDirectory() {
        return isDirectory;
    }

    @Override
    public boolean exists() {
        return exists;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPath() {
        return path;
    }

}
