package synchronizer;

/***
 * Created by Rakovskyi Dmytro on 08.06.2018.
 */
public class Context implements IContext {
    private static Context ctx = new Context();

    private Context() {

    }

    public static Context getContext() {
        return ctx;
    }

    @Override
    public String getProperty(String propertyName) {
        return null;
    }


}
