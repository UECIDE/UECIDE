package org.uecide;

import java.io.InputStream;
import java.io.OutputStream;

public class ProcessStreamThread implements Runnable {
    InputStream in;
    OutputStream out;

    boolean running = true;

    public ProcessStreamThread(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
    }

    public void run() {
        byte[] buffer = new byte[1024];
        while (running) {
            try {
                int v = in.read(buffer);
                if (v == -1) break;
                out.write(buffer, 0, v);
            } catch (Exception ignored) {
                Debug.exception(ignored);
            }
        }
    }

    public void stop() {
        running = false;
    }
}
