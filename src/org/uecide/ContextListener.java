package org.uecide;

public interface ContextListener {
    public void contextError(String message);
    public void contextWarning(String message);
    public void contextMessage(String message);
}

