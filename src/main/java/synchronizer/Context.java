package synchronizer;

import web.layers.IResultLayer;
import web.storages.IStructMeta;

/**
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

    @Override
    public void configure(IStructMeta structMeta) {

    }

    @Override
    public IStructMeta getStructure() {
        return null;
    }

    @Override
    public IResultLayer process() {
        return null;
    }
}
