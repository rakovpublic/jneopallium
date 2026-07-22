package com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime;

import com.rakovpublic.jneuropallium.worker.net.storages.filesystem.IStorage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;

public class DemoFileStorage implements IStorage<DemoStorageItem> {
    private String rootPath;

    public DemoFileStorage() {
        this.rootPath = ".";
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath == null || rootPath.isBlank() ? "." : rootPath;
    }

    private Path getNormalizedRootPath() {
        return Path.of(rootPath).toAbsolutePath().normalize();
    }

    private Path resolveAndValidatePath(String path) {
        Path root = getNormalizedRootPath();
        Path requested = Path.of(path);
        Path resolved = requested.isAbsolute() ? requested : root.resolve(requested);
        Path normalizedResolved = resolved.toAbsolutePath().normalize();
        if (!normalizedResolved.equals(root) && !normalizedResolved.startsWith(root)) {
            throw new IllegalArgumentException("Invalid path outside of storage root: " + path);
        }
        return normalizedResolved;
    }

    @Override
    public DemoStorageItem getItem(String path) {
        return new DemoStorageItem(resolveAndValidatePath(path));
    }

    @Override
    public String read(DemoStorageItem path) {
        try {
            Path safePath = resolveAndValidatePath(path.getPath());
            return Files.readString(safePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + path.getPath(), e);
        }
    }

    @Override
    public boolean createFolder(DemoStorageItem path) {
        try {
            Files.createDirectories(path.asPath());
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create folder " + path.getPath(), e);
        }
    }

    @Override
    public boolean writeCreate(String content, DemoStorageItem path) {
        try {
            Path target = path.asPath();
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(target, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create file " + path.getPath(), e);
        }
    }

    @Override
    public boolean rewrite(String content, DemoStorageItem path) {
        try {
            Path target = path.asPath();
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(target, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot rewrite file " + path.getPath(), e);
        }
    }

    @Override
    public boolean writeUpdate(String content, DemoStorageItem path, int startFrom) {
        String existing = path.exists() ? read(path) : "";
        String prefix = existing.substring(0, Math.min(startFrom, existing.length()));
        return rewrite(prefix + content, path);
    }

    @Override
    public boolean writeUpdateObject(String content, DemoStorageItem path, String idFieldName) {
        return writeUpdateToEnd(content, path);
    }

    @Override
    public boolean writeUpdateObjects(String[] content, DemoStorageItem path, String idFieldName) {
        return writeUpdateToEnd(String.join(System.lineSeparator(), content), path);
    }

    @Override
    public boolean deleteObject(String[] content, DemoStorageItem path, String idFieldName) {
        return true;
    }

    @Override
    public boolean writeUpdateToEnd(String content, DemoStorageItem path) {
        try {
            Path target = path.asPath();
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(target, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot append file " + path.getPath(), e);
        }
    }

    @Override
    public boolean delete(String path) {
        return delete(getItem(path));
    }

    @Override
    public boolean delete(DemoStorageItem path) {
        try {
            Files.deleteIfExists(path.asPath());
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot delete " + path.getPath(), e);
        }
    }

    @Override
    public boolean deleteContent(DemoStorageItem path, int startFrom, int amount) {
        String existing = read(path);
        int start = Math.min(startFrom, existing.length());
        int end = Math.min(start + amount, existing.length());
        return rewrite(existing.substring(0, start) + existing.substring(end), path);
    }

    @Override
    public void copy(DemoStorageItem source, DemoStorageItem destination) {
        try {
            Path parent = destination.asPath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.copy(source.asPath(), destination.asPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot copy " + source.getPath() + " to " + destination.getPath(), e);
        }
    }

    @Override
    public List<DemoStorageItem> listFiles(DemoStorageItem file) {
        try {
            return new java.util.ArrayList<>(Files.list(file.asPath())
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .map(DemoStorageItem::new)
                    .toList());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot list " + file.getPath(), e);
        }
    }

    @Override
    public String getFolderSeparator() {
        return java.io.File.separator;
    }

    @Override
    public void deleteFilesFromDirectory(DemoStorageItem path) {
        if (!path.exists()) {
            return;
        }
        try {
            Files.list(path.asPath()).forEach(item -> {
                try {
                    Files.deleteIfExists(item);
                } catch (IOException e) {
                    throw new IllegalStateException("Cannot delete " + item, e);
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("Cannot clear " + path.getPath(), e);
        }
    }
}
