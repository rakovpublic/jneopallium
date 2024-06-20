/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.ioutils;

import com.rakovpublic.jneuropallium.worker.net.storages.filesystem.IStorage;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

public class LocalFileStorage implements IStorage<LocalFileItem> {

    @Override
    public LocalFileItem getItem(String path) {
        return new LocalFileItem(path);
    }

    @Override
    public String read(LocalFileItem path) {

        File file = new File(path.getPath());
        StringBuilder builder = new StringBuilder();
        // Declaring a string variable
        String st;

        BufferedReader br
                = null;
        try {
            br = new BufferedReader(new FileReader(file));
            while ((st = br.readLine()) != null){
                builder.append(st);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }



        return builder.toString();
    }

    @Override
    public boolean createFolder(LocalFileItem path) {

        return true;
    }

    @Override
    public boolean writeCreate(String content, LocalFileItem path) {
        return false;
    }

    @Override
    public boolean rewrite(String content, LocalFileItem path) {
        File myFoo = new File(path.getPath());
        FileWriter fooWriter = null;
        try {
            fooWriter = new FileWriter(myFoo, false);
            fooWriter.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                fooWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    @Override
    public boolean writeUpdate(String content, LocalFileItem path, int startFrom) {
        return false;
    }

    @Override
    public boolean writeUpdateObject(String content, LocalFileItem path, String idFieldName) {
        return false;
    }

    @Override
    public boolean writeUpdateObjects(String[] content, LocalFileItem path, String idFieldName) {
        return false;
    }

    @Override
    public boolean deleteObject(String[] content, LocalFileItem path, String idFieldName) {
        return false;
    }

    @Override
    public boolean writeUpdateToEnd(String content, LocalFileItem path) {
        return false;
    }

    @Override
    public boolean delete(String path) {
        return false;
    }

    @Override
    public boolean delete(LocalFileItem path) {
        return false;
    }

    @Override
    public boolean deleteContent(LocalFileItem path, int startFrom, int amount) {
        return false;
    }

    @Override
    public void copy(LocalFileItem source, LocalFileItem destination) {

    }

    @Override
    public List<LocalFileItem> listFiles(LocalFileItem file) {

        List<LocalFileItem>  result = new LinkedList<>();
        if(file.isDirectory()){
            File myFoo = new File(file.getPath());
            for(File f:myFoo.listFiles()){
                result.add(new LocalFileItem(f.getAbsolutePath()));
            }
        }
        return result;
    }

    @Override
    public String getFolderSeparator() {
        return null;
    }

    @Override
    public void deleteFilesFromDirectory(LocalFileItem path) {

    }
}
