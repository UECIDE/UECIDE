package org.uecide;

public class ContextEvent {
    Context ctx;
    String name;
    Object object;

    public ContextEvent(Context c, String n) {
        ctx = c;
        name = n;
        object = null;
    }

    public ContextEvent(Context c, String n, Object o) {
        ctx = c;
        name = n;
        object = o;
    }

    public String toString() {
        if (object != null) {
            return "ContextEvent(" + name + ")";
        } else {
            return "ContextEvent(" + name + ", {" + object.toString() + "})";
        }
    }

    public Context getContext() {
        return ctx;
    }

    public String getEvent() {
        return name;
    }

    public Object getObject() {
        return object;
    }
}
