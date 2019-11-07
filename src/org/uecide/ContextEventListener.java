package org.uecide;

public abstract interface ContextEventListener {
    public abstract void contextEventTriggered(String event, Context context);
}
