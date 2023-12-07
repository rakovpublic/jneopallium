package com.rakovpublic.jneuropallium.worker.application;

import com.rakovpublic.jneuropallium.worker.util.IContext;

public interface IRunner {


    void runNet(String mode, String jarPath, String contextClass, String contextJson);


}
