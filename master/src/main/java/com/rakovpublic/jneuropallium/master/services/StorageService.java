package com.rakovpublic.jneuropallium.master.services;

import org.springframework.core.io.InputStreamResource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.stream.Stream;

public interface StorageService {
    void init(String folder);

    String store(MultipartFile file);

    Stream<Path> loadAll();

    InputStreamResource loadAsResource(String filename);

}
