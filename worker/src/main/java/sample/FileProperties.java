package sample;

import com.rakovpublic.jneuropallium.worker.synchronizer.IContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class FileProperties implements IContext {
    private static FileProperties fileProperties = new FileProperties();
    private Properties props = new Properties();

    private FileProperties() {
        init();
    }

    private void init() {
        InputStream inputStream = getClass()
                .getClassLoader().getResourceAsStream("config.properties");
        try {
            props.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static FileProperties getFileProperties() {
        return fileProperties;
    }

    @Override
    public String getProperty(String propertyName) {
        return props.getProperty(propertyName, null);
    }
}
