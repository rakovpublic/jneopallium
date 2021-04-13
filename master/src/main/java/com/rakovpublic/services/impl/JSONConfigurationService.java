package com.rakovpublic.services.impl;

import com.rakovpublic.jneuropallium.worker.util.IConfigurationService;
import com.rakovpublic.jneuropallium.worker.util.JarClassLoaderService;
import com.rakovpublic.jneuropallium.worker.util.NoSuchClassInJarException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class JSONConfigurationService implements IConfigurationService {
    @Override
    public List<String> parseClassNames(String json) {
        return null;
    }

    @Override
    public Boolean loadClassesFromJar(List<String> path, List<String> classNames) {
        URL [] urls = new URL[path.size()];
        int i=0;
        StringBuilder pathNames = new StringBuilder();
        for(String p:path){
            try {
                urls[i] = new URL(p);
                i++;
                pathNames.append(p);
                pathNames.append(",");
            } catch (MalformedURLException e) {
                e.printStackTrace();
                //TODO: add logger
            }
        }
        JarClassLoaderService classLoaderService = new JarClassLoaderService(urls);
        for(String name : classNames){
            if(!classLoaderService.containsClass(name)){
                //TODO: add logger
                throw new NoSuchClassInJarException("class name :"+name +" jar(s): "+pathNames);
            }
        }
        return true;
    }
}
