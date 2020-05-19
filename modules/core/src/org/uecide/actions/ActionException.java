package org.uecide.actions;

import java.lang.Throwable;

public class ActionException extends Throwable {
    String message;

    public ActionException(String m) {
        message = m;
    }

    public ActionException() {
        message = null;
    }

    public String toString() {
        return "ActionException[\"" + message + "\"]";
    }
}
