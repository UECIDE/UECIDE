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
                while (in.available() > 0) {
                    in.read(buffer);
                    out.write(buffer);
                }
                Thread.sleep(10);
            } catch (Exception ignored) {
            }
         }
    }

    public void stop() {
        running = false;
    }
}
