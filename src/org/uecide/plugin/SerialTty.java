package org.uecide.plugin;

import org.uecide.*;

import com.wittams.gritty.Questioner;
import com.wittams.gritty.Tty;

import java.awt.Dimension;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.concurrent.LinkedTransferQueue;

public class SerialTty implements Tty, CommsListener {

    CommunicationPort port;
    boolean addcr = false;
    boolean doDiscardInput = false;

    LinkedTransferQueue<Byte> buffer = new LinkedTransferQueue<Byte>();

    FileOutputStream captureFile = null;

    SerialTty(CommunicationPort p) {
        port = p;
        port.addCommsListener(this);
    }

    public boolean init(Questioner q) {
        feed("[37m[40m");
        return true;
    }

    public void close() {
    }

    public void resize(Dimension termSize, Dimension pixelSize) {
    }

    public String getName() {
        return "Serial";
    }

    public int read(byte[] buf, int offset, int length) throws IOException {

        try {
            int i = 0;
            buf[i++] = buffer.take();
            while ((!buffer.isEmpty()) && (i < length)) {
                buf[i++] = buffer.take();
            }
            return i;
        } catch (Exception e) {
            Base.error(e);
        }
        return 0;
    }

    public void write(byte[] bytes) throws IOException {
        for (int i = 0; i < bytes.length; i++) {
            port.write(bytes[i]);
        }
    }

    public void feed(String s) {
        feed(s.getBytes());
    }

    public void feed(byte[] arr) {
        if (doDiscardInput) return;
        for (int i = 0; i < arr.length; i++) {
            if ((arr[i] == 10) && addcr) {
                buffer.add((byte)13);
            }
            buffer.add(arr[i]);
        }
    }

    public void feed(byte b) {
        if (doDiscardInput) return;
        if (b == 10) {
            buffer.add((byte)13);
        }
        buffer.add(b);
    }

    public void commsDataReceived(byte[] bytes) {
        if (captureFile != null) {
            try {
                captureFile.write(bytes);
            } catch (Exception e) {
            }
        }
        feed(bytes);
    }

    public void commsEventReceived(CommsEvent event) {
    }

    public void setAddCR(boolean ac) {
        addcr = ac;
    }

    public void discardInput(boolean di) {
        doDiscardInput = di;
    }

    public void captureToFile(FileOutputStream f) {
        captureFile = f;
    }

    public void endCapture() {
        captureFile = null;
    }
}

