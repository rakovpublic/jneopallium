package com.rakovpublic.jneuropallium.worker.util;

import java.net.URL;
import java.net.URLClassLoader;

public class JarClassLoaderService extends URLClassLoader {
    private URLClassLoader urlClassLoader;
    private Boolean initiated;
    public JarClassLoaderService(URL[] path){
        super(path);
    }

    public Boolean containsClass(String name){
        try {
            this.findClass(name);
            return true;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            //TODO: add logger
            return false;
        }
    }
}
