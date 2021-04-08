package com.rakovpublic.jneuropallium.worker.synchronizer;

import java.io.Serializable;

/***
 * Created by Rakovskyi Dmytro on 08.06.2018.
 */
public interface IContext extends Serializable {

    String getProperty(String propertyName);


}
