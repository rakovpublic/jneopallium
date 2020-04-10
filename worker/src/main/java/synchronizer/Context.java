package synchronizer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/***
 * Created by Rakovskyi Dmytro on 08.06.2018.
 */
public class Context implements IContext {
    private static Context ctx = new Context();
    private Properties prop;

    private Context() {
        init();
    }

    public static Context getContext() {
        return ctx;
    }

    @Override
    public String getProperty(String propertyName) {
        return prop.getProperty(propertyName, null);
    }

    private void init() {
        try {
            InputStream input = getClass()
                    .getClassLoader().getResourceAsStream("config.properties");
            prop = new Properties();
            prop.load(input);

        } catch (IOException ex) {
            ex.printStackTrace();
            //TODO: add logger
        }

    }


}
