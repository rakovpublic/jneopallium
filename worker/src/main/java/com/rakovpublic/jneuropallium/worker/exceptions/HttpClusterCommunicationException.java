/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.exceptions;

public class HttpClusterCommunicationException extends NullPointerException {
    public HttpClusterCommunicationException() {
    }

    public HttpClusterCommunicationException(String s) {
        super(s);
    }
}
