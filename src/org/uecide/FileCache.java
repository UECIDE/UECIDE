package org.uecide;

import java.io.*;
import java.util.*;

public class FileCache {

    static ArrayList<String> files =
        new ArrayList<String>();

    public static void add(File root) {
        File[] list = root.listFiles();
        ArrayList<File>dirs = new ArrayList<File>();

        if (list == null) return;
        for (File f : list) {
            if (f.isDirectory()) {
                dirs.add(f);
                continue;
            }

            if (f.getName().equals("compiler.txt")) {
                files.add(f.getAbsolutePath());
                return;
            }
            if (f.getName().equals("core.txt")) {
                files.add(f.getAbsolutePath());
                return;
            }
            if (f.getName().equals("board.txt")) {
                files.add(f.getAbsolutePath());
                return;
            }
            if (f.getName().equals("programmer.txt")) {
                files.add(f.getAbsolutePath());
                return;
            }
            if (f.getName().equals("tool.txt")) {
                files.add(f.getAbsolutePath());
                return;
            }
            if (f.getName().equals("icons.txt")) {
                files.add(f.getAbsolutePath());
                return;
            }

        }
        for (File d : dirs) {
            add(d);
        }
    }

    public static ArrayList<File> getFilesByExtension(String ext) {
        ArrayList<File> out = new ArrayList<File>();
        for (String fn : files) {
            if (fn.endsWith("." + ext)) {
                out.add(new File(fn));
            }
        }
        return out;
    }

    public static ArrayList<File> getFilesByName(String name) {
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
