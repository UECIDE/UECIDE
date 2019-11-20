/*
 * Copyright (c) 2015, Majenko Technologies
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 *
 * * Neither the name of Majenko Technologies nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.uecide;

import java.awt.Color;

import javax.swing.ImageIcon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.text.SimpleDateFormat;

import java.util.UUID;
import java.util.Arrays;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * The sketch class is the heart of the IDE.  It manages not only what files a
 * sketch consists of, but also deals with compilation of the sketch and uploading
 * the sketch to a target board.
 */
public class Sketch {

    public Context ctx;

    public String sketchName;       // The name of the sketch
    public File sketchFolder;       // Where the sketch is
    public File buildFolder;        // Where to build the sketch
    public String uuid;             // A globally unique ID for temporary folders etc

    public String percentageFilter = null;
    public float percentageMultiplier = 1.0f;

    public String percentageCharacter = null;
    public int percentageCharacterCount = 0;

    boolean isUntitled;             // Whether or not the sketch has been named

    boolean terminateExecution = false;

    Process runningProcess = null;

    // This lot is what the sketch consists of - the list of files, libraries, parameters etc.
    public TreeMap<String, SketchFile> sketchFiles = new TreeMap<String, SketchFile>();

    public HashMap<String, Library> importedLibraries = new HashMap<String, Library>();
    public ArrayList<Library> orderedLibraries = new ArrayList<Library>();
    public ArrayList<String> unknownLibraries = new ArrayList<String>();

    TreeMap<String, String> selectedOptions = new TreeMap<String, String>();

    ArrayList<FunctionBookmark> functionListBm = new ArrayList<FunctionBookmark>();

    // Do we want to purge the cache files before building?  This is set by the
    // options system.
    public boolean doPrePurge = false;

    PropertyFile settings;

    /**************************************************************************
     * CONSTRUCTORS                                                           *
     *                                                                        *
     * The Sketch takes a file, be that as a string or a File object, as the  *
     * only parameter.  The sketch is loaded either from the provided folder, *
     * or the folder the provided file resides in.                            *
     **************************************************************************/

    HashMap<String, Integer>keywords = new HashMap<String, Integer>();

    public void clearLineComments() {
        for (SketchFile f : sketchFiles.values()) {
            f.clearLineComments();
        }
    }

    public Sketch() {
        // This is only for dummy use. We don't want to do ANYTHING!
        ctx = new Context();
        ctx.setSketch(this);
    }

    public Sketch(String path, Context c) throws IOException {
        this(new File(path), c);
    }

    public Sketch(File path, Context c) throws IOException {
        uuid = UUID.randomUUID().toString();
        ctx = c;
        ctx.setSketch(this);

        isUntitled = false;

        if(path == null) {
            path = createUntitledSketch();
        }

        sketchFolder = path;

        if(!path.exists()) {
            path.mkdirs();
            createBlankFile(path.getName() + ".ino");
        }

        File sketchPropertyFile = new File(sketchFolder, "sketch.properties");
        if (sketchPropertyFile.exists()) {
            settings = new PropertyFile(sketchPropertyFile);
        } else {
            settings = new PropertyFile();
        }

        String fn = path.getName().toLowerCase();

        if(fn.endsWith(".ino") || fn.endsWith(".pde")) {
            String pathName = path.getName();

            File oldPath = path;

            String inoName = pathName.substring(0, pathName.length() - 4);
            path = path.getParentFile();
            if (!path.getName().equals(inoName)) {
                File nsf = new File(path, path.getName() + ".ino");
                if (!nsf.exists()) {
                    nsf = new File(path, path.getName() + ".pde");
                }
            }
        }

        sketchFolder = path;
        sketchName = sketchFolder.getName();

        loadSketchFromFolder(path);
        buildFolder = createBuildFolder();

    }

    /**************************************************************************
     * OBJECTS                                                                *
     *                                                                        *
     * Internal object management.  All the routines to deal with boards,     *
     * cores, compilers, etc, and what happens when you switch from one to    *
     * another.                                                               *
     **************************************************************************/

    public void setSettings() {
        ctx.set("sketch.name", getName());
        ctx.set("sketch.path", getFolder().getAbsolutePath());
    }

    /**************************************************************************
     * FILE HANDLING                                                          *
     *                                                                        *
     * These are all routines to deal with files - file reading, writing,     *
     * copying, etc.                                                          *
     **************************************************************************/

    public void createNewLibrary(String libname) {
        try {
            libname = libname.replaceAll(" ", "_");
            File libs = new File(sketchFolder, "libraries");
            libs.mkdirs();
            File lib = new File(libs, libname);
            lib.mkdirs();
            File header = new File(lib, libname + ".h");
            File code = new File(lib, libname + ".cpp");

            PrintWriter out = new PrintWriter(header);
            String capname = libname.toUpperCase();
            out.println("#ifndef _" + capname + "_H");
            out.println("#define _" + capname + "_H");
            out.println("");
            out.println("#if (ARDUINO >= 100)");
            out.println("# include <Arduino.h>");
            out.println("#else");
            out.println("# include <WProgram.h>");
            out.println("#endif");
            out.println("");
            out.println("class " + libname + " {");
            out.println("\tprivate:");
            out.println("\t\t// Private functions and variables here");
            out.println("\tpublic:");
            out.println("\t\t" + libname + "();");
            out.println("\t\tvoid begin();");
            out.println("};");
            out.println("");
            out.println("#endif");
            out.close();

            out = new PrintWriter(code);
            out.println("#include <" + libname + ".h>");
            out.println("");
            out.println(libname + "::" + libname + "() {");
            out.println("\t// Constructor code here");
            out.println("}");
            out.println("");
            out.println("void " + libname + "::begin() {");
            out.println("\t// Initialization code here");
            out.println("}");
            out.close();

        } catch(Exception e) {
            Base.error(e);
        }
    }

    public void createNewFile(String filename) throws IOException {
        createNewFile(filename, null);
    }

    public void createNewFile(String filename, String content) throws IOException {
        SketchFile f = new SketchFile(ctx, this, new File(sketchFolder, filename));
        f.setFileData(content);
        f.saveDataToDisk();
        sketchFiles.put(filename, f);
    }

    public File createBlankFile(String fileName) {
        File f = new File(sketchFolder, fileName);

        if(f.exists()) {
            return f;
        }

        try {
            f.createNewFile();
        } catch(Exception e) {
        }

        return f;
    }

    public void loadSketchFromFolder(File folder) throws IOException {
        sketchFolder = folder;

        if(!isUntitled()) {
            Base.updateMRU(sketchFolder);
        }

        File fileList[] = sketchFolder.listFiles();
        if (fileList != null) {

            Arrays.sort(fileList);

            for(File f : fileList) {
                switch(FileType.getType(f)) {
                case FileType.SKETCH:
                case FileType.CSOURCE:
                case FileType.CPPSOURCE:
                case FileType.ASMSOURCE:
                case FileType.HEADER:
                    loadFile(f);
                }
            }
        }

        ctx.action("SetBoard", Preferences.get("board.recent"));

        updateSketchConfig();

        updateLibraryList();


        loadConfig();
    }

    public boolean loadFile(File f) throws IOException {
        sketchFiles.put(f.getName(), new SketchFile(ctx, this, f));
        return true;
    }

    public SketchFile getFileByName(String filename) {
        return sketchFiles.get(filename);
    }

    public File getBuildFileByName(String filename) {
        return new File(buildFolder, filename);
    }

    public File getFilesystemFolder() {
        return new File(getFolder(), "files");
    }

    public TreeMap<String, SketchFile> getSketchFiles() {
        return sketchFiles;
    }

    public String[] getFileNames() {
        return sketchFiles.keySet().toArray(new String[0]);
    }

    public File createBuildFolder() {
        if(Preferences.getBoolean("compiler.buildinsketch") || Base.cli.isSet("force-local-build") || Base.cli.isSet("cli")) {
            if(!parentIsProtected()) {
                File f = new File(sketchFolder, "build");

                if(!f.exists()) {
                    f.mkdirs();
                }

                return f;
            }
        }

        String name = "build-" + uuid;
        Debug.message("Creating build folder " + name);
        File f = new File(Base.getTmpDir(), name);

        if(!f.exists()) {
            f.mkdirs();
        }

        f.deleteOnExit();
        return f;
    }

    public File createUntitledSketch() {
        int num = 0;
        isUntitled = true;
        File f = null;

        do {
            num++;
            String name = "untitled" + Integer.toString(num);
            f = new File(Base.getTmpDir(), name);
        } while(f.exists());

        f.deleteOnExit();
        return f;
    }

    public SketchFile getMainFile() {
        for (SketchFile f : sketchFiles.values()) {
            if (f.isMainFile()) return f;
        }
        return null;
    }

    public TreeSet<SketchFile> getModifiedFiles() {
        ctx.triggerEvent("fileDataRead");
        TreeSet<SketchFile> list = new TreeSet<SketchFile>();
        for (SketchFile f : sketchFiles.values()) {
            if (f.isModified()) {
                list.add(f);
            }
        }

        return list;
    }

    public boolean isModified() {
        ctx.triggerEvent("fileDataRead");
        for (SketchFile f : sketchFiles.values()) {
            if (f.isModified()) return true;
        }
        return false;
    }

    static public boolean isSanitaryName(String name) {
        return sanitizeName(name).equals(name);
    }

    static public String sanitizeName(String origName) {
        char c[] = origName.toCharArray();
        StringBuilder buffer = new StringBuilder();

        for(int i = 0; i < c.length; i++) {
            if(((c[i] >= '0') && (c[i] <= '9')) ||
                    ((c[i] >= 'a') && (c[i] <= 'z')) ||
                    ((c[i] >= 'A') && (c[i] <= 'Z'))) {
                buffer.append(c[i]);
            } else {
                buffer.append('_');
            }
        }

        if(buffer.length() > 63) {
            buffer.setLength(63);
        }

        return buffer.toString();
    }

    public File getFolder() {
        return sketchFolder;
    }

    TreeMap<SketchFile, String> cleanedFiles;

    public boolean cleanFiles() {
        cleanedFiles = new TreeMap<SketchFile, String>();

        for(SketchFile f : sketchFiles.values()) {
            if(f.getType() == FileType.SKETCH) {
                String data = f.getFileData();
                String cleanData = stripComments(data);
                String[] lines = cleanData.split("\n");
                int lineno = 1;
                StringBuilder out = new StringBuilder();

                for(String line : lines) {
                    line = line.trim();
                    out.append(line + "\n");
                }

                cleanedFiles.put(f, out.toString());
            } else {
                cleanedFiles.put(f, f.getFileData());
            }
        }

        return true;
    }

    public int linesInString(String l) {
        return l.split("\n").length;
    }

    // Locate a library by its header file.  Scan through all the known
    // libraries for the right file.  First pass looks for libraries named
    // after the included file.  Second pass looks through all the
    // libraries for that include file.
    public Library findLibrary(String filename) {
        int dot = filename.lastIndexOf(".");

        String trimmedName = filename;

        if(dot >= 0) {
            trimmedName = filename.substring(0, dot);
        }

        // If the include file is part of the sketch, then we're not
        // interested in it.

        if(getFileByName(filename) != null) {
            return null;
        }


        // First look in the sketch libs folder if it exists

        File libfolder = getLibrariesFolder();

        if(libfolder.exists() && libfolder.isDirectory()) {
            File[] files = libfolder.listFiles();

            for(File f : files) {
                if(f.getName().equals(trimmedName)) {
                    File testFile = new File(f, filename);

                    if(f.exists()) {
                        Library ne = new Library(testFile, "sketch", "all");
                        ne.rescan();
                        return ne;
                    }
                }
            }
        }

        if (ctx.getCore() == null) {
            return null;
        }
        Library lib = Library.getLibraryByInclude(filename, ctx.getCore().getName());
        if (lib == null) {
            if (ctx.getCore().get("core.alias") != null) {
                String[] aliases = ctx.parseString(ctx.getCore().get("core.alias")).split("::");
                for (String alias : aliases) {
                    lib = Library.getLibraryByInclude(filename, alias);
                    if (lib != null) {
                        return lib;
                    }
                }
            }
        }
        return lib;
    }

    public HashMap<Integer, String> findLabels(String data) {
        HashMap<Integer, String>labels = new HashMap<Integer, String>();

        if(data == null) return labels;

        if(data.equals("")) return labels;

        String[] lines = data.split("\n");
        ArrayList<String>ents = new ArrayList<String>();

        // First find any entries.
        Pattern epat = Pattern.compile("\\.ent\\s+([^\\s]+)");

        for(String line : lines) {
            Matcher m = epat.matcher(line);

            if(m.find()) {
                ents.add(m.group(1));
            }
        }

        // Now match them to labels.
        int lineno = 0;

        for(String line : lines) {
            lineno++;
            line = line.trim();

            if(!line.contains(":")) {
                continue;
            }

            line = line.substring(0, line.indexOf(":"));

            if(ents.indexOf(line) > -1) {
                labels.put(lineno - 1, line);
            }
        }

        return labels;
    }

    public static final int LIB_PENDING = 0;
    public static final int LIB_PROCESSED = 1;
    public static final int LIB_SYSTEM = 2;

    ArrayList<String> includeOrder = new ArrayList<String>();

    public synchronized void updateLibraryList() {
        PropertyFile props = ctx.getMerged();
        cleanFiles();

        importedLibraries = new HashMap<String, Library>();
        unknownLibraries = new ArrayList<String>();
        includeOrder = new ArrayList<String>();
        Pattern inc = Pattern.compile("^#\\s*include\\s+[<\"](.*)[>\"]");

        for(SketchFile f : cleanedFiles.keySet()) {
            try {
                String data = cleanedFiles.get(f);
                String fname = f.toString();
                if(f.getType() == FileType.SKETCH) {
                    String ext = ctx.parseString(props.get("build.extension"));
                    if (ext == null) {
                        ext = "cpp";
                    }
                    fname = "deps-temp." + ext;
                } else {
                    String[] bits = fname.split("\\.");
                    fname = "deps-temp." + bits[bits.length-1];
                }

                File tempFile = new File(getBuildFolder(), fname);
                PrintWriter pw = new PrintWriter(tempFile);
                pw.print(data);
                pw.close();
                
                boolean haveHunted = huntForLibraries(tempFile, importedLibraries, unknownLibraries);

                Base.tryDelete(tempFile);

                String lines[] = data.split("\n");

                for(String line : lines) {
                    Matcher match = inc.matcher(line.trim());

                    if (match.find()) {
                        if (!haveHunted) {
                            Library lib = findLibrary(match.group(1).trim());
                            if (lib != null) {
                                importedLibraries.put(lib.getMainInclude(), lib);
                            } else {
                                if(unknownLibraries.indexOf(match.group(1).trim()) == -1) {
                                    unknownLibraries.add(match.group(1).trim());
                                }
                            }
                        }
                        if(includeOrder.indexOf(match.group(1)) == -1) {
                            includeOrder.add(match.group(1));
                        }
                    }
                }
            } catch (Exception e) {
            }
        }

        orderedLibraries = new ArrayList<Library>();
        for (String inclib : includeOrder) {
            if (importedLibraries.get(inclib) != null) {
                orderedLibraries.add(importedLibraries.get(inclib));
            }
        }

    }

    public String getReturnTypeFromProtoAndName(String proto, String name) {
        if (proto.startsWith("/^")) {
            String trimmed = proto.substring(2).trim();
            trimmed.replaceAll("\\s+", " ");
            int nameLoc = trimmed.indexOf(name);
            if (nameLoc == -1) return "";
            return trimmed.substring(0, nameLoc);
        }
        return "";
    }

    public String getReturnTypeFromProtoAndSignature(String proto, String signature) {
        if (signature == null) return "";
        proto = proto.replaceAll("\\s+"," ");
        signature = signature.replaceAll("\\s+"," ");

        if (proto.startsWith("/^")) {
            proto = proto.substring(2).trim();
        }
        if (proto.endsWith("$/;\"")) {
            proto = proto.substring(0, proto.length() - 4);
        }
        proto = proto.trim();
        int sigpos = proto.indexOf(signature);
        if (sigpos > 0) {
            String front = proto.substring(0, sigpos);
            String[] parts = front.split("\\s+");
            String out = "";
            for (int i = 0; i < parts.length - 1; i++) {
                if (i > 0) out += " ";
                out += parts[i];
            }

            if (parts[parts.length-1].startsWith("*")) {
                out += "*";
            }
            return out;
        }
        return "";
    }

    public File translateBuildFileToSketchFile(String filename) {
        File bf = getBuildFolder();
        File skf = getFolder();
        String bfname = bf.getAbsolutePath();
        if (filename.startsWith(bfname)) {
            int offset = bfname.length();
            return new File(skf, filename.substring(offset));
        }
        return null;
    }

    ArrayList<File> filesToCompile = null;

    public boolean prepare() throws IOException {
        PropertyFile props = ctx.getMerged();

        if(ctx.getBoard() == null) {
            ctx.error(Base.i18n.string("err.noboard"));
            return false;
        }

        if(ctx.getCore() == null) {
            ctx.error(Base.i18n.string("err.nocore"));
            return false;
        }

        if(Preferences.getBoolean("compiler.purge")) {
            ctx.action("purge");
        }

        filesToCompile = new ArrayList<File>();

        ctx.triggerEvent("fileDataRead");

        for (SketchFile f : sketchFiles.values()) {
            switch (f.getType()) {
                case FileType.ASMSOURCE:
                case FileType.CSOURCE:
                case FileType.CPPSOURCE: { // Save to build area and compile
                        File bff = new File(buildFolder, f.toString());
                        f.saveDataToDisk(bff);
                        filesToCompile.add(bff);
                    } break;
                case FileType.HEADER: { // Save to build area but don't compile
                        File bff = new File(buildFolder, f.toString());
                        f.saveDataToDisk(bff);
                    } break;
                case FileType.SKETCH: // Don't do anything
                    break;
            }
        }

        updateSketchConfig();
        updateLibraryList();

        if (Preferences.getBoolean("compiler.generate_makefile")) {
            if (props.get("makefile.template") != null) {
                generateMakefile();
            }
        }

        ctx.bullet("Converting binary files");
        HashMap<File, String[]> binFiles = convertBinaryFiles();

        if (binFiles.size() > 0) {
            filesToCompile.addAll(binFiles.keySet());
            PrintWriter bh = new PrintWriter(new File(buildFolder, "binary/binaries.h"));
            bh.println("#ifndef _UECIDE_BINARY_BINARIES_H");
            bh.println("#define _UECIDE_BINARY_BINARIES_H");
            bh.println();
            for (String[] lines : binFiles.values()) {
                for (String line : lines) {
                    bh.println(line);
                }
                bh.println();
            }
            bh.println("#endif");
            bh.close();
        }

        // Find all the function prototypes in sketch files
        ArrayList<FunctionBookmark> protos = new ArrayList<FunctionBookmark>();

        int lineno = Integer.MAX_VALUE;
        int thisEnd = 0;
        int lastEnd = 0;

        for (SketchFile f : sketchFiles.values()) {
            switch (f.getType()) {
                case FileType.CSOURCE:
                case FileType.CPPSOURCE:
                case FileType.ASMSOURCE:
                case FileType.HEADER:
                    break;
                case FileType.SKETCH: {
                    ArrayList<FunctionBookmark> bms = f.scanForFunctions();
                    for (FunctionBookmark bm : bms) {
                        if (bm.isFunction()) {
                            protos.add(bm);
                            if (f.isMainFile()) {
                                if (bm.getLine() < lineno) {
                                    lineno = bm.getLine();
                                }
                            }
                        }
                    }
                    if (f.isMainFile()) {
                        for (FunctionBookmark bm : bms) {
                            if (thisEnd > lastEnd && thisEnd < lineno) {
                                lastEnd = thisEnd;
                            }
                            thisEnd = bm.getEnd();
                        }
                    }
                } break;
            }
        }

        int protoLocation = lastEnd + 1;

        // Save these prototypes into a header file.
        File out = new File(buildFolder, getName() + "_proto.h");
        PrintWriter pw = new PrintWriter(out);
        pw.println("#ifndef _UECIDE_FUNCTION_PROTOTYPES");
        pw.println("#define _UECIDE_FUNCTION_PROTOTYPES");
        pw.println();
        pw.println("// This should be inserted at line " + protoLocation);
        pw.println();
        for (FunctionBookmark p : protos) {
            pw.println(p + ";");
        }
        pw.println();

        pw.println("#endif");
        pw.close();

        String ext = ctx.parseString(props.get("build.extension"));
        if (ext == null) {
            ext = "cpp";
        }
        File masterSketchFile = new File(buildFolder, getName() + "_combined." + ext);
        filesToCompile.add(masterSketchFile);
        pw = new PrintWriter(masterSketchFile);

        String hdr = props.get("core.header");
        if (hdr != null) {
            pw.println("#include <" + hdr + ">");
            if (binFiles.size() > 0) {
                pw.println("#include <binary/binaries.h>");
            }
        }
        pw.println("#line 1 \"" + getMainFile().getFile().getAbsolutePath().replaceAll("\\\\", "\\\\\\\\") + "\"");

        int thisLine = 1;
        String data = getMainFile().getFileData();
        String lines[] = data.split("\n");
        for (String line : lines) {
            if (thisLine == protoLocation) {
                pw.println("#include <" + getName() + "_proto.h>");
                pw.println("#line " + thisLine + " \"" + getMainFile().getFile().getAbsolutePath().replaceAll("\\\\", "\\\\\\\\") + "\"");
            }
            pw.println(line);
            thisLine++;
        }

        for (SketchFile f : sketchFiles.values()) {
            if (f.getType() == FileType.SKETCH) {
                if (!f.isMainFile()) {
                    pw.println("#line 1 \"" + f.getFile().getAbsolutePath().replaceAll("\\\\", "\\\\\\\\") + "\"");
                    pw.print(f.getFileData());
                }
            }
        }
        pw.close();
        return true;
    }

    public String stripComments(String data) {
        int cpos = 0;
        boolean inString = false;
        boolean inEscape = false;
        boolean inMultiComment = false;
        boolean inSingleComment = false;

        // We'll work through the string a character at a time pushing it on to the
        // string builder if we want it, or pushing a space if we don't.

        StringBuilder out = new StringBuilder();

        while (cpos < data.length()) {
            char thisChar = data.charAt(cpos);
            char nextChar = ' ';
            if (cpos < data.length() - 1) {
                nextChar = data.charAt(cpos + 1);
            }

            // Don't process any escaped characters - just add them verbatim.
            if (thisChar == '\\') {
                if (!inSingleComment && !inMultiComment)
                    out.append(thisChar);
                cpos++;
                if (cpos < data.length()) {
                    if (!inSingleComment && !inMultiComment)
                        out.append(data.charAt(cpos));
                    cpos++;
                }
                continue;
            }

            // If we're currently in a string then keep moving on until the end of the string.
            // If we hit the closing quote we still want to move on since it'll start a new
            // string otherwise.
            if (inString) {
                out.append(thisChar);
                if (thisChar == '"') {
                    inString = false;
                }
                cpos++;
                continue;
            }
            
            // If we're in a single line comment then keep skipping until we hit the end of the line.
            if (inSingleComment) {
                if (thisChar == '\n') {
                    out.append(thisChar);
                    inSingleComment = false;
                    cpos++;
                    continue;
                }
                cpos++;
                continue;
            }

            // If we're in a multi-line comment then keep skipping until we
            // hit the end of comment sequence.  Preserve newlines.
            if (inMultiComment) {
                if  (thisChar == '*' && nextChar == '/') {
                    inMultiComment = false;
                    cpos++;
                    cpos++;
                    continue;
                }
                if (thisChar == '\n') {
                    out.append(thisChar);
                    cpos++;
                    continue;
                }
                cpos++;
                continue;
            }

            // Is this the start of a quote?
            if (thisChar == '"') {
                out.append(thisChar);
                cpos++;
                inString = true;
                continue;
            }

            // How about the start of a single line comment?
            if (thisChar == '/' && nextChar == '/') {
                inSingleComment = true;
                out.append(" ");
                cpos++;
                continue;
            }
  
            // The start of a muti-line comment?
            if (thisChar == '/' && nextChar == '*') {
                inMultiComment = true;
                out.append(" ");
                cpos++;
                continue;
            }

            // None of those? Then let's just append.
            out.append(thisChar);
            cpos++;
        }
                
        return out.toString();
    }

    public boolean upload() {
        ctx.executeKey("upload.precmd");
        boolean ret = programFile(ctx.getProgrammer(), sketchName);
        ctx.executeKey("upload.postcmd");
        return ret;
    }

    public boolean performSerialReset(boolean dtr, boolean rts, int speed, int predelay, int delay, int postdelay) {
        if (!Base.isQuiet()) ctx.bullet("Resetting board.");
        try {
            CommunicationPort port = ctx.getDevice();
            if (port instanceof SerialCommunicationPort) {
                SerialCommunicationPort sport = (SerialCommunicationPort)port;
                if (!sport.openPort()) {
                    ctx.error("Error: " + sport.getLastError());
                    return false;
                }
                sport.setDTR(false);
                sport.setRTS(false);
                Thread.sleep(predelay);
                sport.setDTR(dtr);
                sport.setRTS(rts);
                Thread.sleep(delay);
                sport.setDTR(false);
                sport.setRTS(false);
                sport.closePort();
                Thread.sleep(postdelay);
            }
        } catch (Exception e) {
            ctx.error(e);
            return false;
        }
        return true;
    }

    public boolean performBaudBasedReset(int b, int predelay, int delay, int postdelay) {
        if (!Base.isQuiet()) ctx.bullet("Resetting board.");
        try {
            CommunicationPort port = ctx.getDevice();
            if (port instanceof SerialCommunicationPort) {
                SerialCommunicationPort sport = (SerialCommunicationPort)port;
                if (!sport.openPort()) {
                    ctx.error("Error: " + sport.getLastError());
                    return false;
                }
                sport.setDTR(false);
                sport.setRTS(false);
                Thread.sleep(predelay);
                sport.setDTR(true);
                sport.setRTS(true);
                if (!sport.setSpeed(b)) {
                    ctx.error("Error: " + sport.getLastError());
                }
                Thread.sleep(delay);
                sport.setDTR(false);
                sport.setRTS(false);
                sport.closePort();
                Thread.sleep(postdelay);
            }
        } catch (Exception e) {
            ctx.error(e);
            return false;
        }
        return true;
    }

    public boolean build() throws IOException {

        ctx.triggerEvent("buildStart");

        checkForSettings();

        terminateExecution = false;

        if(ctx.getBoard() == null) {
            ctx.error(Base.i18n.string("err.noboard"));
            ctx.triggerEvent("buildFail");
            return false;
        }

        if(ctx.getCore() == null) {
            ctx.error(Base.i18n.string("err.nocore"));
            ctx.triggerEvent("buildFail");
            return false;
        }

        if(ctx.getCompiler() == null) {
            ctx.error(Base.i18n.string("err.nocompiler"));
            ctx.triggerEvent("buildFail");
            return false;
        }

        if (!Base.isQuiet()) ctx.heading(Base.i18n.string("msg.compiling"));

        if (!Base.isQuiet()) ctx.bullet(Base.i18n.string("msg.preprocessing"));
        try {
            if(!prepare()) {
                ctx.error(Base.i18n.string("err.compiling.failed"));
                ctx.triggerEvent("buildFail");
                return false;
            }
        } catch(Exception e) {
            ctx.error(e);
            ctx.triggerEvent("buildFail");
            return false;
        }
    
        try {
            boolean done = compile();
            if (done) {
                ctx.triggerEvent("buildFinished");
            } else {
                ctx.triggerEvent("buildFail");
            }
            return done;
        } catch (IOException ex) {
            ctx.error(ex);
            ctx.triggerEvent("buildFail");
            return false;
        }
    }

    public boolean isSketchFile(File f) throws IOException {
        for (SketchFile sf : sketchFiles.values()) {
            if (f.getCanonicalFile().equals(sf.getFile().getCanonicalFile())) {
                return true;
            }
        }
        return false;
    }

    public boolean saveAs(File newPath) throws IOException {
        if(newPath.exists()) {
            boolean overwrite = ctx.getGui().askYesNo("Overwrite " + newPath.getName() + "?");
            if (!overwrite) return false;
            Base.removeDescendants(newPath);
        }

        Debug.message("Save as " + newPath.getAbsolutePath());
        // 1. Make the folder to store the files in
        newPath.mkdirs();


        // 2. Copy any data that isn't a sketch file
        File[] files = sketchFolder.listFiles();

        for (File f : files) {
            // Skip any sketch files
            if (isSketchFile(f)) continue;

            // Make a file to point to the destination
            File dest = new File(newPath, f.getName());

            // Copy any folders ...
            if (f.isDirectory()) {
                // ... except the build folder
                if (f != buildFolder) {
                    Utils.copyDir(f, dest);
                }
                continue;
            }

            // And copy any plain files.
            Files.copy(f.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        // 3. Dump any file data from the sketch files into new files.

        for (SketchFile sf : sketchFiles.values()) {
            // New file destination, renaming the main file according to the folder name
            File dest = new File(newPath, sf.isMainFile() ? newPath.getName() + ".ino" : sf.getFile().getName());
            sf.saveDataToDisk(dest);
        }

        // 4. Load the newly created sketch into a new Sketch object and apply it to the context.
        Sketch newlyCreatedSketch = new Sketch(newPath, ctx);
        ctx.setSketch(newlyCreatedSketch);

        // 5. Tell anyone who cares that the sketch object has changed.
        ctx.triggerEvent("sketchLoaded");
        return true;
    }

    public boolean save() throws IOException {
        if (isUntitled()) {
            return false;
        }

        if (parentIsProtected()) {
            return false;
        }

        // TODO: Implement backup versions

        for (SketchFile sf : sketchFiles.values()) {
            sf.saveDataToDisk();
        }
        return true;
    }

    public String getName() {
        return sketchName;
    }

    public Collection<Library> getImportedLibraries() {
        return importedLibraries.values();
    }

    public ArrayList<Library> getOrderedLibraries() {
        
        return orderedLibraries;
    }

    public ArrayList<String> getIncludePaths() {
        ArrayList<String> libFiles = new ArrayList<String>();

        PropertyFile props = ctx.getMerged();

        libFiles.add(buildFolder.getAbsolutePath());
        libFiles.add(ctx.getBoard().getFolder().getAbsolutePath());
        libFiles.add(ctx.getCore().getLibrariesFolder().getAbsolutePath());

        for(String key : props.childKeysOf("compiler.library")) {
            String coreLibName = key.substring(17);
            String libPaths = ctx.parseString(props.get(key));

            if(libPaths != null && !(libPaths.trim().equals(""))) {
                String[] libPathsArray = libPaths.split("::");

                for(String p : libPathsArray) {
                    File f = new File(ctx.getCompiler().getFolder(), p);

                    if(f.exists()) {
                        libFiles.add(f.getAbsolutePath());
                    }
                }
            }
        }

        for(String key : props.childKeysOf("core.library")) {
            String coreLibName = key.substring(13);
            String libPaths = ctx.parseString(props.get(key));

            if(libPaths != null && !(libPaths.trim().equals(""))) {
                String[] libPathsArray = libPaths.split("::");

                for(String p : libPathsArray) {
                    File f = new File(ctx.getCore().getFolder(), p);

                    if(f.exists()) {
                        libFiles.add(f.getAbsolutePath());
                    }
                }
            }
        }

        for(String key : props.childKeysOf("board.library")) {
            String coreLibName = key.substring(14);
            String libPaths = ctx.parseString(props.get(key));

            if(libPaths != null && !(libPaths.trim().equals(""))) {
                String[] libPathsArray = libPaths.split("::");

                for(String p : libPathsArray) {
                    File f = new File(ctx.getBoard().getFolder(), p);

                    if(f.exists()) {
                        libFiles.add(f.getAbsolutePath());
                    }
                }
            }
        }

        for(Library l : getOrderedLibraries()) {
            libFiles.add(l.getSourceFolder().getAbsolutePath());
        }

        return libFiles;
    }

    public void checkForSettings() throws IOException {
        SketchFile mainFile = getMainFile();

        Pattern param = Pattern.compile("^#pragma\\s+parameter\\s+([^\\s=]+)\\s*=\\s*(.*)$");
        Pattern option = Pattern.compile("^#pragma\\s+option\\s+([^\\s=]+)\\s*=\\s*(.*)$");
        Pattern pcompiler = Pattern.compile("^#pragma\\s+compiler\\s+(.*)$");
        Pattern pcore = Pattern.compile("^#pragma\\s+core\\s+(.*)$");
        Pattern pboard = Pattern.compile("^#pragma\\s+board\\s+(.*)$");
        Pattern pport = Pattern.compile("^#pragma\\s+port\\s+(.*)$");

        String[] data = mainFile.getFileData().split("\n");


        for(String line : data) {
            Matcher m = pcompiler.matcher(line);
            if (m.find()) {
                ctx.action("SetCompiler", m.group(1));
            }
            m = pcore.matcher(line);
            if (m.find()) {
                ctx.action("SetCore", m.group(1));
            }
            m = pboard.matcher(line);
            if (m.find()) {
                ctx.action("SetBoard", m.group(1));
            }
            m = pport.matcher(line);
            if (m.find()) {
                ctx.action("SetDevice", m.group(1));
            }
        }

        for(String line : data) {
            line = line.trim();
            Matcher m = param.matcher(line);

            if(m.find()) {
                String key = m.group(1);
                String value = m.group(2);

                ctx.set(key, value);
            }

        }

        boolean updateMenu = false;
        for(String line : data) {
            line = line.trim();
            Matcher m = option.matcher(line);

            if(m.find()) {
                String key = m.group(1);
                String value = m.group(2);

                String oldOption = getOption(key);
                if (oldOption != null) {
                    if (!oldOption.equals(value)) {
                        setOption(key, value);
                        updateMenu = true;
                    }
                }
            }

        }

    }

    public File getBuildFolder() {
        return buildFolder;
    }

    public String getBuildPath() {
        return buildFolder.getAbsolutePath();
    }

    public void cleanup() {
        Base.removeDescendants(buildFolder);
    }

/* TODO: REWRITE THIS FUNCTION */
    public boolean isReadOnly() {
        return false;
    }

    public void needPurge() {
        doPrePurge = true;
    }

    public Library getSketchLibrary(String lib) {
        if (lib.endsWith(".h")) {
            lib = lib.substring(0, lib.lastIndexOf("."));
        }

        File libs = new File(sketchFolder, "libraries");
        if (!libs.exists()) {
            return null;
        }

        if (!libs.isDirectory()) {
            return null;
        }

        File libDir = new File(libs, lib);
        if (!libDir.exists()) {
            return null;
        }
        if (!libDir.isDirectory()) {
            return null;
        }
        Library libob = new Library(libDir, "sketch", "all");
        if (!libob.isValid()) {
            return null;
        }
        return libob;
    }

    public String generateIncludes() {
        updateLibraryList();
        ArrayList<File> includes = new ArrayList<File>();

        TreeMap<String, ArrayList<File>> coreLibs = getCoreLibs();

        for(String lib : coreLibs.keySet()) {
            ArrayList<File> libfiles = coreLibs.get(lib);
            includes.addAll(libfiles);
        }

        for (Library l : orderedLibraries) {
            if(includes.indexOf(l.getSourceFolder()) < 0) {
                includes.add(l.getSourceFolder());
            }
        }

        for (Library l : importedLibraries.values()) {
            if (includes.indexOf(l.getSourceFolder()) < 0) {
                includes.add(l.getSourceFolder());
            }
        }

        includes.add(ctx.getBoard().getFolder());
        includes.add(buildFolder);
        includes.add(sketchFolder);

        String includeList = "";

        for(File f : includes) {
            if (f == null) {
                continue;
            }
            String path = f.getAbsolutePath();

            if(!(includeList.equals(""))) {
                includeList += "::";
            }

            includeList += "-I" + path;
        }

        return includeList;
    }

    public TreeMap<String, ArrayList<File>> getCoreLibs() {
        PropertyFile props = ctx.getMerged();
        TreeMap<String, ArrayList<File>> libs = new TreeMap<String, ArrayList<File>>();

        for(String coreLibName : props.childKeysOf("compiler.library")) {
            ArrayList<File> files = new ArrayList<File>();
            String libPaths = ctx.parseString(props.get("compiler.library." + coreLibName));

            if(libPaths != null && !(libPaths.trim().equals(""))) {
                libPaths = ctx.parseString(libPaths);
                String[] libPathsArray = libPaths.split("::");

                for(String p : libPathsArray) {
                    File f = new File(ctx.getCompiler().getFolder(), p);

                    if(f.exists() && f.isDirectory()) {
                        files.add(f);
                    }
                }

                libs.put(coreLibName, files);
            }
        }

        for(String coreLibName : props.childKeysOf("core.library")) {
            ArrayList<File> files = new ArrayList<File>();
            String libPaths = ctx.parseString(props.get("core.library." + coreLibName));

            if(libPaths != null && !(libPaths.trim().equals(""))) {
                libPaths = ctx.parseString(libPaths);
                String[] libPathsArray = libPaths.split("::");

                for(String p : libPathsArray) {
                    File f = new File(ctx.getCore().getFolder(), p);

                    if(f.exists() && f.isDirectory()) {
                        files.add(f);
                    }
                }

                libs.put(coreLibName, files);
            }
        }

        for(String coreLibName : props.childKeysOf("board.library")) {
            ArrayList<File> files = new ArrayList<File>();
            String libPaths = ctx.parseString(props.get("board.library." + coreLibName));

            if(libPaths != null && !(libPaths.trim().equals(""))) {
                libPaths = ctx.parseString(libPaths);
                String[] libPathsArray = libPaths.split("::");

                for(String p : libPathsArray) {
                    File f = new File(ctx.getBoard().getFolder(), p);

                    if(f.exists() && f.isDirectory()) {
                        files.add(f);
                    }
                }

                libs.put(coreLibName, files);
            }
        }

        return libs;
    }

    public void installLibrary(APT apt, Package pkg, boolean recurse) {
        pkg = apt.getPackage(pkg.getName());

        if (recurse) {
            Package[] deps = apt.resolveDepends(pkg);
            if (deps != null) {
                for (Package p : deps) {
                    if (!apt.isInstalled(p)) {
                        installLibrary(apt, p, false);
                    }
                }
            }
        }

        if (!apt.isInstalled(pkg)) {
            ctx.bullet2("Downloading " + pkg.getName());
            pkg.fetchPackage(Base.getDataFile("apt/cache"));
        
            ctx.bullet2("Installing " + pkg.getName());
            pkg.extractPackage(Base.getDataFile("apt/cache"), Base.getDataFile("apt/db/packages"), Base.getDataFolder());
        }
    }

    public boolean compile() throws IOException {

        
        long startTime = System.currentTimeMillis();

//        ctx.clearSettings();
//        checkForSettings();
        PropertyFile props = ctx.getMerged();

        ctx.set("cache.root", getCacheFolder().getAbsolutePath());
        clearLineComments();

        if (props.getBoolean("purge")) {
            doPrePurge = true;
        }

        ctx.executeKey("compile.precmd");

        // Rewritten compilation system from the ground up.

        // Step one, we need to build a generic set of includes:

        ctx.set("includes", generateIncludes());
        ctx.set("filename", sketchName);

        try {
            if ((Preferences.getBoolean("editor.dialog.missinglibs")) && (Base.isOnline())) {
            }
        } catch (Exception ex) { ctx.error(ex); }

        if(doPrePurge) {
            doPrePurge = false;
            Base.removeDir(getCacheFolder());
        }

        ctx.set("option.flags", getFlags("flags"));
        ctx.set("option.cflags", getFlags("cflags"));
        ctx.set("option.cppflags", getFlags("cppflags"));
        ctx.set("option.ldflags", getFlags("ldflags"));

        String libPaths = "";
        String libNames = "";

        for (Library lib : importedLibraries.values()) {
            if (!libPaths.equals("")) {
                libPaths += "::";
            }
            if (!libNames.equals("")) {
                libNames += "::";
            }
            libPaths += lib.getSourceFolder().getAbsolutePath();
            libNames += lib.getName();
            ctx.set("library." + lib.getName() + ".path", lib.getSourceFolder().getAbsolutePath());
        }

        ctx.set("library.paths", libPaths);
        ctx.set("library.names", libNames);

        ctx.set("build.path", buildFolder.getAbsolutePath());


        if (props.keyExists("compile.script")) {
            boolean ret = (Boolean)ctx.executeKey("compile.script");
            if (!ret) return false;
        }

        // Copy any specified files from the compiler, core or board folders
        // into the build folder.  This is especially good if the compiler
        // executable needs certain DLL files to be in the current working
        // directory (which is the build folder).

        String precopy = ctx.parseString(props.getPlatformSpecific("compile.precopy"));

        if(precopy != null) {
            Debug.message("Copying files...");
            String[] copyfiles = precopy.split("::");

            for(String cf : copyfiles) {
                Debug.message("  " + cf + "...");
                File src = new File(ctx.getCompiler().getFolder(), cf);

                if(!src.exists()) {
                    src = new File(ctx.getCore().getFolder(), cf);
                }

                if(!src.exists()) {
                    src = new File(ctx.getBoard().getFolder(), cf);
                }

                if(src.exists()) {
                    File dest = new File(buildFolder, src.getName());
                    Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    Debug.message("    ... ok");
                } else {
                    Debug.message("    ... not found");
                }
            }
        }

        if (!Base.isQuiet()) ctx.bullet("Compiling sketch...");
        ArrayList<File>sketchObjects = compileSketch();

        if(sketchObjects == null) {
            ctx.error(Base.i18n.string("err.compiling.failed"));
            return false;
        }

        if (!Base.isQuiet()) ctx.bullet(Base.i18n.string("msg.compiling.core"));

        if(!compileCore()) {
            ctx.error(Base.i18n.string("err.compiling.failed"));
            return false;
        }


        if (!Base.isQuiet()) ctx.bullet(Base.i18n.string("msg.compiling.libraries"));

        if(!compileLibraries()) {
            ctx.error(Base.i18n.string("err.compiling.failed"));
            return false;
        }


        if (!Base.isQuiet()) ctx.bullet(Base.i18n.string("msg.linking"));

        if(!compileLink(sketchObjects)) {
            ctx.error(Base.i18n.string("err.compiling.failed"));
            return false;
        }



        PropertyFile autogen = props.getChildren("compile.autogen");
        String[] types = autogen.childKeys();

        if (types.length > 0) {
            int steps = 50 / types.length;
            int pct = 50;

            for (String type : types) {
                ctx.bullet2(Base.i18n.string("msg.compiling.genfile", type));
                ctx.executeKey("compile.autogen." + type);
                pct += steps;
            }
        }

        if(Preferences.getBoolean("compiler.save_lss") && !parentIsProtected()) {
            try {
                File lss = new File(buildFolder, sketchName + ".lss");
                if (lss.exists()) {
                    Files.copy(new File(buildFolder, sketchName + ".lss").toPath(), new File(sketchFolder, sketchName + ".lss").toPath(), StandardCopyOption.REPLACE_EXISTING);

                }
            } catch(Exception e) {
                ctx.error(e);
            }
        }


        if((
            Preferences.getBoolean("compiler.save_hex") || Base.cli.isSet("force-save-hex") || Base.cli.isSet("cli")) 
            && !parentIsProtected()) {
            try {
                String exeSuffix = props.get("exe.extension");
                if (exeSuffix == null) {
                    exeSuffix = ".hex";
                }
                File dest = new File(sketchFolder, sketchName + exeSuffix);
                Files.copy(new File(buildFolder, sketchName + exeSuffix).toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                if (dest.exists()) {
                    dest.setExecutable(true);
                }
                

            } catch(Exception e) {
                ctx.error(e);
            }
        }


        if (!Base.isQuiet()) ctx.heading(Base.i18n.string("msg.compiling.done"));


        compileSize();

        long endTime = System.currentTimeMillis();
        double compileTime = (double)(endTime - startTime) / 1000d;
        if (!Base.isQuiet()) ctx.bullet(Base.i18n.string("msg.compiling.time", compileTime));
        ctx.executeKey("compile.postcmd");
        return true;
    }

    public boolean compileSize() {
        PropertyFile props = ctx.getMerged();

        if (props.get("compile.size") != null) {
            if (!Base.isQuiet()) ctx.heading(Base.i18n.string("msg.compiling.memory"));

            ctx.startBuffer();
            ctx.executeKey("compile.size");
            String output = ctx.endBuffer();

            String reg = props.get("compiler.size.regex", "^\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)");
            int tpos = props.getInteger("compiler.size.text", 1);
            int rpos = props.getInteger("compiler.size.rodata", 0);
            int dpos = props.getInteger("compiler.size.data", 2);
            int bpos = props.getInteger("compiler.size.bss", 3);
            String[] lines = output.split("\n");
            Pattern p = Pattern.compile(reg);
            int textSize = 0;
            int rodataSize = 0;
            int dataSize = 0;
            int bssSize = 0;
            for (String line : lines) {

                try {
                    Matcher m = p.matcher(line);
                    if (m.find()) {
                        if (tpos > 0) {
                            textSize = Integer.parseInt(m.group(tpos));
                        }
                        if (rpos > 0) {
                            rodataSize = Integer.parseInt(m.group(rpos));
                        }
                        if (dpos > 0) {
                            dataSize = Integer.parseInt(m.group(dpos));
                        }
                        if (bpos > 0) {
                            bssSize = Integer.parseInt(m.group(bpos));
                        }
                    }
                } catch (Exception e) {
                }
            }

            ctx.set("size.text", textSize + "");
            ctx.set("size.data", dataSize + "");
            ctx.set("size.rodata", rodataSize + "");
            ctx.set("size.bss", bssSize + "");

            ctx.set("size.flash", (textSize + dataSize + rodataSize) + "");
            ctx.set("size.ram", (bssSize + dataSize) + "");

            if (!Base.isQuiet()) {
                int max_ram = props.getInteger("memory.sram");
                int max_rom = props.getInteger("memory.flash");

                if (max_rom > 0) {
                    int romperc = (textSize + dataSize + rodataSize) * 100 / max_rom;
                    ctx.bullet(Base.i18n.string("msg.compiling.progsize.perc", (textSize + dataSize + rodataSize), romperc)); 
                } else {
                    ctx.bullet(Base.i18n.string("msg.compiling.progsize", (textSize + dataSize + rodataSize))); 
                }

                if (max_ram > 0) {
                    int ramperc = (bssSize + dataSize) * 100 / max_ram;
                    ctx.bullet(Base.i18n.string("msg.compiling.ramsize.perc", (bssSize + dataSize), ramperc)); 
                } else {
                    ctx.bullet(Base.i18n.string("msg.compiling.ramsize", (bssSize + dataSize))); 
                }
            }
        }
        return true;
    }

    class LibCompileThread extends Thread {
        public boolean compiled = false;
        public Library target = null;
        public LibCompileThread(Library tgt) {
            target = tgt;
        }
        public void run() {
            compiled = compileLibrary(target);
        }
    }

    public boolean compileLibraries() {
        boolean ok = true;
        ArrayList<LibCompileThread> threads = new ArrayList<LibCompileThread>();

        int maxThreads = 4;
        int currentThreads = 0;

        for(String lib : importedLibraries.keySet()) {

            while (currentThreads >= maxThreads) {
                for (LibCompileThread t : threads) {
                    try {
                        t.join(10);
                        if (t.getState() == Thread.State.TERMINATED) {
                            if (t.compiled == false) ok = false;
                            currentThreads--;
                            threads.remove(threads.indexOf(t));
                            break;
                        }
                    } catch (Exception e) {
                        ctx.error(e);
                    }
                }
            }

            LibCompileThread t = new LibCompileThread(importedLibraries.get(lib));
            t.start();
            threads.add(t);
            currentThreads++;

            try {
                Thread.sleep(10);
            } catch (Exception e) {
            }
        }

        for (LibCompileThread t : threads) {
            try {
                t.join();
                if (t.compiled == false) ok = false;
            } catch (Exception e) {
                ctx.error(e);
            }
        }

        return ok;
    }

    private File compileFile(Context localCtx, File src) {
        return compileFile(localCtx, src, buildFolder);
    }

    private File compileFile(Context localCtx, File src, File fileBuildFolder) {

    
        String fileName = src.getName();
        String recipe = null;

        if(terminateExecution) {
            terminateExecution = false;
            ctx.error("Compilation terminated");
            return null;
        }

        PropertyFile props = localCtx.getMerged();

        switch (FileType.getType(src)) {
            case FileType.CPPSOURCE:
                recipe = "compile.cpp";
                break;
            case FileType.CSOURCE:
                recipe = "compile.c";
                break;
            case FileType.ASMSOURCE:
                recipe = "compile.S";
                break;
        }

        if(recipe == null) {
            ctx.error(Base.i18n.string("err.badfile", fileName));
            return null;
        }

        if (Preferences.getBoolean("compiler.verbose_files")) {
            ctx.bullet3(fileName);
        }

        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        String objExt = localCtx.parseString(props.get("compiler.object","o"));

        String bfPath = fileBuildFolder.getAbsolutePath();
        String srcPath = src.getParentFile().getAbsolutePath();

        if (srcPath.startsWith(bfPath + "/")) {
            fileBuildFolder = src.getParentFile();
        }

        File dest = new File(fileBuildFolder, fileName + "." +objExt);

        if(dest.exists()) {
            if(dest.lastModified() > src.lastModified()) {
                return dest;
            }
        }

        localCtx.set("build.path", fileBuildFolder.getAbsolutePath());
        localCtx.set("source.name", src.getAbsolutePath());
        localCtx.set("object.name", dest.getAbsolutePath());

        localCtx.addDataStreamParser(new DataStreamParser() {
            public String parseStreamMessage(Context ctx, String m) {
                if (parseLineForWarningMessage(ctx, m)) {
                    return "";
                }
                return m;
            }
            public String parseStreamError(Context ctx, String m) {
                if (parseLineForErrorMessage(ctx, m)) {
                    return "";
                }
                if (parseLineForWarningMessage(ctx, m)) {
                    return "";
                }
                return m;
            }
        });

        String output = "";
        if(!(Boolean)localCtx.executeKey(recipe)) {
            localCtx.removeDataStreamParser();
            return null;
        }
        localCtx.removeDataStreamParser();

        if(!dest.exists()) {
            return null;
        }

        return dest;
    }

    public File getCacheFolder() {
        File cacheRoot = Base.getCacheFolder();
        Core c = ctx.getCore();
        File boardCache = new File(cacheRoot, "unknownCache");
        if (c != null) {
            Board b = ctx.getBoard();
            if (b != null) {
                File coreCache = new File(cacheRoot, c.getName());
                boardCache = new File(coreCache, b.getName());
            }
        }

        if(!boardCache.exists()) {
            boardCache.mkdirs();
        }

        return boardCache;
    }

    public File getCacheFile(String fileName) {
        File cacheFolder = getCacheFolder();
        File out = new File(cacheFolder, fileName);
        return out;
    }

    public boolean compileCore() throws IOException {
        TreeMap<String, ArrayList<File>> coreLibs = getCoreLibs();
        PropertyFile props = ctx.getMerged();

        if (props.get("compile.stub") != null) {
            String mainStub = ctx.parseString(props.get("compile.stub"));
            String[] bits = mainStub.split("::");
            for (String stubFile : bits) {
                File mainStubFile = new File(stubFile);
                if (mainStubFile.exists()) {
                    File mainStubObject = compileFile(ctx, mainStubFile);
                    File cachedStubObject = getCacheFile(mainStubObject.getName());
                    if (mainStubObject.exists()) {
                        Files.copy(mainStubObject.toPath(), cachedStubObject.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        Base.tryDelete(mainStubObject);
                    }
                }
            }
        }

        for(String lib : coreLibs.keySet()) {
            if (!Base.isQuiet()) ctx.bullet2(lib);

            if(!compileCore(coreLibs.get(lib), "Core_" + lib)) {
                return false;
            }
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    public boolean compileCore(ArrayList<File> core, String name) {
        PropertyFile props = ctx.getMerged();
        String prefix = ctx.parseString(props.get("compiler.library.prefix","lib"));
        String suffix = ctx.parseString(props.get("compiler.library", "a"));
        File archive = getCacheFile(prefix + name + "." + suffix);

        ctx.set("library", archive.getAbsolutePath());

        long archiveDate = 0;

        if(archive.exists()) {
            archiveDate = archive.lastModified();
        }

        File coreBuildFolder = new File(buildFolder, "libCore_" + name);
        coreBuildFolder.mkdirs();

        TreeSet<File> fileList = new TreeSet<File>(new CaseInsensitiveFileComparator());

        for(File f : core) {
            if(f.exists() && f.isDirectory()) {
                fileList.addAll(findFilesInFolder(f, "S", false));
                fileList.addAll(findFilesInFolder(f, "c", false));
                fileList.addAll(findFilesInFolder(f, "cpp", false));
                fileList.addAll(findFilesInFolder(f, "cxx", false));
                fileList.addAll(findFilesInFolder(f, "cc", false));
            }
        }

        for(File f : fileList) {
            if(f.lastModified() > archiveDate) {
                File out = compileFile(ctx, f, coreBuildFolder);

                if(out == null) {
                    Base.tryDelete(coreBuildFolder);
                    if (archive.exists()) Base.tryDelete(archive);
                    return false;
                }

                ctx.set("object.name", out.getAbsolutePath());
                boolean ok = (Boolean)ctx.executeKey("compile.ar");

                if(!ok) {
                    Base.tryDelete(out);
                    Base.tryDelete(coreBuildFolder);
                    if (archive.exists()) Base.tryDelete(archive);
                    return false;
                }

                Base.tryDelete(out);
            }
        }

        Base.tryDelete(coreBuildFolder);
        return true;
    }

    public void putToContext(String k, String v) {
        ctx.set(k, v);
    }

    public String getFromContext(String k) {
        return ctx.get(k);
    }

    public String getArchiveName(Library lib) {
        PropertyFile props = ctx.getMerged();
        String prefix = ctx.parseString(props.get("compiler.library.prefix","lib"));
        String suffix = ctx.parseString(props.get("compiler.library", "a"));
        return prefix + lib.getLinkName() + "." + suffix;
    }

    public boolean compileLibrary(Library lib) {
        Context localCtx = new Context(ctx);
        File archive = getCacheFile(getArchiveName(lib));  //getCacheFile("lib" + lib.getName() + ".a");
        File utility = lib.getUtilityFolder();
        PropertyFile props = localCtx.getMerged();
        if (!Base.isQuiet()) ctx.bullet2(lib.toString() + " [" + lib.getFolder().getAbsolutePath() + "]");

        localCtx.set("library", archive.getAbsolutePath());

        long archiveDate = 0;

        if(archive.exists()) {
            archiveDate = archive.lastModified();
        }

        File libBuildFolder = new File(buildFolder, "lib" + lib.getLinkName());
        libBuildFolder.mkdirs();
        if (!libBuildFolder.exists()) {
            ctx.error("Failed to make build folder " + libBuildFolder);
        }

        TreeSet<File> fileList = lib.getSourceFiles(this);

        String origIncs = localCtx.get("includes");
        localCtx.set("includes", origIncs + "::" + "-I" + utility.getAbsolutePath());

        int fileCount = fileList.size();

        int count = 0;

        for(File f : fileList) {
            if(f.lastModified() > archiveDate) {
                File out = compileFile(localCtx, f, libBuildFolder);

                if(out == null) {
                    purgeLibrary(lib);
                    lib.setCompiledPercent(0);

                    Base.tryDelete(libBuildFolder);
                    return false;
                }

                localCtx.set("object.name", out.getAbsolutePath());
                boolean ok = (Boolean)localCtx.executeKey("compile.ar");

                if(!ok) {
                    purgeLibrary(lib);
                    lib.setCompiledPercent(0);

                    Base.tryDelete(out);
                    Base.tryDelete(libBuildFolder);
                    return false;
                }

                count++;
                lib.setCompiledPercent(count * 100 / fileCount);

                Base.tryDelete(out);
            }
        }

        localCtx.set("includes", origIncs);
        lib.setCompiledPercent(100);


        Base.tryDelete(libBuildFolder);

        return true;
    }

    private ArrayList<File> convertFiles(File dest, ArrayList<File> sources) throws IOException {
        ArrayList<File> objectPaths = new ArrayList<File>();
        PropertyFile props = ctx.getMerged();

        ctx.set("build.path", dest.getAbsolutePath());
        String objExt = ctx.parseString(props.get("compiler.object","o"));

        for(File file : sources) {
            File objectFile = new File(dest, file.getName() + "." + objExt);
            objectPaths.add(objectFile);

            if(objectFile.exists() && objectFile.lastModified() > file.lastModified()) {
                continue;
            }

            String sp = file.getAbsolutePath();
            String sf = sketchFolder.getAbsolutePath();
            sp = sp.substring(sf.length() + 1);

            String spf = file.getParentFile().getAbsolutePath();
            spf = spf.substring(sf.length() + 1);

            File destObjects = new File(dest, spf);
            destObjects.mkdirs();

            File destFile = new File(destObjects, file.getName());
            Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            ctx.set("source.name", sp);
            ctx.set("object.name", objectFile.getAbsolutePath());

            if(!(Boolean)ctx.executeKey("compile.bin"))
                return null;

            if(!objectFile.exists())
                return null;
        }

        return objectPaths;
    }

    private ArrayList<File> compileFileList(Context localCtx, File dest, ArrayList<File> sources, String key) {
        ArrayList<File> objectPaths = new ArrayList<File>();
        PropertyFile props = localCtx.getMerged();

        localCtx.set("build.path", dest.getAbsolutePath());
        String objExt = localCtx.parseString(props.get("compiler.object","o"));

        localCtx.addDataStreamParser(new DataStreamParser() {
            public String parseStreamMessage(Context localCtx, String m) {
                if (parseLineForWarningMessage(localCtx, m)) {
                    return "";
                }
                return m;
            }
            public String parseStreamError(Context localCtx, String m) {
                if (parseLineForErrorMessage(localCtx, m)) {
                    return "";
                }
                if (parseLineForWarningMessage(localCtx, m)) {
                    return "";
                }
                return m;
            }
        });

        for(File file : sources) {
            String fileName = file.getName();
            String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
            File objectFile = new File(dest, fileName + "." + objExt);
            objectPaths.add(objectFile);

            localCtx.set("source.name", file.getAbsolutePath());
            localCtx.set("object.name", objectFile.getAbsolutePath());

            if(objectFile.exists() && objectFile.lastModified() > file.lastModified()) {
                continue;
            }

            if(!(Boolean)localCtx.executeKey(key)) {
                localCtx.removeDataStreamParser();
                return null;
            }

            if(!objectFile.exists()) {
                localCtx.removeDataStreamParser();
                return null;
            }
        }
        localCtx.removeDataStreamParser();
        return objectPaths;
    }

    private ArrayList<File> compileFiles(Context localCtx, File dest, ArrayList<File> sSources, ArrayList<File> cSources, ArrayList<File> cppSources) {

        ArrayList<File> objectPaths = new ArrayList<File>();
        PropertyFile props = localCtx.getMerged();

        localCtx.set("build.path", dest.getAbsolutePath());
        String objExt = localCtx.parseString(props.get("compiler.object","o"));

        ArrayList<File> sObjects = compileFileList(localCtx, dest, sSources, "compile.S");
        if (sObjects == null) { return null; }

        ArrayList<File> cObjects = compileFileList(localCtx, dest, cSources, "compile.c");
        if (cObjects == null) { return null; }

        ArrayList<File> cppObjects = compileFileList(localCtx, dest, cppSources, "compile.cpp");
        if (cppObjects == null) { return null; }

        objectPaths.addAll(sObjects);
        objectPaths.addAll(cObjects);
        objectPaths.addAll(cppObjects);

        return objectPaths;
    }

    private ArrayList<File> compileSketch() throws IOException {
        ArrayList<File> sf = new ArrayList<File>();

        PropertyFile props = ctx.getMerged();

        String ext = ctx.parseString(props.get("build.extension"));
        if (ext == null) {
            ext = "cpp";
        }

        for (File f : filesToCompile) {
            File obj = compileFile(ctx, f, buildFolder);
            if (obj == null) {
                return null;
            }
            sf.add(obj);
        }
        
        String boardFiles = ctx.parseString(props.get("build.files"));

        if(boardFiles != null) {
            if(!boardFiles.equals("")) {
                ArrayList<File> sFiles = new ArrayList<File>();
                ArrayList<File> cFiles = new ArrayList<File>();
                ArrayList<File> cppFiles = new ArrayList<File>();

                String[] bfs = boardFiles.split("::");

                for(String bf : bfs) {
                    if(bf.endsWith(".c")) {
                        File f = new File(ctx.getBoard().getFolder(), bf);

                        if(f.exists()) {
                            cFiles.add(f);
                        }
                    } else if(bf.endsWith(".cpp")) {
                        File f = new File(ctx.getBoard().getFolder(), bf);

                        if(f.exists()) {
                            cppFiles.add(f);
                        }
                    } else if(bf.endsWith(".S")) {
                        File f = new File(ctx.getBoard().getFolder(), bf);

                        if(f.exists()) {
                            sFiles.add(f);
                        }
                    }
                }

                sf.addAll(compileFiles(ctx, buildFolder, sFiles, cFiles, cppFiles));
            }
        }

        File suf = new File(sketchFolder, "utility");

        if(suf.exists()) {
            File buf = new File(buildFolder, "utility");
            buf.mkdirs();
            ArrayList<File> uf = compileFiles(ctx,
                                buf,
                                findFilesInFolder(suf, "S", true),
                                findFilesInFolder(suf, "c", true),
                                findFilesInFolder(suf, "cpp", true)
                            );
            sf.addAll(uf);
        }

        return sf;
    }

    static public ArrayList<File> findFilesInFolder(File sketchFolder,
            String extension, boolean recurse) {
        ArrayList<File> files = new ArrayList<File>();

        if (sketchFolder == null) {
            return files;
        }

        if(sketchFolder.listFiles() == null)
            return files;

        for(File file : sketchFolder.listFiles()) {
            if(file.getName().startsWith("."))
                continue; // skip hidden files

            if(file.isDirectory()) {
                if(recurse) {
                    files.addAll(findFilesInFolder(file, extension, recurse));
                }

                continue;
            }

            if(extension == null) {
                files.add(file);
                continue;
            }

            if(file.getName().endsWith("." + extension)) {
                files.add(file);
                continue;
            }

        }

        return files;
    }

    private boolean compileLink(List<File> objectFiles) {
        PropertyFile props = ctx.getMerged();
        TreeMap<String, ArrayList<File>> coreLibs = getCoreLibs();

        String objectFileList = "";

        ctx.set("libraries.path", getCacheFolder().getAbsolutePath());

        String neverInclude = props.get("neverinclude");

        if(neverInclude == null) {
            neverInclude = "";
        }

        neverInclude = neverInclude.replaceAll(" ", "::");
        String neverIncludes[] = neverInclude.split("::");

        String liboption = props.get("compile.liboption");

        if(liboption == null) {
            liboption = "-l${library}";
        }

        String liblist = "";

        for(String libName : importedLibraries.keySet()) {
            Library lib = importedLibraries.get(libName);
            File aFile = getCacheFile(getArchiveName(lib));
            String headerName = lib.getName() + ".h";
            boolean inc = true;

            for(String ni : neverIncludes) {
                if(ni.equals(headerName)) {
                    inc = false;
                }
            }

            if(aFile.exists() && inc) {
                ctx.set("library", lib.getLinkName());
                liblist += "::" + ctx.parseString(liboption);
            }
        }

        for(String libName : coreLibs.keySet()) {
            ctx.set("library", "Core_" + libName);
            liblist += "::" + ctx.parseString(liboption);
        }

        ctx.set("libraries", liblist);

        for(File file : objectFiles) {
            objectFileList = objectFileList + file.getAbsolutePath() + "::";
        }

        ctx.set("build.path", buildFolder.getAbsolutePath());
        ctx.set("object.filelist", objectFileList);

        return (Boolean)ctx.executeKey("compile.link");
    }

    public boolean isUntitled() {
        return isUntitled;
    }


    // Scan all the "common" areas where examples may be found, and see if the
    // start of the sketch folder's path matches any of them.

    public boolean isInternal() {
        String path = sketchFolder.getAbsolutePath();
        String basePath = Base.getContentFile(".").getAbsolutePath() + File.separator;
        String cachePath = getCacheFolder().getAbsolutePath() + File.separator;
        String corePath = ctx.getCore().getFolder().getAbsolutePath() + File.separator;
        String boardPath = ctx.getBoard().getFolder().getAbsolutePath() + File.separator;

        if(path.startsWith(basePath)) return true;

        if(path.startsWith(cachePath)) return true;

        if(path.startsWith(corePath)) return true;

        if(path.startsWith(boardPath)) return true;

        return false;
    }

    public void cleanBuild() {
        Base.removeDescendants(buildFolder);
    }

    public void about() {
        ctx.bullet("Sketch folder: " + sketchFolder.getAbsolutePath());
        ctx.bullet("Selected board: " + ctx.getBoard().getName());
        ctx.bullet("Board folder: " + ctx.getBoard().getFolder().getAbsolutePath());
    }

    public File getLibrariesFolder() {
        return new File(sketchFolder, "libraries");
    }

    public HashMap<String, Library> getLibraries() {
        return importedLibraries;
    }

    public boolean programFile(Programmer programmer, String file) {
        Programmer p = ctx.getProgrammer();
        if (p == null) {
            ctx.error(Base.i18n.string("err.noprogrammer"));
            return false;
        }
        return p.programFile(ctx, file);
    }

    /**************************************************************************
     * MESSAGING AND FEEDBACK                                                 *
     *                                                                        *
     * Any output to the user gets fed through these routines.  If there is   *
     * a valid editor then the output goes to the editor's console, otherwise *
     * it goes to stdout / stderr.                                            *
     **************************************************************************/

    public boolean isWarningMessage(String s) {
        PropertyFile props = ctx.getMerged();
        String wRec = props.get("compiler.warning");
        int wFilename = props.getInteger("compiler.warning.filename", 1);
        int wLine = props.getInteger("compiler.warning.line", 2);
        int wMessage = props.getInteger("compiler.warning.message", 3);

        if(wRec != null) {
            Pattern wPat = Pattern.compile(wRec);
            Matcher wMat = wPat.matcher(s);

            if(wMat.find()) {
                return true;
            }
        }
        return false;
    }

    public SketchFile getSketchFileByFile(File f) {
        SketchFile sf = sketchFiles.get(f.getName());
        if (sf == null) return null;
        if (sf.getFile() == f) return sf;
        return null;
    }

    public void flagError(String s) {

        PropertyFile props = ctx.getMerged();

        String eRec = props.get("compiler.error");
        int eFilename = props.getInteger("compiler.error.filename", 1);
        int eLine = props.getInteger("compiler.error.line", 2);
        int eMessage = props.getInteger("compiler.error.message", 3);

        String wRec = props.get("compiler.warning");
        int wFilename = props.getInteger("compiler.warning.filename", 1);
        int wLine = props.getInteger("compiler.warning.line", 2);
        int wMessage = props.getInteger("compiler.warning.message", 3);

        if(eRec != null) {
            Pattern ePat = Pattern.compile(eRec);
            Matcher eMat = ePat.matcher(s);

            if(eMat.find()) {
                try {
                    int errorLineNumber = Integer.parseInt(eMat.group(eLine));

                    File errorFile = new File(eMat.group(eFilename));

                    if(errorFile != null) {

                        link("uecide://error/" + errorLineNumber + "/" + errorFile.getAbsolutePath() + "|Error at line " + errorLineNumber + " in file " + errorFile.getName());
                        SketchFile sf = getSketchFileByFile(errorFile);
                        if (sf != null) {
                            sf.setLineComment(errorLineNumber, eMat.group(eMessage));
                        }
                    }
                } catch(Exception e) {
                    ctx.error(e);
                }
            }
        }

        if(wRec != null) {
            Pattern wPat = Pattern.compile(wRec);
            Matcher wMat = wPat.matcher(s);

            if(wMat.find()) {
                try {
                    int warningLineNumber = Integer.parseInt(wMat.group(wLine));

                    File warningFile = new File(wMat.group(wFilename));

                } catch(Exception e) {
                    ctx.error(e);
                }
            }
        }
    }

    String mBuffer = "";
    public void messageStream(String msg) {

        mBuffer += msg;
        int nlpos = mBuffer.lastIndexOf("\n");

        if(nlpos == -1) {
            nlpos = mBuffer.lastIndexOf("\r");
        }
        if(nlpos == -1) {
            return;
        }

        mBuffer = mBuffer.replaceAll("\\r\\n", "\\n");
        mBuffer = mBuffer.replaceAll("\\r", "\\n");

        boolean eol = false;

        if(mBuffer.endsWith("\n")) {
            mBuffer = mBuffer.substring(0, mBuffer.length() - 1);
            eol = true;
        }

        String[] bits = mBuffer.split("\n");

        for(int i = 0; i < bits.length - 1; i++) {
           ctx.message(bits[i]);
        }

        if(eol) {
            mBuffer = "";
            ctx.message(bits[bits.length - 1]);
        } else {
            mBuffer = bits[bits.length - 1];
        }
    }

    String wBuffer = "";
    public void warningStream(String msg) {
        wBuffer += msg;
        int nlpos = wBuffer.lastIndexOf("\n");

        if(nlpos == -1) {
            return;
        }

        boolean eol = false;

        if(wBuffer.endsWith("\n")) {
            wBuffer = wBuffer.substring(0, wBuffer.length() - 1);
            eol = true;
        }

        String[] bits = wBuffer.split("\n");

        for(int i = 0; i < bits.length - 1; i++) {
            ctx.warning(bits[i]);
        }

        if(eol) {
            wBuffer = "";
            ctx.warning(bits[bits.length - 1]);
        } else {
            wBuffer = bits[bits.length - 1];
        }
    }

    String eBuffer = "";
    public void errorStream(String msg) {
        eBuffer += msg;
        int nlpos = eBuffer.lastIndexOf("\n");

        if(nlpos == -1) {
            return;
        }

        boolean eol = false;

        if(eBuffer.endsWith("\n")) {
            eBuffer = eBuffer.substring(0, eBuffer.length() - 1);
            eol = true;
        }

        String[] bits = eBuffer.split("\n");

        for(int i = 0; i < bits.length - 1; i++) {
            ctx.error(bits[i]);
        }

        if(eol) {
            eBuffer = "";
            ctx.error(bits[bits.length - 1]);
        } else {
            eBuffer = bits[bits.length - 1];
        }
    }

    public void link(String s) {
        if(!s.endsWith("\n")) {
            s += "\n";
        }
    }        

    public void command(String s) {
        if(!s.endsWith("\n")) {
            s += "\n";
        }
            if (Base.cli.isSet("verbose")) {
                System.out.print(s);
            }
    }        

    /**************************************************************************
     * OPTIONS HANDLING                                                       *
     *                                                                        *
     * Any options specified by the Options menu are controlled through these *
     * routines.                                                              *
     **************************************************************************/

    public String getOption(String opt) {
        PropertyFile props = ctx.getMerged();
        PropertyFile opts = Base.preferences.getChildren("board." + ctx.getBoard().getName() + ".options");
        String optval = opts.get(opt);


        if(optval == null) {
            if (opt.contains("@")) {
                String bits[] = opt.split("@");
                String libLinkName = bits[0];
                String libCatName = bits[1];
                for(Library lib : importedLibraries.values()) {
                    if (lib.getLinkName().equals(libLinkName)) {
                        props = lib.getProperties();
                        optval = props.get("options." + libCatName + ".default");
                    }
                }
            } else {
                optval = props.get("options." + opt + ".default");
            }
        }

        return optval;
    }

    public void setOption(String opt, String val) {
        PropertyFile props = ctx.getMerged();
        Preferences.set("board." + ctx.getBoard().getName() + ".options." + opt, val);

        if (opt.contains("@")) {
            String bits[] = opt.split("@");
            String libLinkName = bits[0];
            String libCatName = bits[1];
            for(Library lib : importedLibraries.values()) {
                if (lib.getLinkName().equals(libLinkName)) {
                    props = lib.getProperties();
                    if (props.getBoolean("options." + libCatName + ".purge")) {
                        needPurge();
                    }
                }
            }
        } else {
            if(props.getBoolean("options." + opt + ".purge")) {
                needPurge();
            }
        }
    }

    public TreeMap<String, String> getOptionGroups() {
        PropertyFile props = ctx.getMerged();

        String[] options = props.childKeysOf("options");
        TreeMap<String, String> out = new TreeMap<String, String>();

        for(String opt : options) {
            String optName = props.get("options." + opt + ".name");
            out.put(opt, optName);
        }

        for(Library lib : importedLibraries.values()) {
            props = lib.getProperties();
            options = props.childKeysOf("options");
            for (String opt : options) {
                String optName = props.get("options." + opt + ".name");
                out.put(lib.getLinkName() + "@" + opt, optName);
            }
        }

        return out;
    }

    @SuppressWarnings("unchecked")
    public TreeMap<String, String> getOptionNames(String group) {
        TreeMap<String, String> out = new TreeMap<String, String>(new NaturalOrderComparator());
        PropertyFile props = ctx.getMerged();

        if (group.contains("@")) {
            String bits[] = group.split("@");
            String libLinkName = bits[0];
            String libCatName = bits[1];
            for(Library lib : importedLibraries.values()) {
                if (lib.getLinkName().equals(libLinkName)) {
                    props = lib.getProperties();
                    PropertyFile opts = props.getChildren("options." + libCatName);
                    for (String opt : opts.childKeys()) {
                        if(opt.equals("name")) continue;

                        if(opt.equals("default")) continue;

                        if(opt.equals("purge")) continue;

                        String name = opts.get(opt + ".name");

                        if(name != null) {
                            out.put(opt, name);
                        }
                    }
                }
            }
        } else {
            PropertyFile opts = props.getChildren("options." + group);
            for(String opt : opts.childKeys()) {
                if(opt.equals("name")) continue;

                if(opt.equals("default")) continue;

                if(opt.equals("purge")) continue;

                String name = opts.get(opt + ".name");

                if(name != null) {
                    out.put(opt, name);
                }
            }
        }

        return out;
    }

    public String getFlags(String type) {
        PropertyFile props = ctx.getMerged();
        PropertyFile opts = Base.preferences.getChildren("board." + ctx.getBoard().getName() + ".options");
        TreeMap<String, String> options = getOptionGroups();

        String flags = "";

        for(String opt : options.keySet()) {
            String value = getOption(opt);
            if (opt.contains("@")) {
                String bits[] = opt.split("@");
                String libLinkName = bits[0];
                String libOptName = bits[1];
                for (Library lib : importedLibraries.values()) {
                    if (lib.getLinkName().equals(libLinkName)) {
                        PropertyFile lprops = lib.getProperties();
                        String data = lprops.get("options." + libOptName + "." + value + "." + type);
                        if(data != null) {
                            flags = flags + "::" + data;
                        }
                    }
                }
            } else {
                String data = props.get("options." + opt + "." + value + "." + type);

                if(data != null) {
                    flags = flags + "::" + data;
                }
            }
        }

        return flags;
    }

    public File getBinariesFolder() {
        return new File(sketchFolder, "objects");
    }

    public HashMap<File, String[]> convertBinaryFiles() {

        // File +_ "extern [type] prefix_data[];"
        //      |_ "extern int prefix_len;"
        //      |_ "extern int prefix_width;" [images only]
        //      |_ "extern int prefix_height;" [images only]
        //      |_ ... etc
        HashMap<File, String[]> conversionData = new HashMap<File, String[]>();

        File dir = getBinariesFolder();
        if (!dir.exists()) {
            return conversionData;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return conversionData;
        }
        for (File file : files) {
            if (getInteger("binary." + file.getName() + ".conversion") > 0) {
                int type = FileType.getType(file);

                FileConverter conv = null;

                switch (type) {
                    case FileType.GRAPHIC:
                        if (getInteger("binary." + file.getName() + ".conversion") > 1) {
                            conv = new ImageFileConverter(file, getInteger("binary." + file.getName() + ".conversion"), get("binary." + file.getName() + ".datatype"), get("binary." + file.getName() + ".prefix"), getColor("binary." + file.getName() + ".transparency"), getInteger("binary." + file.getName() + ".threshold"));
                        } else {
                            conv = new BasicFileConverter(file, get("binary." + file.getName() + ".prefix"));
                        }
                        break;

                    default:
                        conv = new BasicFileConverter(file, get("binary." + file.getName() + ".prefix"));
                        break;
                }

                if (conv != null) {
                    if (conv.convertFile(getBuildFolder())) {
                        File cppFile = conv.getFile();
                        String[] headerLines = conv.getHeaderLines();
                        conversionData.put(cppFile, headerLines);
                    }
                }
            }
        }

        return conversionData;
    }

    public boolean libraryIsCompiled(Library l) {
        if(l == null) return false;

        if(l.isHeaderOnly()) {
            return true;
        }

        File arch = new File(getCacheFolder(), getArchiveName(l));
        return arch.exists();
    }

    public void purgeLibrary(Library lib) {
        File arch = new File(getCacheFolder(), getArchiveName(lib));
        Base.tryDelete(arch);
    }

    public void purgeCache() {
        Base.removeDir(getCacheFolder());
    }

    public void precompileLibrary(Library lib) {
        ctx.set("includes", generateIncludes());
        ctx.set("filename", sketchName);
        ctx.set("cache.root", getCacheFolder().getAbsolutePath());

        if(doPrePurge) {
            doPrePurge = false;
            Base.removeDir(getCacheFolder());
        }

        ctx.set("option.flags", getFlags("flags"));
        ctx.set("option.cflags", getFlags("cflags"));
        ctx.set("option.cppflags", getFlags("cppflags"));
        ctx.set("option.ldflags", getFlags("ldflags"));

        compileLibrary(lib);
    }
/* TODO: Move to SketchFile */
    public void renameFile(File old, File newFile) {
    }

/* TODO: Move to SketchFile */
    public void deleteFile(File f) {
    }

    public void rescanFileTree() throws IOException {
        sketchFiles.clear();
        loadSketchFromFolder(sketchFolder);
    }

    public void purgeBuildFiles() {
        Base.removeDescendants(buildFolder);
    }

    public boolean isChildOf(File dir) {
        File parent = sketchFolder.getParentFile();

        while(parent != null && parent.exists()) {
            if(parent.equals(dir)) {
                return true;
            }

            parent = parent.getParentFile();
        }

        return false;
    }

    public boolean parentIsIn(File[] files) {
        for (File file : files) {
           if (isChildOf(file)) {
                return true;
            }
        }
        return false;
    }

    public boolean parentIsLibrary() {
        ArrayList<File> filelist = new ArrayList<File>();

        TreeSet<String> groups = Library.getLibraryCategories();

        for(String group : groups) {
            TreeSet<Library> libs = Library.getLibraries(group);

            if(libs == null) {
                continue;
            }

            for(Library lib : libs) {
                filelist.add(lib.getFolder());
            }
        }
        return parentIsIn(filelist.toArray(new File[0]));
    }

    public boolean parentIsBoard() {
        return parentIsIn(Base.getBoardsFolders());
    }

    public boolean parentIsCore() {
        return parentIsIn(Base.getCoresFolders());
    }

    public boolean parentIsCompiler() {
        return parentIsIn(Base.getCompilersFolders());
    }

    public boolean parentIsProtected() {
        return
            parentIsCore() ||
            parentIsBoard() ||
            parentIsCompiler() ||
            parentIsLibrary();
    }

    public void requestTermination() {
        terminateExecution = true;
        ctx.killRunningProcess();
    }

    public boolean generateSarFile(File archiveFile) {
        try {
            if(archiveFile.exists()) {
                Base.tryDelete(archiveFile);
            }

            FileOutputStream outfile = new FileOutputStream(archiveFile);
            ZipOutputStream sar = new ZipOutputStream(outfile);
            sar.putNextEntry(new ZipEntry(sketchFolder.getName() + "/"));
            sar.closeEntry();
            addTree(sketchFolder, sketchFolder.getName(), sar);

            sar.putNextEntry(new ZipEntry("libraries" + "/"));
            sar.closeEntry();

            prepare();

            String libList = "";

            for(Library lib : getImportedLibraries()) {
                if(lib.isContributed()) {
                    sar.putNextEntry(new ZipEntry("libraries" + "/" + lib.getFolder().getName() + "/"));
                    sar.closeEntry();
                    addTree(lib.getFolder(), "libraries/" + lib.getFolder().getName(), sar);

                    if(libList.equals("")) {
                        libList = lib.getFolder().getName();
                    } else {
                        libList = libList + " " + lib.getFolder().getName();
                    }
                }
            }

            sar.putNextEntry(new ZipEntry("META-INF/"));
            sar.closeEntry();
            sar.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());

            StringBuilder mf = new StringBuilder();
            mf.append("SAR-Version: 1.0\n");
            mf.append("Author: " + System.getProperty("user.name") + "\n");
            mf.append("Sketch-Name: " + sketchFolder.getName() + "\n");
            mf.append("Libraries: " + libList + "\n");
            mf.append("Board: " + ctx.getBoard().getName() + "\n");
            mf.append("Core: " + ctx.getCore().getName() + "\n");
            mf.append("Archived: " + timeStamp + "\n");

            String mfData = mf.toString();
            byte[] bytes = mfData.getBytes();
            sar.write(bytes, 0, bytes.length);
            sar.closeEntry();
            sar.flush();
            sar.close();
        } catch(Exception e) {
            ctx.error(e);
            return false;
        }

        return true;
    }

    public void addTree(File dir, String sofar, ZipOutputStream zos) throws IOException {
        String files[] = dir.list();

        for(int i = 0; i < files.length; i++) {
            if(files[i].equals(".") || files[i].equals("..")) continue;

            if(files[i].startsWith(".git")) continue;

            if(files[i].startsWith(".svn")) continue;

            if(files[i].startsWith(".csv")) continue;

            if(files[i].startsWith(".SVN")) continue;

            if(files[i].startsWith(".CSV")) continue;

            File sub = new File(dir, files[i]);
            String nowfar = (sofar == null) ?  files[i] : (sofar + "/" + files[i]);

            if(sub.isDirectory()) {
                // directories are empty entries and have / at the end
                ZipEntry entry = new ZipEntry(nowfar + "/");
                zos.putNextEntry(entry);
                zos.closeEntry();
                addTree(sub, nowfar, zos);
            } else {
                ZipEntry entry = new ZipEntry(nowfar);
                entry.setTime(sub.lastModified());
                zos.putNextEntry(entry);
                zos.write(Utils.loadBytesRaw(sub));
                zos.closeEntry();
            }
        }
    }

    public void loadConfig() throws IOException {
        PropertyFile m = ctx.getMerged();
        if (m.get("sketch.board") != null) {
            String wantedBoard = m.get("sketch.board");
            Board b = Base.getBoard(wantedBoard);
            if (b == null) {
                ctx.error(Base.i18n.string("err.badboard", wantedBoard));
            } else {
                ctx.bullet(Base.i18n.string("msg.selecting.board", b));
                ctx.action("SetBoard", b);
            }
        }

        if (m.get("sketch.core") != null) {
            String wantedCore = m.get("sketch.core");
            Core c = Base.getCore(wantedCore);
            if (c == null) {
                ctx.error(Base.i18n.string("err.badcore", wantedCore));
            } else {
                ctx.bullet(Base.i18n.string("msg.selecting.core", c));
                ctx.setCore(c);
            }
        }

        if (m.get("sketch.compiler") != null) {
            String wantedCompiler = m.get("sketch.compiler");
            Compiler c = Base.getCompiler(wantedCompiler);
            if (c == null) {
                ctx.error(Base.i18n.string("err.badcompiler", wantedCompiler));
            } else {
                ctx.bullet(Base.i18n.string("msg.selecting.compiler", c));
                ctx.action("SetCompiler", c);
            }
        }

        if (m.get("sketch.programmer") != null) {
            ctx.action("SetProgrammer", m.get("sketch.programmer"));
        }

        if (m.get("sketch.port") != null) {
            ctx.action("SetDevice", m.get("sketch.port"));
        }

    }

    public String toString() {
        return sketchName;
    }

    public ImageIcon getIcon(int size) {
        ImageIcon i = null;

        if (size == 16) {
            i = getIcon();
            if(i != null) return i;
        }

        if(ctx.getBoard() != null) {
            i = ctx.getBoard().getIcon(size);

            if(i != null) return i;
        }

        if(ctx.getCore() != null) {
            i = ctx.getCore().getIcon(size);

            if(i != null) return i;
        }

        return null;
    }

    public String getArch() {
        if (ctx.getBoard().get("arch") != null) {
            return ctx.getBoard().get("arch");
        }
        if (ctx.getCore().get("arch") != null) {
            return ctx.getCore().get("arch");
        }
        if (ctx.getCompiler().get("arch") != null) {
            return ctx.getCompiler().get("arch");
        }
        return ctx.getBoard().get("family");
    }
/* TODO: Move this to SketchFile 
    public ArrayList<TodoEntry> todo(SketchFile f) {
        if (!sketchFiles.contains(f)) {
            return null;
        }

        Pattern p = Pattern.compile("(?i)\\/\\/\\s*(TODO|NOTE|FIXME):\\s*(.*)$");

        ArrayList<TodoEntry> found = new ArrayList<TodoEntry>();

        String content = getFileContent(f);
        String[] lines = content.split("\n");
        int lineno = 1;
        for (String line : lines) {
            Matcher m = p.matcher(line);
            if (m.find()) {
                String type = m.group(1).trim().toLowerCase();
                String comment = m.group(2).trim();
                int itype = 0;

                if (type.equals("todo")) {
                    itype = TodoEntry.Todo;
                } else if (type.equals("note")) {
                    itype = TodoEntry.Note;
                } else if (type.equals("fixme")) {
                    itype = TodoEntry.Fixme;
                }
                if (itype != 0) {
                    found.add(new TodoEntry(f, lineno, comment, itype));
                }
            }
            lineno++;
        }

        return found;
    }
*/

    public void addKeywordsFromFile(File f) throws IOException {

        String kwd = Utils.getFileAsString(f);

        String[] lines = kwd.split("\n");
        Pattern p = Pattern.compile("^\\s*([^\\s]+)\\s+([^\\s]+)");
        for (String line : lines) {
            Matcher m = p.matcher(line);
            if (m.find()) {
                String name = m.group(1);
                String type = m.group(2);
                if (type.equals("LITERAL1")) {
                    keywords.put(name, KeywordTypes.LITERAL1);
                } else if (type.equals("LITERAL2")) {
                    keywords.put(name, KeywordTypes.LITERAL2);
                } else if (type.equals("LITERAL3")) {
                    keywords.put(name, KeywordTypes.LITERAL3);
                } else if (type.equals("KEYWORD1")) {
                    keywords.put(name, KeywordTypes.KEYWORD1);
                } else if (type.equals("KEYWORD2")) {
                    keywords.put(name, KeywordTypes.KEYWORD2);
                } else if (type.equals("KEYWORD3")) {
                    keywords.put(name, KeywordTypes.KEYWORD3);
                } else if (type.equals("OBJECT")) {
                    keywords.put(name, KeywordTypes.OBJECT);
                } else if (type.equals("VARIABLE")) {
                    keywords.put(name, KeywordTypes.VARIABLE);
                } else if (type.equals("FUNCTION")) {
                    keywords.put(name, KeywordTypes.FUNCTION);
                } else if (type.equals("DATATYPE")) {
                    keywords.put(name, KeywordTypes.DATATYPE);
                } else if (type.equals("RESERVED")) {
                    keywords.put(name, KeywordTypes.RESERVED);
                }
            }
        }
    }


    public void updateKeywords() throws IOException {
        keywords.clear();
        if (ctx.getCompiler() != null) {
            addKeywordsFromFile(ctx.getCompiler().getKeywords());
        }
        if (ctx.getCore() != null) {
            addKeywordsFromFile(ctx.getCore().getKeywords());
        }
        if (ctx.getBoard() != null) {
            addKeywordsFromFile(ctx.getBoard().getKeywords());
        }
        for (Library l : importedLibraries.values()) {
            addKeywordsFromFile(l.getKeywords());
        }
        for (FunctionBookmark bm : functionListBm) {
            if (bm.isFunction()) {
                keywords.put(bm.getName(), KeywordTypes.KEYWORD3);
            }
            if (bm.isMemberFunction()) {
                keywords.put(bm.getName(), KeywordTypes.KEYWORD3);
            }
        }

    }

    public HashMap<String, Integer> getKeywords() {
        return keywords;
    }

    public ImageIcon getIcon() {
        return null;
    }

    public void generateFileFromTemplate(String template, File output) {
        try {
            String[] lines = template.split("\n");
            PrintWriter out = new PrintWriter(output);
            for (String line : lines) {
                String parsed = ctx.parseString(line);
                out.println(parsed);
            }
            out.close();
        } catch (Exception e) {
            Base.error(e);
        }
    }

    public void generateMakefile() throws IOException {
        PropertyFile props = ctx.getMerged();
        if (props.get("makefile.template") != null) {
            String template = Utils.getFileAsString(new File(ctx.parseString(props.get("makefile.template"))));
            File out = new File(sketchFolder, "Makefile");
            generateFileFromTemplate(template, out);
        }
    }

    public Context getContext() {
        return ctx;
    }

    public PropertyFile mergeAllProperties() {
        return ctx.getMerged();
    }

    public String parseString(String s) {
        return ctx.parseString(s);
    }

    public boolean parseLineForErrorMessage(Context ctx, String mess) {
        PropertyFile props = ctx.getMerged();
        Pattern p = Pattern.compile(props.get("compiler.error"));
        Matcher m = p.matcher(mess);
        if (m.find()) {
            File errorFile = new File(m.group(1));
            int errorLineNumber = Integer.parseInt(m.group(2));
            ctx.parsedMessage("{\\bullet}{\\error Error at line " + errorLineNumber + " in file " + errorFile.getName() + ":}\n");
            ctx.parsedMessage("{\\bullet2}{\\error " + m.group(3) + "}\n");
            SketchFile sf = getSketchFileByFile(errorFile);
            if (sf != null) {
                sf.setLineComment(errorLineNumber, m.group(3));
            }
            return true;
        }
        return false;
    }

    public boolean parseLineForWarningMessage(Context ctx, String mess) {
        PropertyFile props = ctx.getMerged();
        Pattern p = Pattern.compile(props.get("compiler.warning"));
        Matcher m = p.matcher(mess);
        if (m.find()) {
            File errorFile = new File(m.group(1));
            int errorLineNumber = Integer.parseInt(m.group(2));
            ctx.parsedMessage("{\\bullet}{\\warning Warning at line " + errorLineNumber + " in file " + errorFile.getName() + ":}\n");
            ctx.parsedMessage("{\\bullet2}{\\warning " + m.group(3) + "}\n");
            SketchFile sf = getSketchFileByFile(errorFile);
            if (sf != null) {
                sf.setLineComment(errorLineNumber, m.group(3));
            }
            return true;
        }
        return false;
    }

    public void parsedMessage(String e) {
        ctx.printParsed(e);
    }

    public ArrayList<String> gatherIncludes(File f) {
        String[] data;
        ArrayList<String> requiredLibraries = new ArrayList<String>();

        try {
            FileReader in = new FileReader(f);
            StringBuilder contents = new StringBuilder();
            char[] buffer = new char[4096];
            int read = 0;

            do {
                contents.append(buffer, 0, read);
                read = in.read(buffer);
            } while(read >= 0);

            in.close();
            data = contents.toString().split("\n");
        } catch(Exception e) {
            Base.error(e);
            return null;
        }

        for(String line : data) {
            line = line.trim();

            if(line.startsWith("#include")) {
                int qs = line.indexOf("<");

                if(qs == -1) {
                    qs = line.indexOf("\"");
                }

                if(qs == -1) {
                    continue;
                }

                qs++;
                int qe = line.indexOf(">");

                if(qe == -1) {
                    qe = line.indexOf("\"", qs);
                }

                String i = line.substring(qs, qe);

                requiredLibraries.add(i);
            }
        }
        return requiredLibraries;
    }

    public void addRecursiveLibraries(HashMap<String, Library>foundLibs, ArrayList<String> missingLibs, Library lib) {
        for (String inc : lib.getRequiredLibraries()) {
            Library sl = findLibrary(inc);
            if (sl != null) {
                if (foundLibs.get(sl.getMainInclude()) == null) {
                    foundLibs.put(sl.getMainInclude(), sl);
                    addRecursiveLibraries(foundLibs, missingLibs, sl);
                }
            } else {
                if (missingLibs.indexOf(inc) == -1) {
                    missingLibs.add(inc);
                }
            }
        }
    }

    public boolean huntForLibraries(File f, HashMap<String, Library>foundLibs, ArrayList<String> missingLibs) throws IOException {

        if (getBuildFolder() == null) {
            return false;
        }
        PropertyFile props = ctx.getMerged();
        if (props.get("compile.preproc") == null) { // Manually parse it.

            ArrayList<String> incs = gatherIncludes(f);
            for (String inc : incs) {
                Library l = findLibrary(inc);
                if (l != null) {
                    if (foundLibs.get(l.getMainInclude()) == null) {
                        foundLibs.put(l.getMainInclude(), l);
                        addRecursiveLibraries(foundLibs, missingLibs, l);
                    }
                } else {
                    if (missingLibs.indexOf(inc) == -1) {
                        missingLibs.add(inc);
                    }
                }
            }

            return true;
        }

        File dst = new File(getBuildFolder(), "deps.txt");

        ctx.silence = true;

        int numberFoundThisPass = 0;
        do {
            ctx.snapshot();

            ctx.set("option.flags", getFlags("flags"));
            ctx.set("option.cflags", getFlags("cflags"));
            ctx.set("option.cppflags", getFlags("cppflags"));
            ctx.set("option.ldflags", getFlags("ldflags"));

            numberFoundThisPass = 0;
            ctx.set("source.name", f.getAbsolutePath());
            ctx.set("object.name", dst.getAbsolutePath());
            String libPaths = "";
            for (Library aLib : foundLibs.values()) {
                if (!libPaths.equals("")) {
                    libPaths += "::";
                }
                libPaths += "-I" + aLib.getSourceFolder().getAbsolutePath();
            }
            ctx.set("includes", libPaths);

            ctx.startBuffer(true);
            ctx.executeKey("compile.preproc");
            ctx.endBuffer();

            if (dst.exists()) {
                String data = Utils.getFileAsString(dst);
                data = data.replaceAll("\\\\\n", " ");
                data = data.replaceAll("\\s+", "::");
                String[] entries = data.split("::");
                for (String entry : entries) {
                    if (entry.endsWith(".h")) {
                        Library lib = findLibrary(entry);
                        if (lib != null) {
                            foundLibs.put(entry, lib);
                            numberFoundThisPass++;
                        } else {
                            if(missingLibs.indexOf(entry) == -1) {
                                missingLibs.add(entry);
                            }
                        }
                    }
                }
            }

            ctx.rollback();
        } while (numberFoundThisPass > 0);

        ctx.silence = false;
        return true;
    }

    long lastConfigChange = 0;
    public boolean updateSketchConfig() {
        File sketchConfigFile = new File(sketchFolder, "sketch.cfg");
        if (!sketchConfigFile.exists()) {
            return false;
        }

        long lastMod = sketchConfigFile.lastModified();
        if (lastConfigChange != lastMod) {
            ctx.loadSketchSettings(sketchConfigFile);
            lastConfigChange = lastMod;
            return true;
        }
        return false;
    }

    public void outputErrorStream(String msg) {
        if (Base.cli.isSet("verbose")) {
            System.err.print(msg);
        }
    }
   
    public void outputMessageStream(String msg) {
        if (Base.cli.isSet("verbose")) {
            System.out.print(msg);
        }
    }



    class CaseInsensitiveFileComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            if ((o1 instanceof File) && (o2 instanceof File)) {
                File f1 = (File)o1;
                File f2 = (File)o2;
                String n1 = f1.getName().toLowerCase();
                String n2 = f2.getName().toLowerCase();
                return n1.compareTo(n2);
            }
            return 0;
        }

        public boolean equals(Object o) {
            return this == o;
        }
    }

    public ArrayList<FunctionBookmark> getBookmarkList() {
        return functionListBm;
    }


    public void dumpAllBookmarks() {
        for (FunctionBookmark bm : functionListBm) {
            System.err.println(bm.dump());
        }
    }

    public void set(String key, String value) { settings.set(key, value); saveSettings(); }
    public void set(String key, int value) { settings.setInteger(key, value); saveSettings(); }
    public void set(String key, long value) { settings.setLong(key, value); saveSettings(); }
    public void set(String key, float value) { settings.setFloat(key, value); saveSettings(); }
    public void set(String key, boolean value) { settings.setBoolean(key, value); saveSettings(); }
    public void set(String key, Color value) { settings.setColor(key, value); saveSettings(); }

    public String get(String key) { return settings.get(key); }
    public int getInteger(String key) { return settings.getInteger(key); }
    public long getLong(String key) { return settings.getLong(key); }
    public float getFloat(String key) { return settings.getFloat(key); }
    public boolean getBoolean(String key) { return settings.getBoolean(key); }
    public Color getColor(String key) { return settings.getColor(key); }

    public String get(String key, String def) { return settings.get(key, def); }
    public int getInteger(String key, int def) { return settings.getInteger(key, def); }
    public long getLong(String key, long def) { return settings.getLong(key, def); }
    public float getFloat(String key, float def) { return settings.getFloat(key, def); }
    public boolean getBoolean(String key, boolean def) { return settings.getBoolean(key, def); }

    public void saveSettings() {
        File sketchPropertyFile = new File(sketchFolder, "sketch.properties");
        settings.save(sketchPropertyFile);
    }

    public void setContext(Context c) {
        ctx = c;
    }
}
