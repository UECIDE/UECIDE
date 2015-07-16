/*
 * Copyright (c) 2014, Majenko Technologies
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

import org.uecide.debug.*;
import org.uecide.editors.*;
import org.uecide.plugin.*;
import org.uecide.builtin.BuiltinCommand;
import org.uecide.varcmd.VariableCommand;

import java.util.regex.*;

import jssc.*;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.jar.*;
import java.util.zip.*;
import java.text.*;
import java.lang.reflect.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import org.uecide.Compiler;

import javax.script.*;

/**
 * The sketch class is the heart of the IDE.  It manages not only what files a
 * sketch consists of, but also deals with compilation of the sketch and uploading
 * the sketch to a target board.
 */
public class Sketch implements MessageConsumer {

    public Context ctx;

    public String sketchName;       // The name of the sketch
    public File sketchFolder;       // Where the sketch is
    public Editor editor = null;    // The editor window the sketch is loaded in
    public File buildFolder;        // Where to build the sketch
    public String uuid;             // A globally unique ID for temporary folders etc
    public PropertyFile configFile = null;  // File containing sketch configuration

    public String percentageFilter = null;
    public float percentageMultiplier = 1.0f;

    boolean isUntitled;             // Whether or not the sketch has been named

    // These are all the plugins selected for this sketch in order to compile
    // the sketch and upload it.
    Core selectedCore = null;
    Board selectedBoard = null;
    String selectedBoardName = null;
    Compiler selectedCompiler = null;
    String selectedProgrammer = null;
    String selectedSerialPort = null;
    InetAddress selectedNetworkPort = null;

    boolean terminateExecution = false;

    Process runningProcess = null;

    // This lot is what the sketch consists of - the list of files, libraries, parameters etc.
    public ArrayList<File> sketchFiles = new ArrayList<File>();

    public HashMap<String, Library> importedLibraries = new HashMap<String, Library>();
    public ArrayList<Library> orderedLibraries = new ArrayList<Library>();
    public ArrayList<String> unknownLibraries = new ArrayList<String>();

    public TreeMap<String, String> parameters = new TreeMap<String, String>();

    TreeMap<String, String> selectedOptions = new TreeMap<String, String>();

    HashMap<File, HashMap<Integer, String>> functionList = new HashMap<File, HashMap<Integer, String>>();

    // Do we want to purge the cache files before building?  This is set by the
    // options system.
    public boolean doPrePurge = false;

    /**************************************************************************
     * CONSTRUCTORS                                                           *
     *                                                                        *
     * The Sketch takes a file, be that as a string or a File object, as the  *
     * only parameter.  The sketch is loaded either from the provided folder, *
     * or the folder the provided file resides in.                            *
     **************************************************************************/

    HashMap<File, HashMap<Integer, String>>lineComments = new HashMap<File, HashMap<Integer, String>>();

    HashMap<String, Integer>keywords = new HashMap<String, Integer>();

    public void setLineComment(File file, int line, String comment) {
        HashMap<Integer, String> comments = lineComments.get(file);

        if(comments == null) {
            comments = new HashMap<Integer, String>();
        }

        comments.put(line, comment);
        lineComments.put(file, comments);
    }

    public String getLineComment(File file, int line) {
        HashMap<Integer, String> comments = lineComments.get(file);

        if(comments == null) {
            return null;
        }

        return comments.get(line);
    }

    public HashMap<Integer, String> getLineComments(File file) {
        return lineComments.get(file);
    }

    public void clearLineComments() {
        lineComments = new HashMap<File, HashMap<Integer, String>>();
    }

    public Sketch() {
        // This is only for dummy use. We don't want to do ANYTHING!
        ctx = new Context();
        ctx.setSketch(this);
    }

    public Sketch(String path) {
        this(new File(path));
        ctx = new Context();
        ctx.setSketch(this);
    }

    public Sketch(File path) {
        uuid = UUID.randomUUID().toString();
        ctx = new Context();
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
                    if (!nsf.exists()) {
                        if (Base.yesno("Import Sketch?", "This sketch file isn't in a properly named sketch folder.\nImport this sketch into your sketchbook area?")) {
                            File sb = new File(Base.getSketchbookFolder(), inoName);
                            sb.mkdirs();
                            File inof = new File(sb, inoName + ".ino");
                            Base.copyFile(oldPath, inof);
                            path = sb;
                        } else {
                            path = createUntitledSketch();
                            sketchFolder = path;
                            sketchName = sketchFolder.getName();
                            path.mkdirs();
                            createBlankFile(path.getName() + ".ino");
                            inoName = path.getName();
                        }
                    }
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

    // Get the board as a string
    public String getBoardName() {
        return selectedBoardName;
    }

    // Get the board as a Board object
    public Board getBoard() {
        return selectedBoard;
    }

    // Look up a board in the global boards map and set the board appropriately
    public void setBoard(String board) {
        if(board == null || board.equals("")) return;

        Board b = Base.getBoard(board);
        setBoard(b);
    }

    // Set the current board.  Also looks up the last settings used for that board
    // and propogates them onwards (core, compiler, etc).
    public void setBoard(Board board) {
        Debug.message("Selecting board " + board);

        if(board == null) {
            return;
        }

        ctx.setBoard(board);

        selectedBoard = board;
        selectedBoardName = selectedBoard.getName();
        Preferences.set("board", board.getName());
        String boardsCore = Preferences.get("board." + selectedBoard.getName() + ".core");
        Core core = null;

        if(boardsCore != null) {
            core = Base.cores.get(boardsCore);
        }

        if(core == null) {
            core = board.getCore();
        }

        Debug.message("Board's core is " + core);

        if(core != null) {
            setCore(core);
        }

    }

    public Core getCore() {
        return selectedCore;
    }

    public void setCore(String core) {
        if(core == null || core.equals("")) return;

        Core c = Base.getCore(core);
        setCore(c);
    }

    public void setSettings() {
        ctx.set("sketch.name", getName());
        ctx.set("sketch.path", getFolder().getAbsolutePath());
    }

    public void setCore(Core core) {
        Debug.message("Selecting core " + core);
        if(core == null) {
            return;
        }

        ctx.setCore(core);

        selectedCore = core;
        Preferences.set("board." + selectedBoard.getName() + ".core", core.getName());
        String boardsCompiler = Preferences.get("board." + selectedBoard.getName() + ".compiler");

        Compiler compiler = null;

        if(boardsCompiler != null) {
            compiler = Base.compilers.get(boardsCompiler);
        }

        if(compiler == null) {
            compiler = core.getCompiler();
        }

        if(compiler != null) {
            setCompiler(compiler);
        }

        if(editor != null) {
            editor.updateTree();
        }
    }

    public Compiler getCompiler() {
        return selectedCompiler;
    }

    public void setCompiler(String compiler) {
        if(compiler == null || compiler.equals("")) return;

        Compiler c = Base.getCompiler(compiler);
        setCompiler(c);
    }

    public void setCompiler(Compiler compiler) {
        Debug.message("Selecting compiler " + compiler);
        if(compiler == null) {
            return;
        }

        ctx.setCompiler(compiler);

        selectedCompiler = compiler;
        if (selectedBoard == null) {
            return;
        }

        Preferences.set("board." + selectedBoard.getName() + ".compiler", compiler.getName());
        String programmer = Preferences.get("board." + selectedBoard.getName() + ".programmer");

        if(programmer == null) {
            TreeMap<String, String> pl = getProgrammerList();

            for(String p : pl.keySet()) {
                programmer = p;
                break;
            }
        }

        setProgrammer(programmer);
    }

    public String getProgrammer() {
        return selectedProgrammer;
    }

    public TreeMap<String, String> getProgrammerList() {
        PropertyFile props = ctx.getMerged();
        TreeMap<String, String> out = new TreeMap<String, String>();

        String[] spl = props.getArray("sketch.upload");

        if(spl != null) {
            Arrays.sort(spl);

            for(String pn : spl) {
                String name = ctx.parseString(props.get("upload." + pn + ".name"));
                out.put(pn, name);
            }
        }

        return out;
    }

    public void setProgrammer(String programmer) {
        if(programmer == null || programmer.equals("")) return;

        Preferences.set("board." + selectedBoard.getName() + ".programmer", programmer);
        selectedProgrammer = programmer;

        if(editor != null) editor.updateAll();
    }

    public InetAddress getNetworkPort() {
        return selectedNetworkPort;
    }

    public String getNetworkPortIP() {
        if(selectedNetworkPort == null) {
            return null;
        }

        byte[] ip = selectedNetworkPort.getAddress();
        return String.format("%d.%d.%d.%d",
                             (int)ip[0] & 0xFF,
                             (int)ip[1] & 0xFF,
                             (int)ip[2] & 0xFF,
                             (int)ip[3] & 0xFF);
    }

    public String getSerialPort() {
        return selectedSerialPort;
    }

    public void setPort(Object port) {
        if(port instanceof SerialPort) {
            SerialPort sp = (SerialPort)port;
            setSerialPort(sp.getPortName());
            return;
        }

        if(port instanceof InetAddress) {
            InetAddress ip = (InetAddress)port;
            setNetworkPort(ip);
            return;
        }
    }

    public void setNetworkPort(String ip) {
        if(ip == null) {
            setNetworkPort((InetAddress)null);
        } else {
            try {
                if(ip.startsWith("/")) {
                    ip = ip.substring(1);
                }

                InetAddress nip = InetAddress.getByName(ip);
                setNetworkPort(nip);
            } catch(Exception e) {
                Base.error(e);
                setNetworkPort((InetAddress)null);
            }
        }
    }

    public void setNetworkPort(InetAddress ip) {
        selectedNetworkPort = ip;

        if(selectedBoard != null) {
            if(selectedNetworkPort == null) {
                Preferences.unset("board." + selectedBoard.getName() + ".ip");
            } else {
                Preferences.set("board." + selectedBoard.getName() + ".ip", selectedNetworkPort.toString());
            }

        }

        if(editor != null) editor.updateAll();
    }

    public void setSerialPort(String p) {
        if(p == null || p.equals("")) return;

        selectedSerialPort = p;

        if(selectedBoard != null) {
            Preferences.set("board." + selectedBoard.getName() + ".port", selectedSerialPort);
            setNetworkPort((InetAddress)null);
        }

        if(editor != null) editor.updateAll();
    }

    public String getProgramPort() {
        String ip = getNetworkPortIP();
        if (ip != null) {
            return ip;
        }
        return getSerialPort();
    }

    void attachToEditor(Editor e) {
        editor = e;
        editor.setTitle(Base.theme.get("product.cap") + " | " + sketchName);
        ctx.setEditor(e);
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

            addLibraryToImportList(libname);

            if(editor != null) {
                editor.insertStringAtStart(getMainFile(), "#include <" + libname + ".h>\n");
                editor.updateTree();
                editor.openOrSelectFile(header);
                editor.openOrSelectFile(code);
            }
        } catch(Exception e) {
            Base.error(e);
        }
    }

    public void createNewFile(String filename) {
        if(filename.endsWith(".lib")) {
            createNewLibrary(filename.substring(0, filename.lastIndexOf(".")));
            return;
        }

        File f = createBlankFile(filename);
        sketchFiles.add(f);

        if(editor != null) {
            editor.updateTree();
            editor.openOrSelectFile(f);
        }
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

    public void loadSketchFromFolder(File folder) {
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

        setBoard(Preferences.get("board"));

        if(selectedBoard != null) {
            setSerialPort(Preferences.get("board." + selectedBoard.getName() + ".port"));
            setNetworkPort(Preferences.get("board." + selectedBoard.getName() + ".ip"));
        } else {
            setSerialPort(null);
        }

        configFile = new PropertyFile(new File(folder, "sketch.cfg"));
        loadConfig();

        updateLibraryList();

        if(editor != null) {
            editor.fireEvent(UEvent.SKETCH_OPEN);
        }
    }

    public boolean loadFile(File f) {
        if(!f.exists()) {
            return false;
        }

        sketchFiles.add(f);
        return true;
    }

    public File getFileByName(String filename) {
        for(File f : sketchFiles) {
            if(f.getName().equals(filename)) {
                return f;
            }
        }

        return null;
    }

    public String[] getFileNames() {
        String[] out = new String[sketchFiles.size()];
        int i = 0;

        for(File f : sketchFiles) {
            out[i++] = f.getName();
        }

        Arrays.sort(out);
        return out;
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

    public File getMainFile() {
        File f = getFileByName(sketchName + ".ino");

        if(f == null) {
            f = getFileByName(sketchName + ".pde");
        }

        return f;
    }

    public String getMainFilePath() {
        File f = getMainFile();

        if(f == null) {
            return "";
        }

        return f.getAbsolutePath();
    }

    public boolean isModified() {
        // If we have no editor then it's impossible for a file
        // to be modified.
        if(editor == null) {
            return false;
        }

        boolean modified = false;
        int tabs = editor.getTabCount();

        for(int tab = 0; tab < tabs; tab++) {
            EditorBase ed = editor.getTab(tab);

            if(ed.isModified()) {
                modified = true;
            }
        }

        return modified;
    }

    static public boolean isSanitaryName(String name) {
        return sanitizeName(name).equals(name);
    }

    static public String sanitizeName(String origName) {
        char c[] = origName.toCharArray();
        StringBuffer buffer = new StringBuffer();

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


    // Get the content of a file.  If it's open in the editor then
    // return the content of the editor, otherwise load the data from
    // the file.
    public String getFileContent(File f) {
        return getFileContent(f, false);
    }
    public String getFileContent(File f, boolean forceOffDisk) {
        if(!f.exists()) {
            return "";
        }

        if(f.isDirectory()) {
            return "";
        }

        if(!forceOffDisk) {
            if(editor != null) {
                int tabNumber = editor.getTabByFile(f);

                if(tabNumber > -1) {
                    EditorBase eb = editor.getTab(tabNumber);
                    return eb.getText();
                }
            }
        }

        InputStream    fis;
        BufferedReader br;
        String         line;
        StringBuilder sb = new StringBuilder();

        try {

            fis = new FileInputStream(f);
            br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));

            while((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }

            br.close();
        } catch(Exception e) {
            error(e);
        }

        return sb.toString();
    }

    // Write the content of a file to a folder.
    public void writeFileToFolder(File file, File folder) {
        File dest = new File(folder, file.getName());

        try {
            PrintWriter out = new PrintWriter(dest);
            out.print(getFileContent(file));
            out.close();
        } catch(Exception e) {
            error(e);
        }
    }

    // Write out any locally cached copies of files (in editors)
    // to the disk.
    public void saveAllFiles() {
        // If we don't have an editor then we don't have any cached files.
        if(editor == null) {
            return;
        }

        for(File f : sketchFiles) {
            int tab = editor.getTabByFile(f);

            if(tab > -1) {
                EditorBase eb = editor.getTab(tab);

                if(eb.isModified()) {
                    eb.setModified(false);
                    writeFileToFolder(f, sketchFolder);
                }
            }
        }

        Base.updateMRU(sketchFolder);

        if(editor != null) {
            editor.updateMenus();
        }
    }

    /**************************************************************************
     * COMPILATION                                                            *
     *                                                                        *
     * This section deals with the compilation of the sketch.  It has all the *
     * routines needed to convert a sketch into C++ or C source code, and to  *
     * compile, link, and archive it all.                                     *
     **************************************************************************/

    TreeMap<File, String> cleanedFiles;

    public boolean cleanFiles() {
        cleanedFiles = new TreeMap<File, String>();

        for(File f : sketchFiles) {
            if(FileType.getType(f) == FileType.SKETCH) {
                String data = getFileContent(f);
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
                cleanedFiles.put(f, getFileContent(f));
            }
        }

        return true;
    }

    public int linesInString(String l) {
        return l.split("\n").length;
    }

    public String stripBlock(String in, String start, String end) {
        String regexp;
        String mid;

        if(start.equals(end)) {
            mid = start;
        } else {
            mid = start + end;
        }

        if(start == "{") start = "\\" + start;

        if(end == "}") end = "\\" + end;

        regexp = "(?s)" + start + "[^" + mid + "]*" + end;

        boolean done = false;
        String out = in;
        int pass = 1;

        while(!done) {
            String rep = out.replaceFirst(regexp, "");
            int lines = linesInString(out);
            int newLines = linesInString(rep);
            int rets = lines - newLines;
            String retStr = "";

            for(int i = 0; i < rets; i++) {
                retStr += "\n";
            }

            rep = out.replaceFirst(regexp, retStr);

            String[] left = out.split("\n");
            String[] right = out.split("\n");

            if(rep.equals(out)) {
                done = true;
            } else {
                out = rep;
            }

            pass++;
        }

        return out;
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
                        return new Library(testFile, sketchName, "all");
                    }
                }
            }
        }

        if (getCore() == null) {
            return null;
        }
        return Library.getLibraryByInclude(filename, getCore().getName());
    }

    public TreeSet<String> getAllFunctionNames() {
        TreeSet<String>funcs = new TreeSet<String>();
        for (HashMap<Integer, String> ent : functionList.values()) {
            funcs.addAll(ent.values());
        }
        return funcs;
    }

    public HashMap<Integer, String> getFunctionsForFile(File f) {
        return functionList.get(f);
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

    public void findAllFunctions() {
        if(editor == null) {
            return;
        }

        functionList = new HashMap<File, HashMap<Integer, String>>();
        String data;
        HashMap<Integer, String>funcs = null;

        for(File f : sketchFiles) {
            switch(FileType.getType(f)) {
            case FileType.SKETCH:
            case FileType.CSOURCE:
            case FileType.CPPSOURCE:
                data = getFileContent(f);
                data = stripComments(data);
                funcs = findFunctions(data);
                functionList.put(f, funcs);
                break;

            case FileType.ASMSOURCE:
                data = getFileContent(f);
                funcs = findLabels(data);
                functionList.put(f, funcs);
                break;
            }
        }
    }

    public HashMap<Integer, String> findFunctions(String in) {
        HashMap<Integer, String> funcs = new HashMap<Integer, String>();

        String out = in.replaceAll("\\\\.", "");

        out = out.replaceAll("'[^'\\n\\r]*'", "");
        out = out.replaceAll("\"[^\"\\n\\r]*\"", "");
        out = stripBlock(out, "{", "}");
        String[] s = out.split("\n");
        StringBuilder decimated = new StringBuilder();
        boolean continuation = false;
        int lineno = 0;

        for(String line : s) {
            line = line.trim();
            lineno++;

            if(line.equals("")) {
                continue;
            }

            if(line.startsWith("#")) {
                continue;
            }

            if(continuation) {
                decimated.append(line);

                if(line.endsWith(")")) {
                    continuation = false;
                    funcs.put(lineno - 1, decimated.toString());
                    decimated = new StringBuilder();
                }

                continue;
            }

            if(line.endsWith(";")) {
                continue;
            }

            if(line.indexOf("(") == -1) {
                continue;
            }

            Pattern p = Pattern.compile("[a-zA-Z0-9_\\*]+\\s+[a-zA-Z0-9_\\*]+\\s*\\(");
            Matcher m = p.matcher(line);

            if(m.find()) {
                decimated.append(line);

                if(!line.endsWith(")")) {
                    continuation = true;
                } else {
                    funcs.put(lineno - 1, decimated.toString());
                    decimated = new StringBuilder();
                }
            }
        }

        return funcs;
    }

    public static final int LIB_PENDING = 0;
    public static final int LIB_PROCESSED = 1;
    public static final int LIB_SYSTEM = 2;

    ArrayList<String> includeOrder = new ArrayList<String>();

    public synchronized void updateLibraryList() {
        cleanFiles();
        includeOrder = new ArrayList<String>();
        HashMap<String, Integer> inclist = new HashMap<String, Integer>();
        Pattern inc = Pattern.compile("^#\\s*include\\s+[<\"](.*)[>\"]");

        for(File f : cleanedFiles.keySet()) {
            String data = cleanedFiles.get(f);
            String lines[] = data.split("\n");

            for(String line : lines) {
                Matcher match = inc.matcher(line.trim());

                if(match.find()) {
                    inclist.put(match.group(1), LIB_PENDING);

                    if(includeOrder.indexOf(match.group(1)) == -1) {
                        includeOrder.add(match.group(1));
                    }
                }
            }
        }

        importedLibraries = new HashMap<String, Library>();
        unknownLibraries = new ArrayList<String>();

        int processed = 0;

        do {
            Thread.yield();
            HashMap<String, Integer> newinclist = new HashMap<String, Integer>();
            processed = 0;

            for(String incfile : inclist.keySet()) {
                if(inclist.get(incfile) == LIB_PROCESSED) {
                    newinclist.put(incfile, LIB_PROCESSED);
                    continue;
                }

                boolean inExisting = false;

                for (Library existing : importedLibraries.values()) {
                    if (existing.hasHeader(incfile)) {
                        inExisting = true;
                    }
                }
                if (inExisting) {
                    inclist.put(incfile, LIB_PROCESSED);
                    continue;
                }

                inclist.put(incfile, LIB_PROCESSED);
                Library lib = findLibrary(incfile);

                if(lib == null) {
                    newinclist.put(incfile, LIB_SYSTEM);
                    if (unknownLibraries.indexOf(incfile) == -1) {
                        unknownLibraries.add(incfile);
                    }
                    continue;
                }

                importedLibraries.put(lib.getName(), lib);
                newinclist.put(incfile, LIB_PROCESSED);
                ArrayList<String> req = lib.getRequiredLibraries();

                if (req != null) {
                    for(String r : req) {

                        if(inclist.get(r) == null) {
                            if(includeOrder.indexOf(r) == -1) {
                                includeOrder.add(r);
                            }

                            newinclist.put(r, LIB_PENDING);
                        } else {
                            newinclist.put(r, LIB_PROCESSED);
                        }
                    }

                    processed++;
                }
            }

            inclist.clear();

            for(String i : includeOrder) {
                Integer state = newinclist.get(i);

                if(state == null) {
                    continue;
                }

                inclist.put(i, state);
            }
        } while(processed != 0);

        if(editor != null) {
            editor.updateLibrariesTree();
        }
    }

    public synchronized boolean prepare() {
        PropertyFile props = ctx.getMerged();

        if(getBoard() == null) {
            error(Translate.w("You have no board selected.  You must select a board before you can compile your sketch.", 80, "\n"));
            return false;
        }

        if(getCore() == null) {
            error(Translate.w("You have no core selected.  You must select a core before you can compile your sketch.", 80, "\n"));
            return false;
        }

        if(Preferences.getBoolean("compiler.purge")) {
            cleanBuild();
        }

        updateLibraryList();

        if (Preferences.getBoolean("compiler.generate_makefile")) {
            if (props.get("makefile.template") != null) {
                generateMakefile();
            }
        }

        // We now have the data.  Now, if we're combining files, we shall do it
        // in this map.

        if(Preferences.getBoolean("compiler.combine_ino") || Base.cli.isSet("force-join-files")) {
            File mainFile = getMainFile();
            StringBuilder out = new StringBuilder();

            if(!Preferences.getBoolean("compiler.disableline")) out.append("#line 1 \"" + mainFile.getAbsolutePath().replaceAll("\\\\", "\\\\\\\\") + "\"\n");

            out.append(cleanedFiles.get(mainFile));

            for(String fn : getFileNames()) {
                File f = getFileByName(fn);

                if(FileType.getType(f) == FileType.SKETCH) {
                    if(f != mainFile) {
                        String data = cleanedFiles.get(f);

                        if(!Preferences.getBoolean("compiler.disableline")) out.append("#line 1 \"" + f.getAbsolutePath().replaceAll("\\\\", "\\\\\\\\") + "\"\n");

                        out.append(data);
                        cleanedFiles.remove(f);
                    }
                }
            }

            cleanedFiles.put(mainFile, out.toString());
        }

        // We now have a has of all the file data, with any ino or pde
        // files combined into the main file entry.  The old ino and pde
        // entries have been removed.  Now to parse any INO or PDE files
        // for prototypes etc and do the Arduino fudging.  We should
        // do it properly, really, looking for the first function in the
        // file and placing the prototypes directly before that.

        Pattern pragma = Pattern.compile("^#pragma\\s+parameter\\s+([^=]+)\\s*=\\s*(.*)\\s*$");
        Pattern paramsplit = Pattern.compile("(?:\"[^\"]*\"|[^\\s\"])+");
        parameters = new TreeMap<String, String>();

        for(File f : cleanedFiles.keySet()) {
            if(FileType.getType(f) == FileType.SKETCH) {
                HashMap<Integer, String> funcs = findFunctions(cleanedFiles.get(f));

                Integer ffLineNo = Integer.MAX_VALUE;
                String firstFunction = "";

                for(Integer i : funcs.keySet()) {
                    if(i < ffLineNo) {
                        ffLineNo = i;
                        firstFunction = funcs.get(i);
                    }
                }

                int line = 1;
                StringBuilder munged = new StringBuilder();

                for(String l : cleanedFiles.get(f).split("\n")) {
                    if(!Preferences.getBoolean("compiler.disable_prototypes")) {
                        if(l.trim().startsWith(firstFunction)) {
                            for(String func : funcs.values()) {
                                func = func.replaceAll("=[^,)]+", "");
                                munged.append(func + ";\n");
                            }

                            if(!Preferences.getBoolean("compiler.disableline")) munged.append("#line " + line + " \"" + f.getAbsolutePath().replaceAll("\\\\", "\\\\\\\\") + "\"\n");
                        }
                    }

                    Matcher mtch = pragma.matcher(l.trim());

                    if(mtch.find()) {
                        l = "// " + l;
                        Matcher part = paramsplit.matcher(mtch.group(2).trim());
                        String parms = "";

                        while(part.find()) {
                            if(parms.equals("") == false) {
                                parms += "::";
                            }

                            parms += part.group(0);
                        }

                        parameters.put(mtch.group(1), parms);
                    }

                    munged.append(l + "\n");

                    if(!l.startsWith("#line 1 ")) {
                        line++;
                    }

                    cleanedFiles.put(f, munged.toString());
                }
            }
        }

        // Now we have munged the files, let's dump them into files in the
        // build folder ready for compilation.

        try {
            for(File f : cleanedFiles.keySet()) {
                if(FileType.getType(f) == FileType.SKETCH) {
                    String ext = ctx.parseString(props.get("build.extension"));

                    if(ext == null) {
                        ext = "cpp";
                    }

                    String newFileName = f.getName().substring(0, f.getName().lastIndexOf(".")) + "." + ext;
                    File newFile = new File(buildFolder, newFileName);
                    PrintWriter pw = new PrintWriter(newFile);
                    pw.write("/***************************************************\n");
                    pw.write(" * ! ! ! !        I M P O R T A N T        ! ! ! ! *\n");
                    pw.write(" *                                                 *\n");
                    pw.write(" * THIS FILE IS AUTOMATICALLY GENERATED AND SHOULD *\n");
                    pw.write(" * NOT BE DIRECTLY EDITED. INSTEAD EDIT THE INO OR *\n");
                    pw.write(" * PDE FILE THIS FILE IS GENERATED FROM!!!         *\n");
                    pw.write(" ***************************************************/\n");
                    pw.write("\n");

                    if(props.get("core.header") != null) {
                        boolean gotHeader = false;
                        String hdr = ctx.parseString(props.get("core.header"));
                        String[] lines = cleanedFiles.get(f).split("\n");
                        Pattern inc = Pattern.compile("^#\\s*include\\s+[<\"](.*)[>\"]");
                        for (String l : lines) {
                            Matcher m = inc.matcher(l);
                            if (m.find()) {
                                if (m.group(1).equals(hdr)) {
                                    gotHeader = true;
                                }
                            }
                        }
                        
                        if (!gotHeader) {
                            pw.write("#include <" + hdr + ">\n");
                        }
                    }

                    if(!Preferences.getBoolean("compiler.combine_ino") || Base.cli.isSet("force-join-files")) {
                        if(!Preferences.getBoolean("compiler.disableline")) pw.write("#line 1 \"" + f.getAbsolutePath().replaceAll("\\\\", "\\\\\\\\") + "\"\n");
                    }

                    pw.write(cleanedFiles.get(f));
                    pw.close();
                } else {
                    File buildFile = new File(buildFolder, f.getName());
                    PrintWriter pw = new PrintWriter(buildFile);
                    pw.write("/***************************************************\n");
                    pw.write(" * ! ! ! !        I M P O R T A N T        ! ! ! ! *\n");
                    pw.write(" *                                                 *\n");
                    pw.write(" * THIS FILE IS AUTOMATICALLY GENERATED AND SHOULD *\n");
                    pw.write(" * NOT BE DIRECTLY EDITED. INSTEAD EDIT THE SOURCE *\n");
                    pw.write(" * FILE THIS FILE IS GENERATED FROM!!!             *\n");
                    pw.write(" ***************************************************/\n");

                    if(!Preferences.getBoolean("compiler.disableline")) pw.write("#line 1 \"" + f.getAbsolutePath().replaceAll("\\\\", "\\\\\\\\") + "\"\n");

                    pw.write(cleanedFiles.get(f));
                    pw.close();
                }
            }
        } catch(Exception e) {
            error(e);
        }

        if(editor != null) {
            editor.updateOutputTree();
        }

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

//            // If the last character was a \ then we'll just skip the next character since we
//            // really don't care what it is.
//            if (inEscape) {
//                out.append(thisChar);
//                cpos++;
//                inEscape = false;
//                continue;
//            }
//
//            // If we're not currently escaped and we get a \ then start escaping. Don't escape inside
//            // comments.
//            if (thisChar == '\\') {
//                out.append(thisChar);
//                if (!inSingleComment && !inMultiComment) {
//                    inEscape = true;
//                }
//                cpos++;
//                continue;
//            }

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

    public String[] gatherIncludes(File f) {
        String[] data = getFileContent(f).split("\n"); //stripComments(f.textArea.getText()).split("\n");
        ArrayList<String> includes = new ArrayList<String>();

        Pattern pragma = Pattern.compile("#pragma\\s+parameter\\s+([^=]+)\\s*=\\s*(.*)");

        for(String line : data) {
            line = line.trim();

            if(line.startsWith("#pragma")) {
                Matcher m = pragma.matcher(line);

                if(m.find()) {
                    String key = m.group(1);
                    String value = m.group(2);
                    String munged = "";

                    for(int i = 0; i < value.length(); i++) {

                        if(value.charAt(i) == '"') {
                            munged += '"';
                            i++;

                            while(value.charAt(i) != '"') {
                                munged += value.charAt(i++);
                            }

                            munged += '"';
                            continue;
                        }

                        if(value.charAt(i) == '\'') {
                            munged += '\'';
                            i++;

                            while(value.charAt(i) != '\'') {
                                munged += value.charAt(i++);
                            }

                            munged += '\'';
                            continue;
                        }

                        if(value.charAt(i) == ' ') {
                            munged += "::";
                            continue;
                        }

                        munged += value.charAt(i);
                    }

                    parameters.put(key, munged);
                }

                continue;
            }

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
                addLibraryToImportList(i);
            }
        }

        return includes.toArray(new String[includes.size()]);
    }

    public Library addLibraryToImportList(String filename) {
        String l = filename;

        if(filename.endsWith(".h")) {
            l = filename.substring(0, filename.lastIndexOf("."));
        }

        Library lib;

        // First, let's look for libraries included in the sketch folder itself.  Those should take priority over
        // every other library in the system.

        File sketchLibFolder = new File(sketchFolder, "libraries");

        if(sketchLibFolder.exists() && sketchLibFolder.isDirectory()) {
            File libFolder = new File(sketchLibFolder, l);

            if(libFolder.exists() && libFolder.isDirectory()) {
                File libHeader = new File(libFolder, l + ".h");

                if(libHeader.exists()) {
                    lib = new Library(libHeader, "sketch", "all");
                    importedLibraries.put(l, lib);
                    orderedLibraries.add(lib);

                    // And then work through all the required libraries and add them.
                    ArrayList<String> requiredLibraries = lib.getRequiredLibraries();

                    if (requiredLibraries != null) {
                        for(String req : requiredLibraries) {
                            addLibraryToImportList(req);
                        }
                    } else {
                    }

                    return lib;
                }
            }
        }

        lib = Library.getLibraryByInclude(filename, getCore().getName());

        if(lib == null) {
            // The library doesn't exist - either it's a system header or a library that isn't installed.
            return null;
        }

        // At this point we have a valid library that hasn't yet been imported.  Now to recurse.
        // First add the library to the imported list
        importedLibraries.put(l, lib);
        orderedLibraries.add(lib);

        // And then work through all the required libraries and add them.
        ArrayList<String> requiredLibraries = lib.getRequiredLibraries();

        for(String req : requiredLibraries) {
            addLibraryToImportList(req);
        }

        return lib;
    }

    public boolean upload() {
        ctx.executeKey("upload.precmd");
        boolean ret = programFile(getProgrammer(), sketchName);
        ctx.executeKey("upload.postcmd");
        return ret;
    }

    public boolean performSerialReset(boolean dtr, boolean rts, int speed) {
        try {
            SerialPort serialPort = Serial.requestPort(getSerialPort(), speed);

            if(serialPort == null) {
                error("Unable to lock serial port for board reset");
                return false;
            }

            bullet("Resetting board...");
            serialPort.setDTR(dtr);
            serialPort.setRTS(dtr);
            Thread.sleep(1000);
            serialPort.setDTR(false);
            serialPort.setRTS(false);
            Serial.closePort(serialPort);
            System.gc();
        } catch(Exception e) {
            error(e);
            return false;
        }

        System.gc();
        return true;
    }

    public boolean performBaudBasedReset(int b) {
        try {
            SerialPort serialPort = Serial.requestPort(getSerialPort(), b);

            if(serialPort == null) {
                error("Unable to lock serial port");
                return false;
            }

            Thread.sleep(1000);
            Serial.closePort(serialPort);
            System.gc();
            Thread.sleep(1500);
        } catch(Exception e) {
            error(e);
            return false;
        }

        return true;
    }

    public boolean build() {
        if(Preferences.getBoolean("editor.external.command")) {
            //reloadAllFiles();
        }

        if(getBoard() == null) {
            error(Translate.w("The sketch cannot be compiled: You have no board selected. If you haven't yet installed any boards please do so through the Plugin Manager.", 80, "\n"));
            return false;
        }

        if(getCore() == null) {
            error(Translate.w("The sketch cannot be compiled: You have no core selected. If you haven't yet installed a core please do so through the Plugin Manager.", 80, "\n"));
            return false;
        }

        if(getCompiler() == null) {
            error(Translate.w("The sketch cannot be compiled: The compiler for the selected core is not available. Please ensure the compiler is installed using the Plugin Manager.", 80, "\n"));
            return false;
        }

        heading("Compiling...");

        try {
            if(!prepare()) {
                error(Translate.t("Compile Failed"));
                setCompilingProgress(0);
                return false;
            }
        } catch(Exception e) {
            error(e);
            setCompilingProgress(0);
            return false;
        }

        boolean done = compile();
        setCompilingProgress(0);
        return done;
    }

    public boolean saveAs(File newPath) {
        if(newPath.exists()) {
            return false;
        }

        Debug.message("Save as " + newPath.getAbsolutePath());
        newPath.mkdirs();
        File newMainFile = new File(newPath, newPath.getName() + ".ino");
        File oldMainFile = getMainFile();

        // First let's copy the contents of the existing sketch folder, renaming the main file.

        File[] files = sketchFolder.listFiles();

        Base.removeDescendants(buildFolder);

        for(File f : files) {
            if(f.equals(oldMainFile)) {
                Debug.message("Copy main file " + f.getAbsolutePath() + " to " + newMainFile.getAbsolutePath());
                Base.copyFile(f, newMainFile);
                continue;
            }

            File dest = new File(newPath, f.getName());

            if(f.isDirectory()) {
                Base.copyDir(f, dest);
                Debug.message("Copy dir " + f.getAbsolutePath() + " to " + dest.getAbsolutePath());
                continue;
            }

            Debug.message("Copy file " + f.getAbsolutePath() + " to " + dest.getAbsolutePath());
            Base.copyFile(f, dest);
        }

        String oldPrefix = sketchFolder.getAbsolutePath();
        Debug.message("Old prefix: " + oldPrefix);
        sketchFolder = newPath;
        sketchName = newPath.getName();
        Debug.message("Sketch name: " + sketchName);
        isUntitled = false;
        // Now we can shuffle the files around in the sketchFiles array.
        // We want to try and keep the indexes in the same order if that
        // is possible, so we'll use a numeric iterator.

        ArrayList<File> newSketchFiles = new ArrayList<File>();

        for(int i = 0; i < sketchFiles.size(); i++) {
            File sf = sketchFiles.get(i);

            if(sf.equals(oldMainFile)) {
                newSketchFiles.add(i, newMainFile);

                if(editor != null) {
                    int tab = editor.getTabByFile(oldMainFile);

                    if(tab > -1) {
                        TabLabel tl = editor.getTabLabel(tab);
                        tl.setFile(newMainFile);
                    }
                }
            } else {
                File newFile = new File(newPath, sf.getName());
                Debug.message("New file name " + newFile.getAbsolutePath());
                newSketchFiles.add(i, newFile);

                if(editor != null) {
                    int tab = editor.getTabByFile(sf);

                    if(tab > -1) {
                        TabLabel tl = editor.getTabLabel(tab);
                        tl.setFile(newFile);
                    }
                }
            }
        }

        sketchFiles = newSketchFiles;

        buildFolder = createBuildFolder();

        // Now we have reconstructed the sketch in a new location we can save any
        // changed files from the editor.

        if(editor != null) {
            // Now step through the files looking for any that are open, change the tab's file pointer
            // and save the data.
            for(int tab = 0; tab < editor.getTabCount(); tab++) {
                TabLabel tl = editor.getTabLabel(tab);
                tl.save();
            }

            editor.updateTree();
            editor.setTitle(Base.theme.get("product.cap") + " | " + sketchName);
        }

        save();
        return true;
    }

    public boolean save() {
        // We can't really save it if it's untitled - there's no point.
        if(isUntitled()) {
            return false;
        }

        // Same if it's an example in a protected area:
        if(parentIsProtected()) {
            return false;
        }

        if(Preferences.getBoolean("editor.save.version")) {
            int numToSave = Preferences.getInteger("editor.save.version_num");
            File versionsFolder = new File(sketchFolder, "backup");

            if(!versionsFolder.exists()) {
                versionsFolder.mkdirs();
            }

            // Prune the oldest version if it exists
            File last = new File(versionsFolder, sketchName + "-" + numToSave);

            if(last.exists()) {
                Debug.message("Deleting " + last.getAbsolutePath());
                last.delete();
            }

            for(int i = numToSave - 1; i >= 1; i--) {
                File low = new File(versionsFolder, sketchName + "-" + i);
                File high = new File(versionsFolder, sketchName + "-" + (i + 1));

                if(low.exists()) {
                    Debug.message("Shuffling " + low.getAbsolutePath() + " to " + high.getAbsolutePath());
                    low.renameTo(high);
                }
            }

            File bottom = new File(versionsFolder, sketchName + "-1");
            Debug.message("Backing up as " + bottom.getAbsolutePath());
            bottom.mkdirs();

            for(File f : sketchFiles) {
                File to = new File(bottom, f.getName());
                Debug.message("    Backing up " + f.getAbsolutePath() + " to " + to.getAbsolutePath());
                Base.copyFile(f, to);
            }
        }

        saveAllFiles();
        Debug.message("All saved");
        saveConfig();
        return true;
    }

    public void saveConfig() {
        if(isUntitled()) {
            return;
        }

        if(parentIsProtected()) {
            return;
        }

        if(configFile.size() > 0) {
            Debug.message("Saving config");
            configFile.save();
        }
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
        libFiles.add(getBoard().getFolder().getAbsolutePath());
        libFiles.add(getCore().getLibrariesFolder().getAbsolutePath());

        for(String key : props.childKeysOf("compiler.library")) {
            String coreLibName = key.substring(17);
            String libPaths = ctx.parseString(props.get(key));

            if(libPaths != null && !(libPaths.trim().equals(""))) {
                String[] libPathsArray = libPaths.split("::");

                for(String p : libPathsArray) {
                    File f = new File(getCompiler().getFolder(), p);

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
                    File f = new File(getCore().getFolder(), p);

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
                    File f = new File(getBoard().getFolder(), p);

                    if(f.exists()) {
                        libFiles.add(f.getAbsolutePath());
                    }
                }
            }
        }

        for(Library l : getOrderedLibraries()) {
            libFiles.add(l.getFolder().getAbsolutePath());
        }

        return libFiles;
    }

    public void checkForSettings() {
        File mainFile = getMainFile();

        Pattern p = Pattern.compile("^#pragma\\s+parameter\\s+([^\\s]+)\\s*=\\s*(.*)$");

        String[] data = getFileContent(mainFile).split("\n");

        for(String line : data) {
            line = line.trim();
            Matcher m = p.matcher(line);

            if(m.find()) {
                String key = m.group(1);
                String value = m.group(2);

                if(key.equals("board")) {
                    setBoard(value);
                }
            }
        }
    }

    public File getCode(int i) {
        return sketchFiles.get(i);
    }

    public File getBuildFolder() {
        return buildFolder;
    }

    public String getBuildPath() {
        return buildFolder.getAbsolutePath();
    }

    public void cleanup() {
        System.gc();
        Base.removeDescendants(buildFolder);
    }

    public void setCompilingProgress(int percent) {
        if(editor != null) {
            editor.setProgress(percent);
        }
    }

    public boolean isReadOnly() {

        if(isInternal()) {
            return true;
        }

        File testFile = new File(sketchFolder, ".testWrite");
        boolean canWrite = false;

        try {
            testFile.createNewFile();

            if(testFile.exists()) {
                testFile.delete();
                canWrite = true;
            }
        } catch(Exception e) {
            return true;
        }

        if(!canWrite) {
            return true;
        }

        canWrite = true;

        for(File c : sketchFiles) {
            if(!c.canWrite()) {
                canWrite = false;
            }
        }

        return !canWrite;
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

        for(String lib : includeOrder) {

            // First look to see if the header is already in a library we have included thus far
            boolean inExisting = false;

            for (Library existing : importedLibraries.values()) {
                if (existing.hasHeader(lib)) {
                    ArrayList<File> lf = existing.getIncludeFolders(this);
                    if (includes.indexOf(lf.get(0)) != -1) {
                        inExisting = true;
                    }
                }
            }
            if (inExisting) {
                continue;
            }

            Library l = getSketchLibrary(lib);

            if (l == null) {
                l = Library.getLibraryByInclude(lib, getCore().getName());
            }

            if(l != null) {
                for (File lf : l.getIncludeFolders(this)) {
                    if (includes.indexOf(lf) == -1) {
                        includes.add(lf);
                    }
                }
            }
        }

        includes.add(getBoard().getFolder());
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
                    File f = new File(getCompiler().getFolder(), p);

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
                    File f = new File(getCore().getFolder(), p);

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
                    File f = new File(getBoard().getFolder(), p);

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
            bullet2("Downloading " + pkg.getName());
            pkg.fetchPackage(Base.getDataFile("apt/cache"));
        
            bullet2("Installing " + pkg.getName());
            pkg.extractPackage(Base.getDataFile("apt/cache"), Base.getDataFile("apt/db/packages"), Base.getDataFolder());
        }
    }

    public boolean compile() {
        
        long startTime = System.currentTimeMillis();

        PropertyFile props = ctx.getMerged();
        clearLineComments();

        if (props.getBoolean("purge")) {
            doPrePurge = true;
        }

        ctx.executeKey("compile.precmd");

        if(editor != null) {
            for(int i = 0; i < editor.getTabCount(); i++) {
                EditorBase eb = editor.getTab(i);

                if(eb != null) {
                    eb.clearHighlights();
                    eb.removeFlagGroup(0x1000); // Error flags
                    eb.removeFlagGroup(0x1001); // Warning flags
                }
            }
        }

        // Rewritten compilation system from the ground up.

        // Step one, we need to build a generic set of includes:

        ctx.set("includes", generateIncludes());
        ctx.set("filename", sketchName);

        if ((Preferences.getBoolean("editor.dialog.missinglibs")) && (Base.isOnline())) {
            if (editor != null) {
                if (unknownLibraries.size() > 0) {
                    PluginManager pm = new PluginManager();
                    HashMap<String, Package> foundPackages = new HashMap<String, Package>();
                    for (String l : unknownLibraries) {
                        File sketchHeader = new File(sketchFolder, l);
                        if (!sketchHeader.exists()) {
                            Package p = pm.findLibraryByInclude(getCore(), l);
                            if (p != null) {
                                foundPackages.put(l, p);
                            }
                        }
                    }

                    if (foundPackages.size() > 0) {
                        JPanel panel = new JPanel();
                        panel.setLayout(new GridBagLayout());
                        GridBagConstraints c = new GridBagConstraints();

                        c.gridwidth = 2;
                        c.gridx = 0;
                        c.gridy = 0;
                        c.fill = GridBagConstraints.HORIZONTAL;
                        c.anchor = GridBagConstraints.LINE_START;

                        panel.add(new JLabel("I have identified some libraries you may be missing:"), c);
                        
                        c.gridy++;
                        panel.add(new JLabel(" "), c);

                        c.gridwidth = 1;

                        for(String l : foundPackages.keySet()) {
                            c.gridx = 0;
                            c.gridy++;
                            panel.add(new JLabel(l + ": "), c);
                            c.gridx = 1;
                            panel.add(new JLabel(foundPackages.get(l).getName()), c);
                        }
                        c.gridx = 0;
                        c.gridy++;
                        c.gridwidth = 2;
                        panel.add(new JLabel(" "), c);

                        c.gridy++;
                        panel.add(new JLabel("Do you want me to install these libraries for you?"), c);

                        JCheckBox cb = new JCheckBox("Never ask me again");
                        Object[] options = {cb, "Yes", "No"};
                        int n = JOptionPane.showOptionDialog(editor, panel, "Missing Libraries", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);

                        if (cb.isSelected()) {
                            Preferences.setBoolean("editor.dialog.missinglibs", false);
                        }

                        if (n == 1) {
                            bullet("Updating repository information...");
                            APT apt = pm.getApt();
                            apt.update();
                            bullet("Installing missing libraries...");
                            for (Package p : foundPackages.values()) {
                                installLibrary(pm.getApt(), p, true);
                            }
                            Base.rescanLibraries();
                            ctx.set("includes", generateIncludes());
                        }
                    }
                }
            }
        }

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
            libPaths += lib.getFolder().getAbsolutePath();
            libNames += lib.getName();
            ctx.set("library." + lib.getName() + ".path", lib.getFolder().getAbsolutePath());
        }

        ctx.set("library.paths", libPaths);
        ctx.set("library.names", libNames);

        ctx.set("build.path", buildFolder.getAbsolutePath());


        if (props.keyExists("compile.script")) {
            return ctx.executeKey("compile.script");
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
                File src = new File(getCompiler().getFolder(), cf);

                if(!src.exists()) {
                    src = new File(getCore().getFolder(), cf);
                }

                if(!src.exists()) {
                    src = new File(getBoard().getFolder(), cf);
                }

                if(src.exists()) {
                    File dest = new File(buildFolder, src.getName());
                    Base.copyFile(src, dest);
                    Debug.message("    ... ok");
                } else {
                    Debug.message("    ... not found");
                }
            }
        }

        bullet("Compiling sketch...");
        setCompilingProgress(10);
        ArrayList<File>sketchObjects = compileSketch();

        if(sketchObjects == null) {
            error("Failed compiling sketch");
            return false;
        }

        bullet("Compiling core...");
        setCompilingProgress(20);

        if(!compileCore()) {
            error("Failed compiling core");
            return false;
        }

        setCompilingProgress(30);

        bullet("Compiling libraries...");

        if(!compileLibraries()) {
            error("Failed compiling libraries");
            return false;
        }

        setCompilingProgress(40);

        bullet("Linking sketch...");

        if(!compileLink(sketchObjects)) {
            error("Failed linking sketch");
            return false;
        }

        setCompilingProgress(50);

        if(!compileEEP()) {
            error("Failed extracting EEPROM image");
            return false;
        }

        setCompilingProgress(60);


        if(props.get("compile.lss") != null) {
            if(Preferences.getBoolean("compiler.generate_lss")) {
                File redirectTo = new File(buildFolder, sketchName + ".lss");

                if(redirectTo.exists()) {
                    redirectTo.delete();
                }

                boolean result = false;

                String output = "";

                try {
                    ctx.startBuffer();
                    result = compileLSS();
                    output = ctx.endBuffer();
                    PrintWriter pw = new PrintWriter(redirectTo);
                    pw.print(output);
                    pw.close();

                } catch(Exception e) {
                    result = false;
                }

                if(!result) {
                    error("Failed generating listing");
                    return false;
                }

                if(Preferences.getBoolean("compiler.save_lss") && !parentIsProtected()) {
                    try {
                        Base.copyFile(new File(buildFolder, sketchName + ".lss"), new File(sketchFolder, sketchName + ".lss"));

                        if(editor != null) {
                            editor.updateFilesTree();
                        }
                    } catch(Exception e) {
                        error(e);
                    }
                }
            }

        }

        setCompilingProgress(70);

        if(!compileHEX()) {
            error("Failed converting to HEX file");
            return false;
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
                Base.copyFile(new File(buildFolder, sketchName + exeSuffix), dest);
                if (dest.exists()) {
                    dest.setExecutable(true);
                }
                

                if(editor != null) {
                    editor.updateFilesTree();
                }
            } catch(Exception e) {
                error(e);
            }
        }


        heading("Compiling done.");
        setCompilingProgress(100);

        if(editor != null) {
            editor.updateOutputTree();
        }


        compileSize();

        long endTime = System.currentTimeMillis();
        double compileTime = (double)(endTime - startTime) / 1000d;
        bullet("Compilation took " + compileTime + " seconds");
        ctx.executeKey("compile.postcmd");
        return true;
    }

    public boolean compileSize() {
        PropertyFile props = ctx.getMerged();

        if (props.get("compile.size") != null) {
            heading("Memory usage");

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
            bullet("Program size: " + (textSize + dataSize + rodataSize) + " bytes");
            bullet("Memory size: " + (bssSize + dataSize) + " bytes");
        }
        return true;
    }

    public boolean compileLibraries() {
        for(String lib : importedLibraries.keySet()) {
            if(!compileLibrary(importedLibraries.get(lib))) {
                return false;
            }
        }

        return true;
    }

    private File compileFile(File src) {
        return compileFile(src, buildFolder);
    }

    private File compileFile(File src, File fileBuildFolder) {

        String fileName = src.getName();
        String recipe = null;

        if(terminateExecution) {
            terminateExecution = false;
            error("Compilation terminated");
            return null;
        }

        PropertyFile props = ctx.getMerged();

        if(fileName.endsWith(".cpp")) {
            recipe = "compile.cpp";
        }

        if(fileName.endsWith(".cxx")) {
            recipe = "compile.cpp";
        }

        if(fileName.endsWith(".cc")) {
            recipe = "compile.cpp";
        }

        if(fileName.endsWith(".c")) {
            recipe = "compile.c";
        }

        if(fileName.endsWith(".S")) {
            recipe = "compile.S";
        }

        if(recipe == null) {
            error("Error: I don't know how to compile " + fileName);
            return null;
        }

        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        String objExt = ctx.parseString(props.get("compiler.object","o"));
        File dest = new File(fileBuildFolder, fileName + "." +objExt);

        if(dest.exists()) {
            if(dest.lastModified() > src.lastModified()) {
                return dest;
            }
        }

        ctx.set("build.path", fileBuildFolder.getAbsolutePath());
        ctx.set("source.name", src.getAbsolutePath());
        ctx.set("object.name", dest.getAbsolutePath());

        if(!ctx.executeKey(recipe)) {
            return null;
        }

        if(!dest.exists()) {
            return null;
        }

        if(editor != null) {
            editor.updateOutputTree();
        }

        return dest;
    }

    public File getCacheFolder() {
        File cacheRoot = Base.getCacheFolder();
        Core c = getCore();
        File boardCache = new File(cacheRoot, "unknownCache");
        if (c != null) {
            Board b = getBoard();
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

    public boolean compileCore() {
        TreeMap<String, ArrayList<File>> coreLibs = getCoreLibs();
        PropertyFile props = ctx.getMerged();

        if (props.get("compile.stub") != null) {
            String mainStub = ctx.parseString(props.get("compile.stub"));
            String[] bits = mainStub.split("::");
            for (String stubFile : bits) {
                File mainStubFile = new File(stubFile);
                if (mainStubFile.exists()) {
                    File mainStubObject = compileFile(mainStubFile);
                    File cachedStubObject = getCacheFile(mainStubObject.getName());
                    if (mainStubObject.exists()) {
                        Base.copyFile(mainStubObject, cachedStubObject);
                        mainStubObject.delete();
                    }
                }
            }
        }

        for(String lib : coreLibs.keySet()) {
            bullet2(lib.toString());

            if(!compileCore(coreLibs.get(lib), "Core_" + lib)) {
                return false;
            }
        }

        return true;
    }

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

        ArrayList<File> fileList = new ArrayList<File>();

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
                File out = compileFile(f, coreBuildFolder);

                if(out == null) {
                    coreBuildFolder.delete();
                    return false;
                }

                ctx.set("object.name", out.getAbsolutePath());
                boolean ok = ctx.executeKey("compile.ar");

                if(!ok) {
                    out.delete();
                    coreBuildFolder.delete();
                    return false;
                }

                out.delete();
            }
        }

        coreBuildFolder.delete();
        return true;
    }

    public void put(String k, String v) {
        ctx.set(k, v);
    }

    public String get(String k) {
        return ctx.get(k);
    }

    public String getArchiveName(Library lib) {
        PropertyFile props = ctx.getMerged();
        String prefix = ctx.parseString(props.get("compiler.library.prefix","lib"));
        String suffix = ctx.parseString(props.get("compiler.library", "a"));
        return prefix + lib.getLinkName() + "." + suffix;
    }

    public boolean compileLibrary(Library lib) {
        File archive = getCacheFile(getArchiveName(lib));  //getCacheFile("lib" + lib.getName() + ".a");
        File utility = lib.getUtilityFolder();
        PropertyFile props = ctx.getMerged();
        bullet2(lib.toString());

        ctx.set("library", archive.getAbsolutePath());

        long archiveDate = 0;

        if(archive.exists()) {
            archiveDate = archive.lastModified();
        }

        File libBuildFolder = new File(buildFolder, "lib" + lib.getLinkName());
        libBuildFolder.mkdirs();

        ArrayList<File> fileList = lib.getSourceFiles(this);

        String origIncs = ctx.get("includes");
        ctx.set("includes", origIncs + "::" + "-I" + utility.getAbsolutePath());

        int fileCount = fileList.size();

        int count = 0;

        for(File f : fileList) {
            if(f.lastModified() > archiveDate) {
                File out = compileFile(f, libBuildFolder);

                if(out == null) {
                    purgeLibrary(lib);
                    lib.setCompiledPercent(0);

                    if(editor != null) {
                        editor.updateLibrariesTree();
                    }

                    libBuildFolder.delete();
                    return false;
                }

                ctx.set("object.name", out.getAbsolutePath());
                boolean ok = ctx.executeKey("compile.ar");

                if(!ok) {
                    purgeLibrary(lib);
                    lib.setCompiledPercent(0);

                    if(editor != null) {
                        editor.updateLibrariesTree();
                    }

                    out.delete();
                    libBuildFolder.delete();
                    return false;
                }

                count++;
                lib.setCompiledPercent(count * 100 / fileCount);

                if(editor != null) {
                    editor.updateLibrariesTree();
                }

                out.delete();
            }
        }

        if(editor != null) {
            editor.updateOutputTree();
        }

        ctx.set("includes", origIncs);
        lib.setCompiledPercent(100);

        if(editor != null) {
            editor.updateLibrariesTree();
        }

        libBuildFolder.delete();

        return true;
    }

    private ArrayList<File> convertFiles(File dest, ArrayList<File> sources) {
        ArrayList<File> objectPaths = new ArrayList<File>();
        PropertyFile props = ctx.getMerged();

        ctx.set("build.path", dest.getAbsolutePath());
        String objExt = ctx.parseString(props.get("compiler.object","o"));

        for(File file : sources) {
            File objectFile = new File(dest, file.getName() + "." + objExt);
            objectPaths.add(objectFile);

            if(objectFile.exists() && objectFile.lastModified() > file.lastModified()) {
                if(Preferences.getBoolean("compiler.verbose_compile")) {
                    bullet2("Skipping " + file.getAbsolutePath() + " as not modified.");
                }

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
            Base.copyFile(file, destFile);

            ctx.set("source.name", sp);
            ctx.set("object.name", objectFile.getAbsolutePath());

            if(!ctx.executeKey("compile.bin"))
                return null;

            if(!objectFile.exists())
                return null;
        }

        return objectPaths;
    }

    private ArrayList<File> compileFileList(File dest, ArrayList<File> sources, String key) {
        ArrayList<File> objectPaths = new ArrayList<File>();
        PropertyFile props = ctx.getMerged();

        ctx.set("build.path", dest.getAbsolutePath());
        String objExt = ctx.parseString(props.get("compiler.object","o"));

        for(File file : sources) {
            String fileName = file.getName();
            String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
            File objectFile = new File(dest, fileName + "." + objExt);
            objectPaths.add(objectFile);

            ctx.set("source.name", file.getAbsolutePath());
            ctx.set("object.name", objectFile.getAbsolutePath());

            if(objectFile.exists() && objectFile.lastModified() > file.lastModified()) {
                if(Preferences.getBoolean("compiler.verbose_compile")) {
                    bullet2("Skipping " + file.getAbsolutePath() + " as not modified.");
                }

                continue;
            }

            if(!ctx.executeKey(key))
                return null;

            if(!objectFile.exists())
                return null;
        }
        return objectPaths;
    }

    private ArrayList<File> compileFiles(File dest, ArrayList<File> sSources, ArrayList<File> cSources, ArrayList<File> cppSources) {

        ArrayList<File> objectPaths = new ArrayList<File>();
        PropertyFile props = ctx.getMerged();

        ctx.set("build.path", dest.getAbsolutePath());
        String objExt = ctx.parseString(props.get("compiler.object","o"));

        ArrayList<File> sObjects = compileFileList(dest, sSources, "compile.S");
        if (sObjects == null) { return null; }

        ArrayList<File> cObjects = compileFileList(dest, cSources, "compile.c");
        if (cObjects == null) { return null; }

        ArrayList<File> cppObjects = compileFileList(dest, cppSources, "compile.cpp");
        if (cppObjects == null) { return null; }

        objectPaths.addAll(sObjects);
        objectPaths.addAll(cObjects);
        objectPaths.addAll(cppObjects);

        return objectPaths;
    }

    private ArrayList<File> compileSketch() {
        ArrayList<File> sf = new ArrayList<File>();

        PropertyFile props = ctx.getMerged();

        // We can only do this if the core supports binary conversions
        if(props.get("compile.bin") != null) {
            File obj = new File(sketchFolder, "objects");

            if(obj.exists()) {
                File buf = new File(buildFolder, "objects");
                buf.mkdirs();
                ArrayList<File> uf = convertFiles(buildFolder, findFilesInFolder(obj, null, true));

                if(uf != null) {
                    sf.addAll(uf);
                }
            }
        }

        ArrayList<File> compiledFiles = compileFiles(
                                            buildFolder,
                                            findFilesInFolder(buildFolder, "S", false),
                                            findFilesInFolder(buildFolder, "c", false),
                                            findFilesInFolder(buildFolder, "cpp", false)
                                        );

        if(compiledFiles != null) {
            sf.addAll(compiledFiles);
        } else {
            return null;
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
                        File f = new File(getBoard().getFolder(), bf);

                        if(f.exists()) {
                            cFiles.add(f);
                        }
                    } else if(bf.endsWith(".cpp")) {
                        File f = new File(getBoard().getFolder(), bf);

                        if(f.exists()) {
                            cppFiles.add(f);
                        }
                    } else if(bf.endsWith(".S")) {
                        File f = new File(getBoard().getFolder(), bf);

                        if(f.exists()) {
                            sFiles.add(f);
                        }
                    }
                }

                sf.addAll(compileFiles(buildFolder, sFiles, cFiles, cppFiles));
            }
        }

        File suf = new File(sketchFolder, "utility");

        if(suf.exists()) {
            File buf = new File(buildFolder, "utility");
            buf.mkdirs();
            ArrayList<File> uf = compileFiles(
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

    static private boolean createFolder(File folder) {
        if(folder.isDirectory())
            return false;

        if(!folder.mkdir())
            return false;

        return true;
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

        neverInclude.replaceAll(" ", "::");
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

        return ctx.executeKey("compile.link");
    }

    private boolean compileEEP() {
        PropertyFile props = ctx.getMerged();
        if (!props.keyExists("compile.eep")) {
            return true;
        }
        return ctx.executeKey("compile.eep");
    }

    private boolean compileLSS() {
        PropertyFile props = ctx.getMerged();
        if (!props.keyExists("compile.lss")) {
            return true;
        }
        return ctx.executeKey("compile.lss");
    }

    private boolean compileHEX() {
        PropertyFile props = ctx.getMerged();
        if (!props.keyExists("compile.hex")) {
            return true;
        }
        return ctx.executeKey("compile.hex");
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
        String corePath = getCore().getFolder().getAbsolutePath() + File.separator;
        String boardPath = getBoard().getFolder().getAbsolutePath() + File.separator;

        if(path.startsWith(basePath)) return true;

        if(path.startsWith(cachePath)) return true;

        if(path.startsWith(corePath)) return true;

        if(path.startsWith(boardPath)) return true;

        return false;
    }

    public void cleanBuild() {
        System.gc();
        Base.removeDescendants(buildFolder);
    }

    public void about() {
        bullet("Sketch folder: " + sketchFolder.getAbsolutePath());
        bullet("Selected board: " + getBoard().getName());
        bullet("Board folder: " + getBoard().getFolder().getAbsolutePath());
    }

    public File getLibrariesFolder() {
        return new File(sketchFolder, "libraries");
    }

    public HashMap<String, Library> getLibraries() {
        return importedLibraries;
    }

    public boolean programFile(String programmer, String file) {
        PropertyFile props = ctx.getMerged();
        heading("Uploading firmware...");

        ctx.set("filename", file);

        if (props.get("upload." + programmer + ".message") != null) {
            message(ctx.parseString(props.get("upload." + programmer + ".message")));
        }

        if(props.get("upload." + programmer + ".using") != null) {
            if(props.get("upload." + programmer + ".using").equals("script")) {
                return ctx.executeKey("upload." + programmer + ".script");
            }
        }

        String cmdKey = null;

        if(props.keyExists("upload." + programmer + ".command")) {
            cmdKey = "upload." + programmer + ".command";
        } else if(props.keyExists("upload." + programmer + ".script")) {
            cmdKey = "upload." + programmer + ".script";
        }

        if(cmdKey == null) {
            error("Unable to get a suitable upload command");
            return false;
        }

        String uploadType = props.get("upload." + programmer + ".using");

        if(uploadType == null) uploadType = "serial";

        int progbaud = 9600;

        if(uploadType.equals("serial")) {

            boolean dtr = props.getBoolean("upload." + programmer + ".dtr");
            boolean rts = props.getBoolean("upload." + programmer + ".rts");

            String br = props.get("upload.speed");

            if(br != null) {
                br = ctx.parseString(br);
                try {
                    progbaud = Integer.parseInt(br);
                } catch(Exception e) {
                    progbaud = 115200;
                }
            }

            if (!performSerialReset(dtr, rts, progbaud)) {
                return false;
            }
        } else if(uploadType.equals("usbcdc")) {
            int baud = props.getInteger("upload." + programmer + ".reset.baud");
            if (!performBaudBasedReset(baud)) {
                return false;
            }
        }

        percentageFilter = props.get("upload." + programmer + ".percent");
        try {
            percentageMultiplier = Float.parseFloat(props.get("upload." + programmer + ".percent.multiply"));
        } catch (Exception ee) {
            percentageMultiplier = 1.0f;
        }

        bullet("Uploading...");
        
        boolean res = ctx.executeKey(cmdKey);
        percentageFilter = null;

        if(uploadType.equals("serial")) {

            boolean dtr = props.getBoolean("upload." + programmer + ".dtr");
            boolean rts = props.getBoolean("upload." + programmer + ".rts");

            if (!performSerialReset(dtr, rts, progbaud)) {
                return false;
            }
        }

        if(res) {
            bullet("Upload Complete");
            return true;
        } else {
            error("Upload Failed");
            return false;
        }
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

    public void flagError(String s) {
        if(editor == null) {
            return;
        }

        String theme = Preferences.get("theme.editor");
        theme = "theme." + theme + ".";
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
                        int tabNumber = editor.getTabByFile(errorFile);

                        if(tabNumber > -1) {
                            EditorBase eb = editor.getTab(tabNumber);
                            eb.highlightLine(errorLineNumber - 1, Base.theme.getColor(theme + "editor.compile.error.bgcolor"));
                            eb.flagLine(errorLineNumber - 1, Base.getIcon("flags", "fixme", 16), 0x1000);
                        }

                        link("uecide://error/" + errorLineNumber + "/" + errorFile.getAbsolutePath() + "|Error at line " + errorLineNumber + " in file " + errorFile.getName());

                        setLineComment(errorFile, errorLineNumber, eMat.group(eMessage));
                    }
                } catch(Exception e) {
                    Base.error(e);
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

                    if(warningFile != null) {
                        int tabNumber = editor.getTabByFile(warningFile);

                        if(tabNumber > -1) {
                            EditorBase eb = editor.getTab(tabNumber);
                            eb.highlightLine(warningLineNumber - 1, Base.theme.getColor(theme + "editor.compile.warning.bgcolor"));
                            eb.flagLine(warningLineNumber - 1, Base.getIcon("flags", "todo", 16), 0x1001);
                            link("uecide://error/" + warningLineNumber + "/" + warningFile.getAbsolutePath() + "|Warning at line " + warningLineNumber + " in file " + warningFile.getName());
                        }

                        setLineComment(warningFile, warningLineNumber, wMat.group(wMessage));
                    }
                } catch(Exception e) {
                    Base.error(e);
                }
            }
        }
    }

    String mBuffer = "";
    public void messageStream(String msg) {
        if(editor != null) {
            editor.messageStream(msg);
            return;
        }

        mBuffer += msg;
        int nlpos = mBuffer.lastIndexOf("\n");

        if(nlpos == -1) {
            nlpos = mBuffer.lastIndexOf("\r");
        }
        if(nlpos == -1) {
            return;
        }

        mBuffer.replaceAll("\\r\\n", "\\n");
        mBuffer.replaceAll("\\r", "\\n");

        boolean eol = false;

        if(mBuffer.endsWith("\n")) {
            mBuffer = mBuffer.substring(0, mBuffer.length() - 1);
            eol = true;
        }

        String[] bits = mBuffer.split("\n");

        for(int i = 0; i < bits.length - 1; i++) {
            message(bits[i]);
        }

        if(eol) {
            mBuffer = "";
            message(bits[bits.length - 1]);
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
            warning(bits[i]);
        }

        if(eol) {
            wBuffer = "";
            warning(bits[bits.length - 1]);
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
            error(bits[i]);
        }

        if(eol) {
            eBuffer = "";
            error(bits[bits.length - 1]);
        } else {
            eBuffer = bits[bits.length - 1];
        }
    }

    public void link(String s) {
        if(!s.endsWith("\n")) {
            s += "\n";
        }
        if(editor != null) {
            editor.link(s);
        }
    }        

    public void command(String s) {
        if(!s.endsWith("\n")) {
            s += "\n";
        }
        if(editor != null) {
            editor.command(s);
        } else {
            System.out.print(s);
        }
    }        

    public void bullet(String s) {
        if(!s.endsWith("\n")) {
            s += "\n";
        }
        if(editor != null) {
            editor.bullet(s);
        } else {
            System.out.print("    * " + s);
        }
    }        

    public void bullet2(String s) {
        if(!s.endsWith("\n")) {
            s += "\n";
        }
        if(editor != null) {
            editor.bullet2(s);
        } else {
            System.out.print("      > " + s);
        }
    }        

    public void heading(String s) {
        if(!s.endsWith("\n")) {
            s += "\n";
        }
        if(editor != null) {
            editor.heading(s);
        } else {
            System.out.print(s);
            for (int i = 0; i < s.trim().length(); i++) {
                System.out.print("=");
            }
            System.out.println();
        }
    }        

    public void message(String s) {
        flagError(s);

        if (percentageFilter != null) {
            Pattern p = Pattern.compile(percentageFilter);
            Matcher m = p.matcher(s);
            if (m.find()) {
                try {
                    String ps = m.group(1);
                    float perc = Float.parseFloat(ps);
                    setCompilingProgress((int)(perc * percentageMultiplier));
                    return;
                } catch (Exception e) {
                    Base.error(e);
                }
            }
        }

        if(!s.endsWith("\n")) {
            s += "\n";
        }

        if(editor != null) {
            editor.message(s);
        } else {
            System.out.print(s);
        }
    }

    public void warning(String s) {
        flagError(s);

        if(!s.endsWith("\n")) {
            s += "\n";
        }

        if(editor != null) {
            editor.warning(s);
        } else {
            System.out.print(s);
        }
    }

    public void error(String s) {
        if (isWarningMessage(s)) {
            warning(s);
            return;
        }
        flagError(s);

        if(!s.endsWith("\n")) {
            s += "\n";
        }

        if(editor != null) {
            editor.error(s);
        } else {
            System.err.print(s);
        }
    }

    public void error(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        error(sw.toString());
    }

    /**************************************************************************
     * OPTIONS HANDLING                                                       *
     *                                                                        *
     * Any options specified by the Options menu are controlled through these *
     * routines.                                                              *
     **************************************************************************/

    public String getOption(String opt) {
        PropertyFile props = ctx.getMerged();
        PropertyFile opts = Base.preferences.getChildren("board." + selectedBoard.getName() + ".options");
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
        Preferences.set("board." + selectedBoard.getName() + ".options." + opt, val);

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
        PropertyFile opts = Base.preferences.getChildren("board." + selectedBoard.getName() + ".options");
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
        arch.delete();
    }

    public void purgeCache() {
        Base.removeDir(getCacheFolder());
    }

    public void precompileLibrary(Library lib) {
        ctx.set("includes", generateIncludes());
        ctx.set("filename", sketchName);

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
    public void renameFile(File old, File newFile) {
        if(sketchFiles.indexOf(old) >= 0) {
            sketchFiles.remove(old);
            sketchFiles.add(newFile);
        }
    }

    public void deleteFile(File f) {
        if(sketchFiles.indexOf(f) >= 0) {
            sketchFiles.remove(f);
        }
    }

    public void rescanFileTree() {
        sketchFiles = new ArrayList<File>();
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

    public boolean parentIsLibrary() {
        TreeSet<String> groups = Library.getLibraryCategories();

        for(String group : groups) {
            TreeSet<Library> libs = Library.getLibraries(group);

            if(libs == null) {
                continue;
            }

            for(Library lib : libs) {
                if(isChildOf(lib.getFolder())) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean parentIsBoard() {
        return isChildOf(Base.getBoardsFolder());
    }

    public boolean parentIsCore() {
        return isChildOf(Base.getCoresFolder());
    }

    public boolean parentIsCompiler() {
        return isChildOf(Base.getCompilersFolder());
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
                archiveFile.delete();
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
            mf.append("Board: " + getBoard().getName() + "\n");
            mf.append("Core: " + getCore().getName() + "\n");
            mf.append("Archived: " + timeStamp + "\n");

            String mfData = mf.toString();
            byte[] bytes = mfData.getBytes();
            sar.write(bytes, 0, bytes.length);
            sar.closeEntry();
            sar.flush();
            sar.close();
        } catch(Exception e) {
            Base.error(e);
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
                //System.out.println(entry);
                zos.putNextEntry(entry);
                zos.closeEntry();
                addTree(sub, nowfar, zos);
            } else {
                ZipEntry entry = new ZipEntry(nowfar);
                entry.setTime(sub.lastModified());
                zos.putNextEntry(entry);
                zos.write(Base.loadBytesRaw(sub));
                zos.closeEntry();
            }
        }
    }

    public void loadConfig() {
        Debug.message("Loading config");

        if(configFile.get("board") != null) {
            Debug.message("Board: " + configFile.get("board"));
            setBoard(configFile.get("board"));
        }

        if(configFile.get("core") != null) {
            Debug.message("Core: " + configFile.get("core"));
            setCore(configFile.get("core"));
        }

        if(configFile.get("compiler") != null) {
            Debug.message("Compiler: " + configFile.get("compiler"));
            setCompiler(configFile.get("compiler"));
        }

        if(configFile.get("port") != null) {
            Debug.message("Port: " + configFile.get("port"));
            setSerialPort(configFile.get("port"));
        }

        if(configFile.get("programmer") != null) {
            Debug.message("Programmer: " + configFile.get("programmer"));
            setProgrammer(configFile.get("programmer"));
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

        if(selectedBoard != null) {
            i = selectedBoard.getIcon(size);

            if(i != null) return i;
        }

        if(selectedCore != null) {
            i = selectedCore.getIcon(size);

            if(i != null) return i;
        }

        return null;
    }

    public String getArch() {
        if (getBoard().get("arch") != null) {
            return getBoard().get("arch");
        }
        if (getCore().get("arch") != null) {
            return getCore().get("arch");
        }
        if (getCompiler().get("arch") != null) {
            return getCompiler().get("arch");
        }
        return getBoard().get("family");
    }

    public ArrayList<TodoEntry> todo(File f) {
        if (sketchFiles.indexOf(f) == -1) {
            return null;
        }

        Pattern p = Pattern.compile("(?i)\\/\\/\\s*(TODO|NOTE|FIXME):\\s*(.*)$");

        ArrayList<TodoEntry> found = new ArrayList<TodoEntry>();

        String content = getFileContent(f);
        String[] lines = content.split("\n");
        int lineno = 0;
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

    public void addKeywordsFromFile(File f) {
        String kwd = Base.getFileAsString(f);

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


    public void updateKeywords() {
        keywords.clear();
        if (selectedCompiler != null) {
            addKeywordsFromFile(selectedCompiler.getKeywords());
        }
        if (selectedCore != null) {
            addKeywordsFromFile(selectedCore.getKeywords());
        }
        if (selectedBoard != null) {
            addKeywordsFromFile(selectedBoard.getKeywords());
        }
        for (Library l : importedLibraries.values()) {
            addKeywordsFromFile(l.getKeywords());
        }
        TreeSet<String> fl = getAllFunctionNames();
        for (String s : fl) {
            FunctionBookmark bm = new FunctionBookmark(null, 0, s);

            keywords.put(bm.getName(), KeywordTypes.KEYWORD3);
        }

    }

    public HashMap<String, Integer> getKeywords() {
        return keywords;
    }

    public ImageIcon getIcon() {
        if ((configFile.get("icon") != null) && !(configFile.get("icon").equals(""))) {
            File iconFile = new File(sketchFolder, configFile.get("icon"));
            if (iconFile.exists()) {
                return Base.loadIconFromFile(iconFile);
            }
        }
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

    public void generateMakefile() {
        PropertyFile props = ctx.getMerged();
        if (props.get("makefile.template") != null) {
            String template = Base.getFileAsString(new File(ctx.parseString(props.get("makefile.template"))));
            File out = new File(sketchFolder, "Makefile");
            generateFileFromTemplate(template.toString(), out);
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
}
