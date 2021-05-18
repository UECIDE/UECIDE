package org.uecide.builtin;

import java.lang.Throwable;

public class BuiltinCommandException extends Throwable {
    String message;

    public BuiltinCommandException(String m) {
        message = m;
    }   

    public String toString() {
        return "BuiltinCommandException[\"" + message + "\"]";
    }   
}

