package sample;

import net.storages.filesystem.IFileSystemItem;

import java.io.File;

public class LocalFile implements IFileSystemItem {
    private File file;

     LocalFile(File file) {
        this.file = file;
    }

    @Override
    public boolean isDirectory() {
        return file.isDirectory();

    }

    @Override
    public boolean exists() {
        return file.exists();
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public String getPath() {
        return file.getAbsolutePath();
    }
}
