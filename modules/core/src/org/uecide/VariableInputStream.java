package org.uecide;

import java.io.InputStream;

public class VariableInputStream extends InputStream {
    Context ctx;
    String key;
    int readPos;
    String data;

    public VariableInputStream(Context c, String k) {
        super();
        ctx = c;
        key = k;
        data = ctx.get(key);
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public int available() {
        return data.length() - readPos;
    }

    @Override
    public int read() {
        if (readPos >= data.length()) return -1;
        char c = data.charAt(readPos);
        readPos++;
        return c;
    }

    @Override
    public int read(byte[] b) {
        int i = 0;
        int c = read();
        if (c == -1) return -1;
        while (c > -1) {
            b[i] = (byte)c;
            i++;
            c = read();
        }
        return i;
    }

    @Override
    public int read(byte[] b, int off, int len) {
        int i = 0;
        int c = read();
        if (c == -1) return -1;
        while (c > -1) {
            b[off + i] = (byte)c;
            i++;
            c = read();
        }
        return i;
    }
}
