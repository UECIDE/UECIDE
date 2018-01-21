package org.uecide;

import java.io.*;
import java.util.*;

public class FileCache {

    ArrayList<String> files;

    public FileCache() {
        files = new ArrayList<String>();
    }

    public void add(File root) {
        File[] list = root.listFiles();
        if (list == null) return;
        for (File f : list) {
            files.add(f.getAbsolutePath());
            if (f.isDirectory()) {
                add(f);
            }
        }
    }

    public ArrayList<File> getFilesByExtension(String ext) {
        ArrayList<File> out = new ArrayList<File>();
        for (String fn : files) {
            if (fn.endsWith("." + ext)) {
                out.add(new File(fn));
            }
        }
        return out;
    }

    public ArrayList<File> getFilesByName(String name) {
        ArrayList<File> out = new ArrayList<File>();
        for (String fn : files) {
            File f = new File(fn);
            if (f.getName().equals(name)) {
                out.add(f);
            }
        }
        return out;
    }
}
