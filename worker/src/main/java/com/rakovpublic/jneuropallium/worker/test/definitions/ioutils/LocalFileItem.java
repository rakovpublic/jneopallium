/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.ioutils;

import com.rakovpublic.jneuropallium.worker.net.storages.filesystem.IStorageItem;

import java.io.File;

public class LocalFileItem implements IStorageItem {
    private File file;

    public LocalFileItem(String path) {
        file = new File(path);
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

    @Override
    public void setPath(String path) {
        file =  new File(path);
    }
}
