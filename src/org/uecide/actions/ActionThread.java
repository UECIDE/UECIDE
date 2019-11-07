package org.uecide.actions;

import org.uecide.Context;

public class ActionThread extends Thread {
    Context ctx;
    String name;
    Object[] args;

    public ActionThread(Context c, String n, Object a[]) {
        ctx = c;
        name = n;
        args = a;
    }

    public void run() {
        ctx.threads.put(name.toLowerCase(), this);
        Action.run(ctx, name, args);
        ctx.threads.remove(name);
    }
    
}
