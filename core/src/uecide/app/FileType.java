package uecide.app;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;
import java.util.jar.*;
import uecide.plugin.*;


import javax.swing.*;
import javax.imageio.*;

import uecide.app.debug.Board;
import uecide.app.debug.Core;
import uecide.app.debug.Compiler;

import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.*;


public class FileType {

    public static class FileTypeInfo {
        public int type;
        public boolean canEdit;
        public String style;
        public FileTypeInfo(int t, boolean c, String s) {
            type = t;
            canEdit = c;
            style = s;
        }
    };
        

    public static final int INVALID = 0;
    public static final int HEADER = 1;
    public static final int CSOURCE = 2;
    public static final int CPPSOURCE = 3;
    public static final int ASMSOURCE = 4;
    public static final int OBJECT = 5;
    public static final int LIBRARY = 6;
    public static final int SKETCH = 7;

    public static HashMap<String, FileTypeInfo> fileTypeList;
    static {
        fileTypeList = new HashMap<String, FileTypeInfo>();
        fileTypeList.put("c",   new FileTypeInfo(FileType.CSOURCE, true, SyntaxConstants.SYNTAX_STYLE_C));

        fileTypeList.put("cpp", new FileTypeInfo(FileType.CPPSOURCE, true, SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS));
        fileTypeList.put("c++", new FileTypeInfo(FileType.CPPSOURCE, true, SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS));
        fileTypeList.put("cc",  new FileTypeInfo(FileType.CPPSOURCE, true, SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS));
        fileTypeList.put("cp",  new FileTypeInfo(FileType.CPPSOURCE, true, SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS));
        fileTypeList.put("cxx", new FileTypeInfo(FileType.CPPSOURCE, true, SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS));
        fileTypeList.put("CPP", new FileTypeInfo(FileType.CPPSOURCE, true, SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS));
        fileTypeList.put("C",   new FileTypeInfo(FileType.CPPSOURCE, true, SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS));

        fileTypeList.put("S",   new FileTypeInfo(FileType.ASMSOURCE, true, SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_AVR));
        fileTypeList.put("sx",  new FileTypeInfo(FileType.ASMSOURCE, true, SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_AVR));

        fileTypeList.put("h",   new FileTypeInfo(FileType.HEADER, true, SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS));
        fileTypeList.put("H",   new FileTypeInfo(FileType.HEADER, true, SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS));
        fileTypeList.put("hh",  new FileTypeInfo(FileType.HEADER, true, SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS));
        fileTypeList.put("hp",  new FileTypeInfo(FileType.HEADER, true, SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS));
        fileTypeList.put("hxx", new FileTypeInfo(FileType.HEADER, true, SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS));
        fileTypeList.put("hpp", new FileTypeInfo(FileType.HEADER, true, SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS));
        fileTypeList.put("HPP", new FileTypeInfo(FileType.HEADER, true, SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS));
        fileTypeList.put("h++", new FileTypeInfo(FileType.HEADER, true, SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS));
        fileTypeList.put("tcc", new FileTypeInfo(FileType.HEADER, true, SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS));

        fileTypeList.put("o",   new FileTypeInfo(FileType.OBJECT, false, SyntaxConstants.SYNTAX_STYLE_NONE));
        fileTypeList.put("a",   new FileTypeInfo(FileType.LIBRARY, false, SyntaxConstants.SYNTAX_STYLE_NONE));

        fileTypeList.put("pde", new FileTypeInfo(FileType.SKETCH, true, SyntaxConstants.SYNTAX_STYLE_ARDUINO));
        fileTypeList.put("ino", new FileTypeInfo(FileType.SKETCH, true, SyntaxConstants.SYNTAX_STYLE_ARDUINO));
    }

    public static int getType(File f) { return getType(f.getName()); }
    public static int getType(String f) {
        for (String extension : fileTypeList.keySet()) {
            if (f.endsWith("." + extension)) {
                return fileTypeList.get(extension).type;
            }
        }
        return FileType.INVALID;
    }

    public static boolean isValid(File f) { return isValid(f.getName()); }
    public static boolean isValid(String f) {
        return getType(f) != FileType.INVALID;
    }

    public static boolean canEdit(File f) { return canEdit(f.getName()); }
    public static boolean canEdit(String f) {
        int type = getType(f);
        switch (type) {
            case FileType.INVALID:
            case FileType.OBJECT:
            case FileType.LIBRARY:
                return false;
            case FileType.HEADER:
            case FileType.CSOURCE:
            case FileType.CPPSOURCE:
            case FileType.ASMSOURCE:
            case FileType.SKETCH:
                return true;
        }
        return false;
    }

    public static String editableList() {
        String out = "";
        for (String extension : fileTypeList.keySet()) {
            if (canEdit("dummy." + extension)) {
                if (out.equals("")) {
                    out = "." + extension;
                } else {
                    out += ", ." + extension;
                }
            }
        }
        return out;
    }

    public static String getSyntaxStyle(File f) { return getSyntaxStyle(f.getName()); }
    public static String getSyntaxStyle(String f) {
        for (String extension : fileTypeList.keySet()) {
            if (f.endsWith("." + extension)) {
                return fileTypeList.get(extension).style;
            }
        }
        return SyntaxConstants.SYNTAX_STYLE_NONE;
    }
};
