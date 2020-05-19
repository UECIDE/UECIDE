package org.uecide;

import java.io.OutputStream;

public class VariableOutputStream extends OutputStream {
    Context ctx;
    String key;

    public VariableOutputStream(Context context, String k) {
        super();
        ctx = context;
        key = k;
        ctx.set(key, ""); // Start empty
    }

    @Override
    public void write(int c) {
        if (c == 0) return;
        String l = String.valueOf((char)c);
        String s = ctx.get(key);
        s = s + l;
        ctx.set(key, s);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        if (b.length == 0) return;
        String l = new String(b, off, len);
        String s = ctx.get(key);
        s = s + l;
        ctx.set(key, s);
    }

    @Override
    public void write(byte[] b) {
        if (b.length == 0) return;
        String l = new String(b);
        String s = ctx.get(key);
        s = s + l;
        ctx.set(key, s);
    }
}
