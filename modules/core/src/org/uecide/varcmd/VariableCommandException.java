package org.uecide.varcmd;

import java.lang.Throwable;

public class VariableCommandException extends Throwable {
    String message;

    public VariableCommandException(String m) {
        message = m;
    }

    public String toString() {
        return "VariableCommandException[\"" + message + "\"]";
    }
}
