package org.uecide;

import java.io.File;

public class FileCompiler extends QueueJob {
    File source;
    File destination;
    File result;
    File sourceRoot;

    public FileCompiler(Context ctx, File source, File destination, File sourceRoot) {
        super(ctx);
        this.source = source;
        this.destination = destination;
        this.sourceRoot = sourceRoot;
    }

    public void run() {
        state = RUNNING;
        ctx.triggerEvent("fileCompileStarted", this);
        result = ctx.compileFile(null, source, destination, sourceRoot);
        if (result == null) {
            state = FAILED;
            ctx.triggerEvent("fileCompileFailed", this);
        } else {
            state = COMPLETED;
            ctx.triggerEvent("fileCompileFinished", this);
        }
    }

    public File getResult() {
        return result;
    }

    public File getFile() {
        return source;
    }

    public void kill() {
        ctx.killAllRunningProcesses();
    }
}
