package org.uecide;

import java.io.InputStream;

/* Equivalent to </dev/null
 */

public class NullInputStream extends InputStream {
    Context ctx;

    public NullInputStream(Context c) {
        super();
        ctx = c;
    }

    @Override
    public int available() {
        return 0;
    }

    @Override
    public int read() {
        return -1;
    }

    @Override
    public int read(byte[] b) {
        return -1;
    }

    @Override
    public int read(byte[] b, int off, int len) {
        return -1;
    }
}
