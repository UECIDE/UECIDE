package org.uecide;

interface DataStreamParser {
    public String parseStreamMessage(Context ctx, String data);
    public String parseStreamError(Context ctx, String data);
}
