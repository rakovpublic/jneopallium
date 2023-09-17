/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.master.services.impl;

import com.rakovpublic.jneuropallium.master.configs.PropertyHolder;
import com.rakovpublic.jneuropallium.master.services.StorageService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

@Service
public class FileStorageService implements StorageService {
    private String folder;
    private static final Logger logger = LogManager.getLogger(FileStorageService.class);

    public FileStorageService() {
       folder = PropertyHolder.getPropertyHolder().getProp("storage.file.path");
    }

    @Override
    public void init(String folder) {
        this.folder =folder;
    }

    @Override
    public String store(MultipartFile file) {
        String path =null;
        try {
            path = folder+"/"+file.getOriginalFilename();
            Files.copy(file.getInputStream(),Path.of(path), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error("Cannot create file " + path,e);
        }
        return path;
    }

    @Override
    public Stream<Path> loadAll() {
        try {
            return Files.list(Paths.get(folder));
        } catch (IOException e) {
            logger.error("Empty storage foldr " + folder,e);
            return Stream.<Path>builder().build();
        }
    }


    @Override
    public InputStreamResource loadAsResource(String filename) {
        InputStreamResource resource = null;
        try {
            resource = new InputStreamResource(new FileInputStream(folder+"/"+filename));
        } catch (FileNotFoundException e) {
            logger.error("Cannot found file " + filename,e);
        }
        return resource;
    }
}
