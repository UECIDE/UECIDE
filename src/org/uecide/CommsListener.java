package org.uecide;

public interface CommsListener {
    public void commsDataReceived(byte[] data);
    public void commsEventReceived(CommsEvent event);
}
