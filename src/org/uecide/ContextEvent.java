package org.uecide;

public class ContextEvent {
    Context ctx;
    String name;

    public ContextEvent(Context c, String n) {
        ctx = c;
        name = n;
    }

    public String toString() {
        return "ContextEvent(" + name + ")";
    }

    public Context getContext() {
        return ctx;
    }

    public String getEvent() {
        return name;
    }
}
