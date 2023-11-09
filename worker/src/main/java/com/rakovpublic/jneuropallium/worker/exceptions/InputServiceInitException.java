/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.exceptions;

import java.io.FileNotFoundException;

public class InputServiceInitException extends NullPointerException {
    public InputServiceInitException() {
    }

    public InputServiceInitException(String s) {
        super(s);
    }
}
