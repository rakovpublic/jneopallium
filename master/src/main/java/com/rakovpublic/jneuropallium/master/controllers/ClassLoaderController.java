package com.rakovpublic.jneuropallium.master.controllers;

import com.rakovpublic.jneuropallium.master.services.StorageService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/classloader")
public class ClassLoaderController {

    //TODO: add implementation for service
    private StorageService storageService;

    @PostMapping("/upload")
    public ResponseEntity<?> persistAndLoadClasses(@RequestParam("file") MultipartFile file) {
        try {
            String url = storageService.store(file);
            JarFile jarFile = new JarFile(url);
            Enumeration<JarEntry> e = jarFile.entries();
            URL[] urls = {new URL("jar:file:" + url + "!/")};
            URLClassLoader cl = URLClassLoader.newInstance(urls);
            while (e.hasMoreElements()) {
                JarEntry je = e.nextElement();
                if (je.isDirectory() || !je.getName().endsWith(".class")) {
                    continue;
                }
                // -6 because of .class
                String className = je.getName().substring(0, je.getName().length() - 6);
                className = className.replace('/', '.');
                Class c = cl.loadClass(className);

            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/jar")
    public ResponseEntity<?> getJar(@RequestParam String path) {
        InputStreamResource resource= null;
        try {
            resource = storageService.loadAsResource(path);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e);
        }
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"").body(resource);
    }

    @GetMapping("/jars")
    public ResponseEntity<?> getNamesJarsToLoad() {
        List<String> result = new ArrayList<>();
        try {
            result.addAll(storageService.loadAll().map(p -> p.toString()).collect(Collectors.toList()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e);
        }
        return ResponseEntity.ok(result);
    }
}
