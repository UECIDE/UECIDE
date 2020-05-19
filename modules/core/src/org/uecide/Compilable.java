package org.uecide;

import java.io.*;
import java.util.*;

public abstract class Compilable {

    public abstract File[] getSourceFiles();
    public abstract String getCompilationFolderName();
    public File[] compile(File buildFolder) {
        ArrayList<File> outFiles = new ArrayList<File>();
        File[] files = getSourceFiles();
        File compileTo = new File(buildFolder, getCompilationFolderName());
        if (!compileTo.exists()) {
            compileTo.mkdirs();
        }
        for (File file : files) {
            File o = compileFile(file, compileTo);
            if (o == null) {
                return null;
            } else {
                outFiles.add(o);
            }
        }
        return outFiles.toArray(new File[0]);
    }

    public File compileFile(File source, File dest) {
        return null;
    }
}
