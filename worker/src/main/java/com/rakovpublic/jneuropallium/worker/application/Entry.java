/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.application;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Entry {
    private static final Logger logger = LogManager.getLogger(Entry.class);

    public static void main(String [] args){
        Runner runner = new Runner();
        if(args.length==4){
            runner.runNet(args[0],args[1],args[2],args[3]);
        }else {
            logger.error("incorrect amount of arguments");
        }
    }
}
