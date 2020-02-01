package sample;

import net.storages.filesystem.IFileSystem;

import java.io.*;
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
            try {
                reader = new BufferedReader(new FileReader(path.getPath()));
            StringBuilder builder = new StringBuilder();
            currentLine = reader.readLine();
            while (currentLine != null) {
                builder.append(currentLine);
                builder.append("n");
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
            return currentLine;


        }
        return null;
    }

    @Override
    public boolean writeCreate(String content, LocalFile path) {
        return false;
    }

    @Override
    public boolean rewrite(String content, LocalFile path) {
        return false;
    }

    @Override
    public boolean writeUpdate(String content, LocalFile path, int startFrom) {
        return false;
    }

    @Override
    public boolean writeUpdateObject(Object content, LocalFile path) {
        return false;
    }

    @Override
    public boolean writeUpdateObjects(Object[] content, LocalFile path) {
        return false;
    }

    @Override
    public boolean writeUpdateToEnd(String content, LocalFile path) {
        return false;
    }

    @Override
    public boolean delete(String path) {
        return false;
    }

    @Override
    public boolean delete(LocalFile path) {
        return false;
    }

    @Override
    public boolean deleteContent(LocalFile path, int startFrom, int amount) {
        return false;
    }

    @Override
    public void copy(LocalFile source, LocalFile destination) {

    }

    @Override
    public List<LocalFile> listFiles(LocalFile file) {
        return null;
    }

    @Override
    public String getFolderSeparator() {
        return null;
    }

    @Override
    public void deleteFilesFromDirectory(LocalFile path) {

    }
}
