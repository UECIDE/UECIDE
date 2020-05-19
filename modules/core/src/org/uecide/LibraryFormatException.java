package org.uecide;

import java.lang.Throwable;

public class LibraryFormatException extends Throwable {
    String message;

    public LibraryFormatException(String m) {
        message = m;
    }

    @Override
    public String toString() {
        return "Library format exception: " + message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}

