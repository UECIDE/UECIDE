package org.uecide;

public class CommsEvent {
    CommunicationPort port;
    int event;

    public CommsEvent(CommunicationPort p, int e) {
        port = p;
        event = e;
    }

    public CommunicationPort getSource() {
        return port;
    }

    public int getType() {
        return event;
    }
}
