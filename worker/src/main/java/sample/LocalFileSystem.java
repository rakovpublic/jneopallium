package sample;

import net.storages.filesystem.IFileSystem;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class LocalFileSystem  implements IFileSystem<LocalFile> {

    @Override
    public LocalFile getItem(String path) {
        return new LocalFile(new File(path));
    }

    @Override
    public String read(LocalFile path) {
        if (path.exists()&&!path.isDirectory()){
            BufferedReader reader = null;
            String currentLine=null;
            StringBuilder builder = new StringBuilder();
            try {
                reader = new BufferedReader(new FileReader(path.getPath()));

            currentLine = reader.readLine();
            while (currentLine != null) {
                builder.append(currentLine);
                currentLine = reader.readLine();
            }

            } catch (FileNotFoundException e) {
                e.printStackTrace();

            } catch (IOException e) {
                e.printStackTrace();

            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return builder.toString();


        }
        return null;
    }

    @Override
    public boolean createFolder(LocalFile path) {
        File f=new File(path.getPath());
        if(!f.exists()){
           return f.mkdir();
        }
        return true;
    }

    @Override
    public boolean writeCreate(String content, LocalFile path) {
        List<String> strings= new ArrayList<>();
        strings.add(content);
        try {
            File f=new File(path.getPath());
            if(!f.exists()){
                f.createNewFile();
            }

            Files.write(f.toPath(), Collections.singleton(content));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean rewrite(String content, LocalFile path) {
        return writeCreate(content,path);
    }

    @Override
    public boolean writeUpdate(String content, LocalFile path, int startFrom) {
        return false;
    }

    @Override
    public boolean writeUpdateObject(String content, LocalFile path, String idFieldName) {
        return false;
    }

    @Override
    public boolean writeUpdateObjects(String[] content, LocalFile path, String idFieldName) {
        return false;
    }

    @Override
    public boolean deleteObject(String[] content, LocalFile path, String idFieldName) {
        return false;
    }


    @Override
    public boolean writeUpdateToEnd(String content, LocalFile path) {
        try {
            Files.write(new File(path.getPath()).toPath(), content.getBytes(), new StandardOpenOption[]{StandardOpenOption.APPEND});
            return true;
        }catch (IOException e) {
            //TODO: add logging;
            return false;
        }
    }

    @Override
    public boolean delete(String path) {
        return new File(path).delete();
    }

    @Override
    public boolean delete(LocalFile path) {
        return new File(path.getPath()).delete();
    }

    @Override
    public boolean deleteContent(LocalFile path, int startFrom, int amount) {
        return false;
    }

    @Override
    public void copy(LocalFile source, LocalFile destination) {
        File in= new File(source.getPath());
        File out= new File(destination.getPath());
        if(destination.isDirectory()){
            if(in.isDirectory()){
                File[] fList = in.listFiles();
                for(File ff:fList){
                    try {
                        Files.copy(ff.toPath(),out.toPath());
                    } catch (IOException e) {
                        //TODO:add logging
                        e.printStackTrace();
                    }
                }
            }else {
                try {
                    Files.copy(in.toPath(),out.toPath());
                } catch (IOException e) {
                    //TODO:add logging
                    e.printStackTrace();
                }
            }

        }

    }

    @Override
    public List<LocalFile> listFiles(LocalFile file) {
        File in= new File(file.getPath());
        List<LocalFile> result= new LinkedList<>();
        for(File ff:in.listFiles()){
            result.add(getItem(ff.getAbsolutePath()));
        }
        return result;
    }

    @Override
    public String getFolderSeparator() {
        return File.separator;
    }

    @Override
    public void deleteFilesFromDirectory(LocalFile path) {
        File in= new File(path.getPath());
        File[] fList = in.listFiles();
        for(File ff:fList){
            ff.delete();
        }
    }
}
