package org.uecide;

import java.io.File;

public class FileCompiler implements Runnable {
    File source;
    File destination;
    File result;
    Context ctx;
    int state;

    public final static int UNCOMPILED = 0;
    public final static int COMPILING = 1;
    public final static int COMPILED = 2;
    public final static int FAILED = 3;

    public FileCompiler(Context ctx, File source, File destination) {
        this.source = source;
        this.destination = destination;
        this.ctx = ctx;
        this.state = UNCOMPILED;
    }

    public void run() {
        state = COMPILING;
        result = ctx.compileFile(null, source, destination);
        if (result == null) {
            state = FAILED;
        } else {
            state = COMPILED;
        }
    }

    public File getResult() {
        return result;
    }

    public int getState() {
        return state;
    }
    
    public File getFile() {
        return source;
    }
}
