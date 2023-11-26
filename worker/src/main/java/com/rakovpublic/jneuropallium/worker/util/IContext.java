/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.util;

import java.io.Serializable;

/***
 * Created by Rakovskyi Dmytro on 08.06.2018.
 */
public interface IContext extends Serializable {

    String getProperty(String propertyName);

    void update(String path);


}
