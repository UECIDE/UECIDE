package org.uecide;

import java.io.OutputStream;

public class NullOutputStream extends OutputStream {
    public NullOutputStream() {
        super();
    }

    @Override
    public void write(int c) {
    }

    @Override
    public void write(byte[] b, int off, int len) {
    }

    @Override
    public void write(byte[] b) {
    }
}
