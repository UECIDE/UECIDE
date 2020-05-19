package org.uecide.actions;

import java.lang.Throwable;

public class FileNotFoundActionException extends ActionException {
    public FileNotFoundActionException() { }
    public String toString() {
        return "File Not Found in Action";
    }
}
