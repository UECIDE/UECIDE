package org.uecide;

import java.io.OutputStream;

public class ContextStream extends OutputStream {
    Context ctx;
    int type;

    public ContextStream(Context context, int t) {
        super();
        ctx = context;
        type = t;
    }

    @Override
    public void write(int c) {
        if (c == 0) return;
        String l = String.valueOf((char)c);
        ctx.triggerEvent("message", new Message(type, l));
    }

    @Override
    public void write(byte[] b, int off, int len) {
        if (b.length == 0) return;
        String l = new String(b, off, len);
        ctx.triggerEvent("message", new Message(type, l));
    }

    @Override
    public void write(byte[] b) {
        if (b.length == 0) return;
        String l = new String(b);
        ctx.triggerEvent("message", new Message(type, l));
    }
}
