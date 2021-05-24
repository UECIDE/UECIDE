package org.uecide.builtin;

import org.uecide.Base;
import org.uecide.Context;
import org.uecide.Sketch;
import org.uecide.FileType;

import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;

import java.util.HashMap;


/*
 * Export the active sketch as an Arduino project, including converting
 * any special files.
 *
 * __builtin_export::/path/to/export/to
 */

public class export extends BuiltinCommand {
    public boolean main(Context ctx, String[] args) throws BuiltinCommandException {

        if (args.length != 1) {
            throw new BuiltinCommandException("Syntax Error");
        }

        Sketch sketch = ctx.getSketch();

        File dir = new File(args[0], sketch.getName());
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (File f : files) {
                if (f.getName().equals("build")) {
                    if (f.isDirectory()) continue;
                }
                if (f.isDirectory()) {
                    Base.removeDir(f);
                } else {
                    f.delete();
                }
            }
        }

        dir.mkdirs();

        File mainFile = sketch.getMainFile();
        File mainOutputFile = new File(dir, mainFile.getName());

        boolean hasHeader = false;

        for (File f : sketch.sketchFiles) {
            int ft = FileType.getType(f);
            String fname = f.getName();
        
            switch (ft) {
                case FileType.ASMSOURCE:
                case FileType.CSOURCE:
                case FileType.CPPSOURCE:
                case FileType.SKETCH:
                case FileType.HEADER:
                    if (!fname.equals(sketch.getName() + ".ino")) 
                        sketch.dumpFileData(dir, fname);
                    }
                    break;
                
        }


        HashMap<File, String[]> binFiles = sketch.convertBinaryFiles();

        if (binFiles.size() > 0) {
            hasHeader = true;

            try {
                PrintWriter pw = new PrintWriter(new File(dir, "uecide.h"));
        
                pw.println("#ifndef _UECIDE_H");
                pw.println("#define _UECIDE_H");
                pw.println("");

                File src = new File(dir, "src");

                src.mkdirs();

                for (File f : binFiles.keySet()) {
                    String fname = f.getName();
                    Base.copyFile(f, new File(src, fname));
                    String[] hlines = binFiles.get(f);
                    pw.println("// " + fname);
                    for(String hl : hlines) {
                        pw.println(hl);
                    }
                    pw.println("");
                }

                pw.println("#endif");
                pw.close();
            } catch (IOException ex) {
                throw new BuiltinCommandException(ex.getMessage());
            }
        }

        try {
            PrintWriter pw = new PrintWriter(mainOutputFile);

            if (hasHeader) {
                pw.println("// Included by UECIDE export");
                pw.println("#include \"uecide.h\"");
                pw.println("");
            }

            pw.print(sketch.getFileContent(mainFile));
            pw.close();
        } catch (IOException ex) {
            throw new BuiltinCommandException(ex.getMessage());
        }

        return true;

    }

    public void kill() {
    }
}
