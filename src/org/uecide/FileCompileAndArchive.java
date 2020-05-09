package org.uecide;

import java.io.File;
import java.nio.file.Files;

import java.security.MessageDigest;

public class FileCompileAndArchive extends QueueJob {
    File source;
    File destination;
    File result;
    File sourceRoot;
    File archive;
    long archiveModificationTime;

    static Object archiveMutex = new Object();
    
    public FileCompileAndArchive(Context ctx, File source, File destination, File sourceRoot, File archive) {
        super(ctx);
        this.source = source;
        this.destination = destination;
        this.sourceRoot = sourceRoot;
        this.archive = archive;
        if (this.archive.exists()) {
            this.archiveModificationTime = this.archive.lastModified();
        } else {
            this.archiveModificationTime = 0;
        }
    }

    public void run() {
        state = RUNNING;
        ctx.triggerEvent("fileCompileStarted", this);
        result = ctx.compileFile(null, source, destination, sourceRoot);
        if (result == null) {
            state = FAILED;
            ctx.triggerEvent("fileCompileFailed", this);
            return;
        }
        ctx.triggerEvent("fileCompileFinished", this);

        if (result.lastModified() > archiveModificationTime) {
            try {
                synchronized (archiveMutex) {
                    ctx.triggerEvent("fileArchiveStarted", this);

                    String path = destination.toPath().relativize(result.toPath()).toString();
                    path = path.replace('\\', '_');
                    path = path.replace('/', '_');
                    File hashFile = new File(destination, path);

                    if (!hashFile.equals(result)) {
                        if (hashFile.exists()) hashFile.delete();
                        Files.copy(result.toPath(), hashFile.toPath());
                    }

                    ctx.set("object.name", hashFile.getAbsolutePath());
                    ctx.set("library", archive.getAbsolutePath());
                    if (!((Boolean)ctx.executeKey("compile.ar"))) {
                        ctx.triggerEvent("fileArchiveFailed", this);
                        state = FAILED;
                        return;
                    }
                    if (!hashFile.equals(result)) {
                        hashFile.delete();
                    }
                    ctx.triggerEvent("fileArchiveFinished", this);
                }
            } catch (Exception ex) {
                Debug.exception(ex);
                state = FAILED;
                return;
            }
        }
        state = COMPLETED;
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
