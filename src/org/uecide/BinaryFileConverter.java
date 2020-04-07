package org.uecide;

import java.io.File;

public class BinaryFileConverter implements Runnable {
    File source;
    File destination;
    File result;
    Context ctx;
    int state;
    String[] headerLines;

    public final static int UNCOMPILED = 0;
    public final static int COMPILING = 1;
    public final static int COMPILED = 2;
    public final static int FAILED = 3;

    public BinaryFileConverter(Context ctx, File source, File destination) {
        this.source = source;
        this.destination = destination;
        this.ctx = ctx;
        this.state = UNCOMPILED;
        this.headerLines = null;
    }

    public void run() {
        state = COMPILING;

        FileConverter conv = null;

        if (Preferences.getBoolean("compiler.verbose_files")) {
            ctx.bullet3(source.getName());
        }

        int type = FileType.getType(source);

        switch (type) {
            case FileType.GRAPHIC:
                if (ctx.getSketch().getInteger("binary." + source.getName() + ".conversion") > 1) {
                    conv = new ImageFileConverter(source, ctx.getSketch().getInteger("binary." + source.getName() + ".conversion"), ctx.getSketch().get("binary." + source.getName() + ".datatype"), ctx.getSketch().get("binary." + source.getName() + ".prefix"), ctx.getSketch().getColor("binary." + source.getName() + ".transparency"), ctx.getSketch().getInteger("binary." + source.getName() + ".threshold"));
                } else {
                    conv = new BasicFileConverter(source, ctx.getSketch().get("binary." + source.getName() + ".prefix"));
                }
                break;

            default:
                conv = new BasicFileConverter(source, ctx.getSketch().get("binary." + source.getName() + ".prefix"));
                break;
        }

        if (conv != null) {
            if (conv.convertFile(destination)) {
                result = conv.getFile();
                headerLines = conv.getHeaderLines();
            }
        }

        if (result == null) {
            state = FAILED;
        } else {
            state = COMPILED;
        }
    }

    public File getResult() {
        return result;
    }

    public String[] getHeaders() {
        return headerLines;
    }

    public int getState() {
        return state;
    }
    
    public File getFile() {
        return source;
    }
}
