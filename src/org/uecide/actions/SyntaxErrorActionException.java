package org.uecide.actions;

import java.lang.Throwable;

public class SyntaxErrorActionException extends ActionException {
    public SyntaxErrorActionException() { }
    public String toString() {
        return "Syntax Error in Action";
    }
}
