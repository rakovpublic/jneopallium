package sample;

import java.io.*;

public class ByteSaverHelper {
    public static void saveObjectToFile(String path,Object obj){
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            String serializedObject;
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream so = new ObjectOutputStream(bo);
            so.writeObject(obj);
            so.flush();
            serializedObject = bo.toString();
            writer.write(serializedObject);

        } catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void saveArrayObjectToFile(String path,Object [] obj){
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            String serializedObject;
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream so = new ObjectOutputStream(bo);
            so.writeObject(obj);
            so.flush();
            serializedObject = bo.toString();
            writer.write(serializedObject);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
