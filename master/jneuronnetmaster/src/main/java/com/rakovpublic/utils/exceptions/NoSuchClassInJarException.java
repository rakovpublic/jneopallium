package com.rakovpublic.utils.exceptions;

public class NoSuchClassInJarException extends NullPointerException {
    public NoSuchClassInJarException(String s) {
        super(s);
    }
}
