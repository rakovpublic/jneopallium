package synchronizer;

import web.layers.IResultLayer;
import web.storages.IStructMeta;

/**
 * Created by Rakovskyi Dmytro on 08.06.2018.
 */
public interface IContext {

    String getProperty(String propertyName);

    void configure(IStructMeta structMeta);

    IStructMeta getStructure();

    IResultLayer process();


}
