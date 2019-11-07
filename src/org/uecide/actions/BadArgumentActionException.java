package org.uecide.actions;

import java.lang.Throwable;

public class BadArgumentActionException extends ActionException {
    public BadArgumentActionException() { }
    public String toString() {
        return "Bad Argument in Action";
    }
}
