package org.uecide;

import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class PercentProgressFilter extends OutputStream {
    Context ctx;
    int type;
    OutputStream passthrough;
    Pattern regex;
    StringBuilder line;

    public PercentProgressFilter(Context context, OutputStream pass, String reg) {
        super();
        ctx = context;
        passthrough = pass;
        regex = Pattern.compile(reg);
        line = new StringBuilder();
    }

    boolean processLine() {
        String l = line.toString();
        Matcher m = regex.matcher(l);
        if (m.find()) {
            ctx.triggerEvent("percentComplete", Utils.s2i(m.group(1)));
            return true;
        }
        return false;
    }

    void processCharacter(char ch) throws IOException {
        synchronized(line) {
            switch (ch) {
                case '\n':
                case '\r':
                    if (!processLine()) {
                        String l = line.toString();
                        passthrough.write(l.getBytes());
                    }
                    line.setLength(0);
                    break;
                default:
                    line.append(String.valueOf(ch));
                    break;
            }
        }
    }

    @Override
    public void write(int c) throws IOException {
        if (c == 0) return;
        processCharacter((char)c);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (b.length == 0) return;

        for (int i = 0; i < len; i++) {
            char ch = (char)b[off + i];
            processCharacter(ch);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        if (b.length == 0) return;

        for (int i = 0; i < b.length; i++) {
            char ch = (char)b[i];
            processCharacter(ch);
        }
    }
}
