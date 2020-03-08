package org.uecide;

public class Message {
    public static final int NORMAL      = 0;
    public static final int WARNING     = 1;
    public static final int ERROR       = 2;
    public static final int BULLET1     = 3;
    public static final int BULLET2     = 4;
    public static final int BULLET3     = 5;
    public static final int COMMAND     = 6;
    public static final int HEADING     = 7;
    public static final int STREAM_MESSAGE      = 8;
    public static final int STREAM_ERROR        = 9;

    int type;
    String text;

    public Message(int t, String m) {
        type = t;
        text = m;
    }

    public String toString() { return text; }
    public int getMessageType() { return type; }
    public String getText() { return text; }
}
