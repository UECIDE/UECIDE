package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.Debug;

import com.wittams.gritty.Questioner;
import com.wittams.gritty.Tty;

import java.awt.Dimension;
import java.io.IOException;
import java.util.concurrent.LinkedTransferQueue;

public class ConsoleTty implements Tty {

    LinkedTransferQueue<Byte> buffer = new LinkedTransferQueue<Byte>();
    Context ctx;

    public ConsoleTty(Context c) {
        ctx = c;
    }

    public boolean init(Questioner q) {
        feed("[32m[40m[2J[1;1H");
        return true;
    }

    public void close() {
    }

    public void resize(Dimension termSize, Dimension pixelSize) {
    }

    public String getName() {
        return "Console";
    }

    public int read(byte[] buf, int offset, int length) throws IOException {
        try {
            int i = 0;
            buf[i++] = buffer.take();
            while ((!buffer.isEmpty()) && (i < length)) {
                buf[i++] = buffer.take();
            }
            return i;
        } catch (InterruptedException e) {
            Debug.exception(e);
            throw new IOException(e.getMessage());
        }
    }

    public void write(byte[] bytes) throws IOException {
    }

    public void feed(String s) {
        feed(s.getBytes());
    }

    public void feed(byte[] arr) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == 10) {
                buffer.add((byte)13);
            }
            buffer.add(arr[i]);
        }
    }

    public void feed(byte b) {
        if (b == 10) {
            buffer.add((byte)13);
        }
        buffer.add(b);
    }
}
