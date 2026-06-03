package com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime;

import com.rakovpublic.jneuropallium.worker.net.storages.filesystem.IStorageItem;

import java.nio.file.Files;
import java.nio.file.Path;

public class DemoStorageItem implements IStorageItem {
    private String path;

    public DemoStorageItem() {
    }

    public DemoStorageItem(Path path) {
        this.path = path.toString();
    }

    @Override
    public boolean isDirectory() {
        return Files.isDirectory(Path.of(path));
    }

    @Override
    public boolean exists() {
        return Files.exists(Path.of(path));
    }

    @Override
    public String getName() {
        return Path.of(path).getFileName().toString();
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public void setPath(String path) {
        this.path = path;
    }

    public Path asPath() {
        return Path.of(path);
    }
}
