package org.uecide;

import java.io.File;

public class TodoEntry {
    File file;
    int line;
    String comment;
    int type;

    public static final int Note = 1;
    public static final int Todo = 2;
    public static final int Fixme = 3;

    public TodoEntry(File f, int l, String c, int t) {
        file = f;
        line = l;
        comment = c;
        type = t;
    }

    public String toString() {
        return comment;
    }

    public int getLine() {
        return line;
    }

    public File getFile() {
        return file;
    }

    public int getType() {
        return type;
    }
}

