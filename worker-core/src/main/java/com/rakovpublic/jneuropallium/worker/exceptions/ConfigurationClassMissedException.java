/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.exceptions;

public class ConfigurationClassMissedException extends NullPointerException {
    public ConfigurationClassMissedException() {
    }

    public ConfigurationClassMissedException(String s) {
        super(s);
    }
}
