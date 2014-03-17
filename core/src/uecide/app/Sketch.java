/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

package uecide.app;

import uecide.app.debug.*;
import uecide.app.preproc.*;
import uecide.plugin.*;

import java.util.regex.*;

import jssc.*;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.jar.*;
import java.util.zip.*;
import java.text.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

/**
 * Stores information about files in the current sketch
 */
public class Sketch implements MessageConsumer {
    public String name;     // The name of the sketch
    public File folder;
    public Editor editor;
    public File buildFolder;
    public String uuid; // used for temporary folders etc

    PrintWriter stdoutRedirect = null;
    PrintWriter stderrRedirect = null;

    public ArrayList<SketchFile> sketchFiles = new ArrayList<SketchFile>();

    public HashMap<String, Library> importedLibraries = new HashMap<String, Library>();
    public ArrayList<Library> orderedLibraries = new ArrayList<Library>();

    public HashMap<String, String> settings = new HashMap<String, String>();
    public HashMap<String, String> parameters = new HashMap<String, String>();

    public boolean doPrePurge = false;

    public Sketch(Editor ed, String path) {
        this(ed, new File(path));
    }

    public Sketch(Editor ed, File path) {
        editor = ed;
        uuid = UUID.randomUUID().toString();

        buildFolder = createBuildFolder();
        if (path == null) {
            path = createUntitledSketch();
        }
        folder = path;
        if (!path.exists()) {
            path.mkdirs();
            createBlankFile(path.getName() + ".ino");
        }

        String fn = path.getName().toLowerCase();
        if (fn.endsWith(".ino") || fn.endsWith(".pde")) {
            path = path.getParentFile();
        }
        folder = path;
        name = folder.getName();
        loadSketchFromFolder();
        editor.setTitle(Base.theme.get("product.cap") + " | " + name);
    }

    public void createBlankFile(String fileName) {
        File f = new File(folder, fileName);
        if (f.exists()) {
            return;
        }
        try {
            f.createNewFile();
        } catch (Exception e) {
        }
    }

    public void loadSketchFromFolder() {
        if (!isUntitled()) {
            Base.updateMRU(folder);
        }
        File fileList[] = folder.listFiles();
        Arrays.sort(fileList);

        for (File f : fileList){
            if (f.getName().endsWith(".ino")) {
                loadFile(f);
            }
        }

        for (File f : fileList){
            if (f.getName().endsWith(".pde")) {
                loadFile(f);
            }
        }

        for (File f : fileList){
            if (f.getName().endsWith(".cpp")) {
                loadFile(f);
            }
        }

        for (File f : fileList){
            if (f.getName().endsWith(".cxx")) {
                loadFile(f);
            }
        }

        for (File f : fileList){
            if (f.getName().endsWith(".cc")) {
                loadFile(f);
            }
        }

        for (File f : fileList){
            if (f.getName().endsWith(".c")) {
                loadFile(f);
            }
        }

        for (File f : fileList){
            if (f.getName().endsWith(".S")) {
                loadFile(f);
            }
        }

        for (File f : fileList){
            if (f.getName().endsWith(".h")) {
                loadFile(f);
            }
        }


    }

    public boolean loadFile(File f) {
        if (!f.exists()) {
            return false;
        }

        SketchFile newFile = new SketchFile();
        newFile.file = f;
        newFile.textArea = editor.addTab(f);
        newFile.modified = false;
        sketchFiles.add(newFile);
        return true;
    }

    public SketchFile getFileByName(String filename) {
        for (SketchFile f : sketchFiles) {
            if (f.file.getName().equals(filename)) {
                return f;
            }   
        }
        return null;
    }

    public File createBuildFolder() {
        String name = "build-" + uuid;
        File f = new File(Base.getTmpDir(), name);
        if (!f.exists()) {
            f.mkdirs();
        }
        f.deleteOnExit();
        return f;
    }

    public File createUntitledSketch() {

        int num = 0;
        File f = null;
        do {
            num++;
            String name = "untitled" + Integer.toString(num);
            f = new File(Base.getTmpDir(), name);
        } while (f.exists());
        f.deleteOnExit();
        return f;
    }

    public SketchFile getMainFile() {
        SketchFile f = getFileByName(name + ".ino");
        if (f == null) {
            f = getFileByName(name + ".pde");
        }
        return f;
    }

    public String getMainFilePath() {
        SketchFile f = getMainFile();
        if (f == null) {
            return "";
        }
        return f.file.getAbsolutePath();
    }

    public boolean isModified() {
        boolean modified = false;
        for (SketchFile f : sketchFiles) {
            if (f.textArea.isModified()) {
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

        for (int i = 0; i < c.length; i++) {
            if (((c[i] >= '0') && (c[i] <= '9')) ||
                ((c[i] >= 'a') && (c[i] <= 'z')) ||
                ((c[i] >= 'A') && (c[i] <= 'Z'))) {
                buffer.append(c[i]);
            } else {
                buffer.append('_');
            }
        }
        if (buffer.length() > 63) {
            buffer.setLength(63);
        }
        return buffer.toString();
    }

    public File getFolder() {
        return folder;
    }


    /*
     * This is the new preprocessing routine which uses the
     * compiler's preprocessor to strip comments, include files,
     * expand macros, etc.  Much better than the old Arduino way
     * of doing it. 
     */

    public boolean convertINOtoCPP(File ino, File cpp) {
        // This is quite a tricky routine to get right.  The steps are
        // simple enough, but getting them absoltely right and robust
        // is going to be interesting.

        // Step one is to just take the file and pass it through the
        // compile.preproc routine.

        return false;
    }

    public boolean doNewPrepare() {
        // Step one is to combine all INOs into one
        // if needed.

        HashMap<String, String> all = mergeAllProperties();

        SketchFile mainFile = getMainFile();
        String ext = all.get("build.extension"); 
        if (ext == null) ext = "cpp";

        if (Base.preferences.getBoolean("compiler.combine_ino")) {
            StringBuilder combinedINO = new StringBuilder();
            combinedINO.append("#line 1 \"" + mainFile.file.getName() + "\"\n");
            String[] bodylines = mainFile.textArea.getText().split("\n");
            for (String line : bodylines) {
                combinedINO.append(line + "\n");
            }
            for (SketchFile f : sketchFiles) {
                if (f != mainFile) {
                    combinedINO.append("#line 1 \"" + f.file.getName() + "\"\n");
                    bodylines = f.textArea.getText().split("\n");
                    for (String line : bodylines) {
                        combinedINO.append(line + "\n");
                    }
                }
            }

            File ino = new File(buildFolder, mainFile.file.getName());
            File cpp = new File(buildFolder, mainFile.file.getName().replace(".ino","." + ext));

            try {
                PrintWriter pw = new PrintWriter(ino);
                pw.print(combinedINO.toString());
                pw.close();
            } catch (Exception e) {
                Base.error(e);
            }

            return convertINOtoCPP(ino, cpp);
        } else {
            for (SketchFile f : sketchFiles) {
                File ino = new File(buildFolder, f.file.getName());
                File cpp = new File(buildFolder, f.file.getName().replace(".ino","." + ext));
                try {
                    PrintWriter pw = new PrintWriter(ino);
                    pw.print(f.textArea.getText());
                    pw.close();
                } catch (Exception e) {
                    Base.error(e);
                    return false;
                }
                boolean rv = convertINOtoCPP(ino, cpp);
                if (!rv) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean prepare() {
        HashMap<String, String> all = mergeAllProperties();
        if (editor.board == null) {
            Base.showWarning(Translate.t("No Board Selected"), Translate.w("You have no board selected.  You must select a board before you can compile your sketch.", 40, "\n"), null);
            return false;
        }
        if (editor.core == null) {
            Base.showWarning(Translate.t("No Core Selected"), Translate.w("You have no core selected.  You must select a core before you can compile your sketch.", 40, "\n"), null);
            return false;
        }
        if (Base.preferences.getBoolean("export.delete_target_folder")) {
            cleanBuild();
        }

        if (all.get("compile.preproc") != null) {
            if (all.get("compile.preproc").equals("") == false) {
                return doNewPrepare();
            }
        }

        parameters = new HashMap<String, String>();
        importedLibraries = new HashMap<String, Library>();
        orderedLibraries = new ArrayList<Library>();
        StringBuilder combinedMain = new StringBuilder();
        SketchFile mainFile = getMainFile();
        if (Base.preferences.getBoolean("compiler.combine_ino")) {
            combinedMain.append("#line 1 \"" + mainFile.file.getName() + "\"\n");
            Pattern pragma = Pattern.compile("#pragma\\s+parameter");
            String[] bodylines = mainFile.textArea.getText().split("\n");
            for (String line : bodylines) {
                if (line.trim().startsWith("#pragma")) {
                    Matcher mtch = pragma.matcher(line);
                    if (mtch.find()) {
                        line = "// " + line;
                    }
                }
                combinedMain.append(line + "\n");
            }
        }
        for (SketchFile f : sketchFiles) {
            String lcn = f.file.getName().toLowerCase();
            if (
                lcn.endsWith(".h") || 
                lcn.endsWith(".hh") || 
                lcn.endsWith(".c") || 
                lcn.endsWith(".cpp") || 
                lcn.endsWith(".cxx") || 
                lcn.endsWith(".cc") || 
                lcn.endsWith(".s")
            ) {
                f.writeToFolder(buildFolder);
            }
            if (lcn.endsWith(".pde") || lcn.endsWith(".ino")) {

                if (Base.preferences.getBoolean("compiler.combine_ino")) {
                    if (!(f.equals(mainFile))) {
                        combinedMain.append("#line 1 \"" + f.file.getName() + "\"\n");
                        Pattern pragma = Pattern.compile("#pragma\\s+parameter");
                        String[] bodylines = f.textArea.getText().split("\n");
                        for (String line : bodylines) {
                            if (line.trim().startsWith("#pragma")) {
                                Matcher mtch = pragma.matcher(line);
                                if (mtch.find()) {
                                    line = "// " + line;
                                }
                            }
                            combinedMain.append(line + "\n");
                        }
                    }
                } else {
                    String rawData = f.textArea.getText();
                    PdePreprocessor proc = new PdePreprocessor();

                    f.includes = gatherIncludes(f);
                    f.prototypes = proc.prototypes(rawData).toArray(new String[0]);
                    StringBuilder sb = new StringBuilder();

                    f.headerLines = 0;
                    int firstStat = proc.firstStatement(rawData);

                    String header = rawData.substring(0, firstStat);
                    String body = rawData.substring(firstStat);

                    Matcher m = Pattern.compile("(\n)|(\r)|(\r\n)").matcher(header);
                    int headerLength = 1;
                    while (m.find())
                    {
                        headerLength++;
                    }

                    String coreHeader = all.get("core.header");
                    if (coreHeader != "") {
                        sb.append("#include <" + coreHeader + ">\n");
                        f.headerLines ++;
                    }
                    sb.append("#line 1 \"" + f.file.getName() + "\"\n");
                    sb.append(header);
                    f.headerLines += headerLength;

                    if (Base.preferences.getBoolean("compiler.disable_prototypes") == false) {
                        for (String prototype : f.prototypes) {
                            sb.append(prototype + "\n");
                            f.headerLines++;
                        }
                    }

                    sb.append("\n");
                    f.headerLines ++;

                    sb.append("#line " + headerLength + " \"" + f.file.getName() + "\"\n");
                    f.headerLines ++;

                    Pattern pragma = Pattern.compile("#pragma\\s+parameter");
                    String[] bodylines = body.split("\n");
                    for (String line : bodylines) {
                        if (line.trim().startsWith("#pragma")) {
                            Matcher mtch = pragma.matcher(line);
                            if (mtch.find()) {
                                line = "// " + line;
                            }
                        }
                        sb.append(line + "\n");
                    }

                    String newFileName = f.file.getName();
                    int dot = newFileName.lastIndexOf(".");
                    newFileName = newFileName.substring(0, dot);
                    String ext = all.get("build.extension"); 
                    if (ext == null) ext = "cpp";
                    newFileName = newFileName + "." + ext;

                    try {
                        PrintWriter pw = new PrintWriter(new File(buildFolder, newFileName));
                        pw.print(sb.toString());
                        pw.close();
                    } catch (Exception e) {
                        Base.error(e);
                    }
                }
            }
            if (Base.preferences.getBoolean("editor.correct_numbers")) {
                f.textArea.setNumberOffset(f.headerLines+1);
            } else {
                f.textArea.setNumberOffset(1);
            }
        }
        if (Base.preferences.getBoolean("compiler.combine_ino")) {
            SketchFile f = getMainFile();
            String rawData = combinedMain.toString();
            PdePreprocessor proc = new PdePreprocessor();

            f.includes = gatherIncludes(f);
            f.prototypes = proc.prototypes(rawData).toArray(new String[0]);
            StringBuilder sb = new StringBuilder();
            f.headerLines = 0;
            int firstStat = proc.firstStatement(rawData);

            String header = rawData.substring(0, firstStat);
            String body = rawData.substring(firstStat);

            Matcher m = Pattern.compile("(\n)|(\r)|(\r\n)").matcher(header);
            int headerLength = 1;
            while (m.find())
            {
                headerLength++;
            }

            String coreHeader = all.get("core.header");
            if (coreHeader != "") {
                sb.append("#include <" + coreHeader + ">\n");
                f.headerLines ++;
            }
            sb.append("#line 1 \"" + f.file.getName() + "\"\n");
            sb.append(header);
            f.headerLines += headerLength;

            if (Base.preferences.getBoolean("compiler.disable_prototypes") == false) {
                for (String prototype : f.prototypes) {
                    sb.append(prototype + "\n");
                    f.headerLines++;
                }
            }

            sb.append("\n");
            f.headerLines ++;

            sb.append("#line " + headerLength + " \"" + f.file.getName() + "\"\n");
            f.headerLines ++;
            Pattern pragma = Pattern.compile("#pragma\\s+parameter");
            String[] bodylines = body.split("\n");
            for (String line : bodylines) {
                if (line.trim().startsWith("#pragma")) {
                    Matcher mtch = pragma.matcher(line);
                    if (mtch.find()) {
                        line = "// " + line;
                    }
                }
                sb.append(line + "\n");
            }

            String ext = all.get("build.extension"); 
            if (ext == null) ext = "cpp";
            String newFileName = name + "." + ext;

            try {
                PrintWriter pw = new PrintWriter(new File(buildFolder, newFileName));
                pw.print(sb.toString());
                pw.close();
            } catch (Exception e) {
                Base.error(e);
            }
        }
        return true;
    }

    public String stripComments(String data) {
        StringBuilder b = new StringBuilder();

        String[] lines = data.split("\n");
        for (String line : lines) {
            int comment = line.indexOf("//");
            if (comment > -1) {
                line = line.substring(0, comment);
            }
            b.append(line);
            b.append("\n");
        }

        String out = b.toString();

        out.replaceAll("/\\*(?:.|[\\n\\r])*?\\*/", "");

        return out;
    }

    public String[] gatherIncludes(SketchFile f) {
        String[] data = f.textArea.getText().split("\n"); //stripComments(f.textArea.getText()).split("\n");
        ArrayList<String> includes = new ArrayList<String>();
    
        Pattern pragma = Pattern.compile("#pragma\\s+parameter\\s+([^=]+)\\s*=\\s*(.*)");

        for (String line : data) {
            line = line.trim();
            if (line.startsWith("#pragma")) {
                Matcher m = pragma.matcher(line);
                if (m.find()) {
                    String key = m.group(1);
                    String value = m.group(2);
                    String munged = "";
                    for (int i = 0; i < value.length(); i++) {

                        if (value.charAt(i) == '"') {
                            munged += '"';
                            i++;
                            while (value.charAt(i) != '"') {
                                munged += value.charAt(i++);
                            }
                            munged += '"';
                            continue;
                        }

                        if (value.charAt(i) == '\'') {
                            munged += '\'';
                            i++;
                            while (value.charAt(i) != '\'') {
                                munged += value.charAt(i++);
                            }
                            munged += '\'';
                            continue;
                        }

                        if (value.charAt(i) == ' ') {
                            munged += "::";
                            continue;
                        }

                        munged += value.charAt(i);
                    }
                    parameters.put(key, munged);
                }
                continue;
            }
            if (line.startsWith("#include")) {
                int qs = line.indexOf("<");
                if (qs == -1) {
                    qs = line.indexOf("\"");
                }
                if (qs == -1) {
                    continue;
                }
                qs++;
                int qe = line.indexOf(">");
                if (qe == -1) {
                    qe = line.indexOf("\"", qs);
                }
                String i = line.substring(qs, qe);
                addLibraryToImportList(i);
            }
        }
        return includes.toArray(new String[includes.size()]);
    }

    public void addLibraryToImportList(String l) {
        if (l.endsWith(".h")) {
            l = l.substring(0, l.lastIndexOf("."));
        }
        HashMap<String, Library> coreLibraries = Base.getLibraryCollection(editor.core.getName());
        HashMap<String, Library> contribLibraries = Base.getLibraryCollection("sketchbook");

        if (importedLibraries.get(l) != null) {
            // The library has already been imported.  Do nothing
            return;
        }

        Library lib = coreLibraries.get(l);
        if (lib == null) {
            lib = contribLibraries.get(l);
        }

        if (lib == null) {
            // The library doesn't exist - either it's a system header or a library that isn't installed.
            return;
        }

        // At this point we have a valid library that hasn't yet been imported.  Now to recurse.
        // First add the library to the imported list
        importedLibraries.put(l, lib);
        orderedLibraries.add(lib);

        // And then work through all the required libraries and add them.
        ArrayList<String> requiredLibraries = lib.getRequiredLibraries();
        for (String req : requiredLibraries) {
            addLibraryToImportList(req);
        }
    }

    public SketchFile getCodeByEditor(SketchEditor e) {
        for (SketchFile f : sketchFiles) {
            if (f.textArea.equals(e)) {
                return f;
            }   
        }
        return null;
    }

    public boolean upload() {
        String uploadCommand;
        if (!build()) {
            return false;
        }

        HashMap<String, String> all = mergeAllProperties();
        editor.statusNotice(Translate.t("Uploading to Board..."));
        settings.put("filename", name);
        settings.put("filename.elf", name + ".elf");
        settings.put("filename.hex", name + ".hex");
        settings.put("filename.eep", name + ".eep");

        boolean isJava = true;
        uploadCommand = editor.board.get("upload." + editor.programmer + ".command.java");
        if (uploadCommand == null) {
            uploadCommand = editor.core.get("upload." + editor.programmer + ".command.java");
        }
        if (uploadCommand == null) {
            isJava = false;
            uploadCommand = editor.board.get("upload." + editor.programmer + ".command." + Base.getOSFullName());
        }
        if (uploadCommand == null) {
            uploadCommand = editor.board.get("upload." + editor.programmer + ".command." + Base.getOSName());
        }
        if (uploadCommand == null) {
            uploadCommand = editor.board.get("upload." + editor.programmer + ".command");
        }
        if (uploadCommand == null) {
            uploadCommand = editor.core.get("upload." + editor.programmer + ".command." + Base.getOSFullName());
        }
        if (uploadCommand == null) {
            uploadCommand = editor.core.get("upload." + editor.programmer + ".command." + Base.getOSName());
        }
        if (uploadCommand == null) {
            uploadCommand = editor.core.get("upload." + editor.programmer + ".command");
        }

        if (uploadCommand == null) {
            message("No upload command defined for board\n", 2);
            editor.statusNotice(Translate.t("Upload Failed"));
            return false;
        }

   
        if (isJava) {
            Plugin uploader;
            uploader = Base.plugins.get(uploadCommand);
            if (uploader == null) {
                message("Upload class " + uploadCommand + " not found.\n", 2);
                editor.statusNotice(Translate.t("Upload Failed"));
                return false;
            }
            try {
                if ((uploader.flags() & BasePlugin.LOADER) == 0) {
                    message(uploadCommand + "is not a valid loader plugin.\n", 2);
                    editor.statusNotice(Translate.t("Upload Failed"));
                    return false;
                }
                uploader.run();
            } catch (Exception e) {
                editor.statusNotice(Translate.t("Upload Failed"));
                message(e.toString(), 2);
                return false;
            }
            editor.statusNotice(Translate.t("Upload Complete"));
            return true;
        }

        String[] spl;
        spl = parseString(uploadCommand).split("::");

        String executable = spl[0];
        if (Base.isWindows()) {
            executable = executable + ".exe";
        }

        File exeFile = new File(folder, executable);
        File tools;
        if (!exeFile.exists()) {
            tools = new File(folder, "tools");
            exeFile = new File(tools, executable);
        }
        if (!exeFile.exists()) {
            exeFile = new File(editor.core.getFolder(), executable);
        }
        if (!exeFile.exists()) {
            tools = new File(editor.core.getFolder(), "tools");
            exeFile = new File(tools, executable);
        }
        if (!exeFile.exists()) {
            exeFile = new File(executable);
        }
        if (exeFile.exists()) {
            executable = exeFile.getAbsolutePath();
        }

        spl[0] = executable;

        // Parse each word, doing String replacement as needed, trimming it, and
        // generally getting it ready for executing.

        String commandString = executable;
        for (int i = 1; i < spl.length; i++) {
            String tmp = spl[i];
            tmp = tmp.trim();
            if (tmp.length() > 0) {
                commandString += "::" + tmp;
            }
        }

        boolean dtr = false;
        boolean rts = false;


        String ulu = all.get("upload." + editor.programmer + ".using");
        if (ulu == null) ulu = "serial";

        String doDtr = all.get("upload." + editor.programmer + ".dtr");
        if (doDtr != null) {
            if (doDtr.equals("yes")) {
                dtr = true;
            }
        }
        String doRts = all.get("upload." + editor.programmer + ".rts");
        if (doRts != null) {
            if (doRts.equals("yes")) {
                rts = true;
            }
        }

        if (ulu.equals("serial") || ulu.equals("usbcdc"))
        {
            editor.grabSerialPort();
            if (dtr || rts) {
                assertDTRRTS(dtr, rts);
            }
        }
        if (ulu.equals("usbcdc")) {
            try {
                String baud = all.get("upload." + editor.programmer + ".reset.baud");
                if (baud != null) {
                    int b = Integer.parseInt(baud);
          //          editor.grabSerialPort();
                    
                    SerialPort serialPort = Serial.requestPort(editor.getSerialPort(), this, b);
                    if (serialPort == null) {
                        Base.error("Unable to lock serial port");
                        return false;
                    }
                    Thread.sleep(1000);
                    Serial.releasePort(serialPort);
                    serialPort = null;
                    System.gc();
                    Thread.sleep(1500);
                }
            } catch (Exception e) {
                Base.error(e);
            }
        }

        boolean res = execAsynchronously(commandString);
        if (ulu.equals("serial") || ulu.equals("usbcdc"))
        {
            if (dtr || rts) {
                assertDTRRTS(false, false);
            }
            editor.releaseSerialPort();
        }
        if (res) {
            editor.statusNotice(Translate.t("Upload Complete"));
        } else {
            editor.statusNotice(Translate.t("Upload Failed"));
        }
        return res;
    }

    public void assertDTRRTS(boolean dtr, boolean rts) {
        try {
            SerialPort serialPort = Serial.requestPort(editor.getSerialPort(), this);
            if (serialPort == null) {
                Base.error("Unable to lock serial port");
            }
            serialPort.setDTR(dtr);
            serialPort.setRTS(rts);
            Serial.releasePort(serialPort);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        System.gc();
    }

    public boolean build() {
        if (editor.board == null) {
            Base.showWarning(Translate.t("No Board Selected"), Translate.w("The sketch cannot be compiled: You have no board selected. If you haven't yet installed any boards please do so through the Plugin Manager.", 40, "\n"), null);
            return false;
        }
        if (editor.core == null) {
            Base.showWarning(Translate.t("No Core Selected"), Translate.w("The sketch cannot be compiled: You have no core selected. If you haven't yet installed a core please do so through the Plugin Manager.", 40, "\n"), null);
            return false;
        }
        if (editor.compiler == null) {
            Base.showWarning(Translate.t("No Compiler Available"), Translate.w("The sketch cannot be compiled: The compiler for the selected core is not available. Please ensure the compiler is installed using the Plugin Manager.", 40, "\n"), null);
            return false;
        }
        editor.statusNotice(Translate.t("Compiling..."));
        try {
            if (!prepare()) {
                editor.statusNotice(Translate.t("Compile Failed"));
                return false;
            }
        } catch (Exception e) {
            Base.error(e);
            return false;
        }
        return compile();
    }

    public boolean save() {
        for (SketchFile f : sketchFiles) {
            f.save();
        }
        return true;
    }

    public String getName() {
        return name;
    }

    public Collection<Library> getImportedLibraries() {
        return importedLibraries.values();
    }

    public ArrayList<Library> getOrderedLibraries() {
        return orderedLibraries;
    }

    public ArrayList<String> getIncludePaths() {
        ArrayList<String> libFiles = new ArrayList<String>();

        HashMap<String, String> all = mergeAllProperties();
      
        libFiles.add(buildFolder.getAbsolutePath());
        libFiles.add(editor.board.getFolder().getAbsolutePath());
        libFiles.add(editor.core.getLibrariesFolder().getAbsolutePath());

        for (String key : all.keySet()) {
            if (key.startsWith("compiler.library.")) {
                String coreLibName = key.substring(17);
                String libPaths = all.get(key);
                if (libPaths != null && !(libPaths.trim().equals(""))) {
                    String[] libPathsArray = libPaths.split("::");
                    for (String p : libPathsArray) {
                        File f = new File(editor.compiler.getFolder(), p);
                        if (f.exists()) {
                            libFiles.add(f.getAbsolutePath());
                        }        
                    }
                }
            }
        }

        for (String key : all.keySet()) {
            if (key.startsWith("core.library.")) {
                String coreLibName = key.substring(13);
                String libPaths = all.get(key);
                if (libPaths != null && !(libPaths.trim().equals(""))) {
                    String[] libPathsArray = libPaths.split("::");
                    for (String p : libPathsArray) {
                        File f = new File(editor.core.getFolder(), p);
                        if (f.exists()) {
                            libFiles.add(f.getAbsolutePath());
                        }        
                    }
                }
            }
        }

        for (String key : all.keySet()) {
            if (key.startsWith("board.library.")) {
                String coreLibName = key.substring(14);
                String libPaths = all.get(key);
                if (libPaths != null && !(libPaths.trim().equals(""))) {
                    String[] libPathsArray = libPaths.split("::");
                    for (String p : libPathsArray) {
                        File f = new File(editor.board.getFolder(), p);
                        if (f.exists()) {
                            libFiles.add(f.getAbsolutePath());
                        }        
                    }
                }
            }
        }

        for (Library l : getOrderedLibraries()) {
            libFiles.add(l.getFolder().getAbsolutePath());
        }
        return libFiles;
    }

    public void checkForSettings() {
        SketchFile mainFile = getMainFile();

        Pattern p = Pattern.compile("^#pragma\\s+parameter\\s+([^\\s]+)\\s*=\\s*(.*)$");

        String[] data = mainFile.textArea.getText().split("\n");
        for (String line : data) {
            line = line.trim();
            Matcher m = p.matcher(line);
            if (m.find()) {
                String key = m.group(1);
                String value = m.group(2);
                if (key.equals("board")) {
                    editor.selectBoard(value);
                }
            }
        }
    }
        
    public boolean saveAs() {
        FileDialog fd = new FileDialog(editor,
                                   Translate.t("Save sketch folder as..."),
                                   FileDialog.SAVE);

        fd.setDirectory(Base.preferences.get("sketchbook.path"));

        fd.setFile(name);

        fd.setVisible(true);

        String newParentDir = fd.getDirectory();
        String newFileName = fd.getFile();

        if (newFileName == null) {
            return false;
        }

        File newFolder = new File(newParentDir, newFileName);

        if (newFolder.equals(folder)) {
            save();
            return true;
        }

        if (newFolder.exists()) {
            Object[] options = { "OK", "Cancel" };
            String prompt = "Replace " + newFolder.getAbsolutePath();
            int result = JOptionPane.showOptionDialog(editor, prompt, "Replace",
                                                JOptionPane.YES_NO_OPTION,
                                                JOptionPane.QUESTION_MESSAGE,
                                                null, options, options[0]);
            if (result != JOptionPane.YES_OPTION) {
                return false;
            }
            Base.removeDir(newFolder);
        }

        newFolder.mkdirs();
        for (SketchFile f : sketchFiles) {
            String n = f.file.getName();
            f.file = new File(newFolder, n);
        }

        SketchFile mf = getMainFile();
        mf.file = new File(newFolder, newFolder.getName() + ".ino");
        folder = newFolder;
        name = folder.getName();
        save();
        editor.setTitle(Base.theme.get("product.cap") + " | " + name);
        int index = editor.getTabByFile(mf);
        editor.setTabName(index, mf.file.getName());
        return true;
    }

    public void handleAddFile() {
        ensureExistence();

        // if read-only, give an error
        if (isReadOnly()) {
            // if the files are read-only, need to first do a "save as".
            Base.showMessage(Translate.t("Sketch is Read-Only"),
                Translate.w("Some files are marked \"read-only\", so you'll need to re-save the sketch in another location, and try again.", 40, "\n"));
            return;
        }

        // get a dialog, select a file to add to the sketch
        String prompt = Translate.t("Select a source file to copy to your sketch");
        FileDialog fd = new FileDialog(editor, prompt, FileDialog.LOAD);
        fd.setVisible(true);

        String directory = fd.getDirectory();
        String filename = fd.getFile();
        if (filename == null) return;

        // copy the file into the folder. if people would rather
        // it move instead of copy, they can do it by hand
        File sourceFile = new File(directory, filename);

        // now do the work of adding the file
        boolean result = addFile(sourceFile);

        if (result) {
            editor.statusNotice(Translate.t("One file added to the sketch."));
        }
    }

    public boolean addFile(File sourceFile) {
        String filename = sourceFile.getName();
        boolean replacement = false;

        if (!sourceFile.exists()) {
            Base.showWarning(Translate.t("Error Adding File"),Translate.w("Error: %1 does not exist", 40, "\n", sourceFile.getAbsolutePath()), null);
            return false;
        }

        if (!(validSourceFile(sourceFile))) {
            Base.showWarning(Translate.t("Error Adding File"),Translate.w("Error: you can only add .ino, .pde, .c, .cpp, .h, .hh or .S files to a sketch", 40, "\n"), null);
            return false;
        }

        File destFile = new File(folder, sourceFile.getName());

        if (sourceFile.equals(destFile)) {
            Base.showWarning(Translate.t("Error Adding File"),Translate.t("Error: that file is already in the sketch!"), null);
            return false;
        }

        if (destFile.exists()) {
            Object[] options = { "OK", "Cancel" };
            String prompt = "Replace the existing version of " + filename + "?";
            int result = JOptionPane.showOptionDialog(editor, prompt, "Replace",
                                                JOptionPane.YES_NO_OPTION,
                                                JOptionPane.QUESTION_MESSAGE,
                                                null, options, options[0]);
            if (result == JOptionPane.YES_OPTION) {
                replacement = true;
            } else {
                return false;
            }
        }

        if (replacement) {
            boolean muchSuccess = destFile.delete();
            if (!muchSuccess) {
                Base.showWarning(Translate.t("Error Adding File"), Translate.w("Could not delete the existing file %1", 40, "\n",
                    destFile.getAbsolutePath()), null);
                return false;
            }
        }

        try {
            Base.copyFile(sourceFile, destFile);
        } catch (Exception e) {
            Base.showWarning(Translate.t("Error Adding File"),Translate.w("Error copying file: %1", 40, "\n", e.getMessage()), null);
            return false;
        }

        loadFile(destFile);
        return true;
    }

    public SketchFile getCode(int i) {
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
            editor.status.progressUpdate(percent);
        }

    public void ensureExistence() {
        if (folder.exists() && folder.isDirectory()) return;

        Base.showWarning(Translate.t("Sketch Disappeared"),
             Translate.w("The sketch folder has disappeared. Will attempt to re-save in the same location, but anything besides the code will be lost.", 40, "\n"), null);
        try {
            folder.mkdirs();

            for (SketchFile c : sketchFiles) {
                c.save();
            }
        } catch (Exception e) {
            Base.showWarning(Translate.t("Could not re-save sketch"),
                Translate.w("Could not properly re-save the sketch. You may be in trouble at this point, and it might be time to copy and paste your code to another text editor.", 40, "\n"), e);
        }
    }

    public boolean isReadOnly() {

        if (isInternal()) {
            return true;
        }

        File testFile = new File(folder, ".testWrite");
        boolean canWrite = false;
        try {
            testFile.createNewFile();
            if (testFile.exists()) {
                testFile.delete();
                canWrite = true;
            }
        } catch (Exception e) {
            return true;
        }

        if (!canWrite) {
            return true;
        }

        canWrite = true;

        for (SketchFile c : sketchFiles) {
            if (!c.file.canWrite()) {
                canWrite = false;
            }
        }
        return !canWrite;
    }

    public void redirectChannel(int c, PrintWriter pw) {
        if (c == 1) {
            stdoutRedirect = pw;
        }
        if (c == 2) {
            stderrRedirect = pw;
        }
    }

    public void unredirectChannel(int c) {
        if (c == 1) {
            if (stdoutRedirect != null) {
                stdoutRedirect.close();
                stdoutRedirect = null;
            }
        }
        if (c == 2) {
            if (stderrRedirect != null) {
                stderrRedirect.close();
                stderrRedirect = null;
            }
        }
    }

    public void message(String m) {
        message(m, 1);
    }

    public void message(String m, int chan) {
        if (m.trim() != "") {
            if (chan == 2) {
                if (stderrRedirect == null) {
                    Pattern p = Pattern.compile(editor.compiler.getErrorRegex());
                    Matcher match = p.matcher(m);
                    if (match.find()) {
                        String filename = match.group(1);
                        if (filename.startsWith(getBuildFolder().getAbsolutePath())) {
                            filename = filename.substring(getBuildFolder().getAbsolutePath().length() + 1);
                        }
                        editor.console.message("Error at line " + match.group(2) + " in file " + filename + ":\n    " + match.group(3) + "\n", 2, false);
                        SketchFile f = getFileByName(filename);
                        if (f != null) {
                            int tn = editor.getTabByFile(f);
                            editor.selectTab(tn);
                            f.textArea.setCaretLineNumber(Integer.parseInt(match.group(2)));
                            f.textArea.addLineHighlight(Integer.parseInt(match.group(2)), Base.theme.getColor("editor.compile.error.bgcolor"));
                        }
                    } else {

                        p = Pattern.compile(editor.compiler.getWarningRegex());
                        match = p.matcher(m);
                        if (match.find()) {
                            String filename = match.group(1);
                            if (filename.startsWith(getBuildFolder().getAbsolutePath())) {
                                filename = filename.substring(getBuildFolder().getAbsolutePath().length() + 1);
                            }
                            editor.console.message("Warning at line " + match.group(2) + " in file " + filename + ":\n    " + match.group(3) + "\n", 1, false);
                            SketchFile f = getFileByName(filename);
                            if (f != null) {
                                f.textArea.addLineHighlight(Integer.parseInt(match.group(2)), Base.theme.getColor("editor.compile.warning.bgcolor"));
                            }
                        } else {
                            editor.console.message(m, 0, false);
                        }
                    }
                } else {
                    stderrRedirect.print(m);
                }
            } else {
                if (stdoutRedirect == null) {
                    editor.console.message(m, 0, false);
                } else {
                    stdoutRedirect.print(m);
                }
            }
        }
    }

    public void needPurge() {
        doPrePurge = true;
    }

    public boolean compile() {
        ArrayList<String> includePaths;
        List<File> objectFiles = new ArrayList<File>();
        List<File> tobjs;
        HashMap<String, String> all = mergeAllProperties();

        if (doPrePurge) {
            doPrePurge = false;
            Base.removeDir(getCacheFolder());
        }

        for (SketchFile f : sketchFiles) {
            f.textArea.removeAllLineHighlights();
        }

        includePaths = getIncludePaths();

        settings.put("filename", name);
        settings.put("includes", preparePaths(includePaths));

        settings.put("option.flags", editor.getFlags("flags"));
        settings.put("option.cflags", editor.getFlags("cflags"));
        settings.put("option.cppflags", editor.getFlags("cppflags"));
        settings.put("option.ldflags", editor.getFlags("ldflags"));

        editor.statusNotice(Translate.t("Compiling Sketch..."));

        // Copy any specified files from the compiler, core or board folders
        // into the build folder.  This is especially good if the compiler
        // executable needs certain DLL files to be in the current working
        // directory (which is the build folder).

        String precopy = parseString(all.get("compile.precopy"));
        if (precopy != null) {
            String[] copyfiles = precopy.split("::");
            for (String cf : copyfiles) {
                File src = new File(editor.compiler.getFolder(), cf);
                if (!src.exists()) {
                    src = new File(editor.core.getFolder(), cf);
                }
                if (!src.exists()) {
                    src = new File(editor.board.getFolder(), cf);
                }
                if (src.exists()) {
                    File dest = new File(buildFolder, src.getName());
                    Base.copyFile(src, dest);
                }
            }
        }

        tobjs = compileSketch();
        if (tobjs == null) {
            editor.statusNotice(Translate.t("Error Compiling Sketch"));
            return false;
        }
        objectFiles.addAll(tobjs);
        setCompilingProgress(30);

        editor.statusNotice(Translate.t("Compiling Libraries..."));
        if (!compileLibraries()) {
            editor.statusNotice(Translate.t("Error Compiling Libraries"));
            return false;
        }

        setCompilingProgress(40);

        ArrayList<String> coreLibraries = new ArrayList<String>();

        for (String key : all.keySet()) {
            if (key.startsWith("compiler.library.")) {
                String coreLibName = key.substring(13);
                String libPaths = all.get(key);
                if (libPaths != null && !(libPaths.trim().equals(""))) {
                    libPaths = parseString(libPaths);
                    editor.statusNotice(Translate.t("Compiling lib" + coreLibName + "..."));
                    String[] libPathsArray = libPaths.split("::");
                    if (!compileCore(libPathsArray, coreLibName)) {
                        editor.statusNotice(Translate.t("Error Compiling lib" + coreLibName));
                        return false;
                    }
                    coreLibraries.add("-l" + coreLibName);
                }
            }
        }

        for (String key : all.keySet()) {
            if (key.startsWith("core.library.")) {
                String coreLibName = key.substring(13);
                String libPaths = all.get(key);
                if (libPaths != null && !(libPaths.trim().equals(""))) {
                    editor.statusNotice(Translate.t("Compiling lib" + coreLibName + "..."));
                    libPaths = parseString(libPaths);
                    String[] libPathsArray = libPaths.split("::");
                    if (!compileCore(libPathsArray, coreLibName)) {
                        editor.statusNotice(Translate.t("Error Compiling lib" + coreLibName));
                        return false;
                    }
                    coreLibraries.add("-l" + coreLibName);
                }
            }
        }

        for (String key : all.keySet()) {
            if (key.startsWith("board.library.")) {
                String coreLibName = key.substring(13);
                String libPaths = all.get(key);
                if (libPaths != null && !(libPaths.trim().equals(""))) {
                    libPaths = parseString(libPaths);
                    editor.statusNotice(Translate.t("Compiling lib" + coreLibName + "..."));
                    String[] libPathsArray = libPaths.split("::");
                    if (!compileCore(libPathsArray, coreLibName)) {
                        editor.statusNotice(Translate.t("Error Compiling lib" + coreLibName));
                        return false;
                    }
                    coreLibraries.add("-l" + coreLibName);
                }
            }
        }

        String coreLibs = "";
        for (String lib : coreLibraries) {
            if (!coreLibs.equals("")) {
                coreLibs += "::";
            }
            coreLibs += lib;
        }

        setCompilingProgress(50);

        editor.statusNotice(Translate.t("Linking Sketch..."));
        settings.put("filename", name);
        if (!compileLink(objectFiles, coreLibs)) {
            editor.statusNotice(Translate.t("Error Linking Sketch"));
            return false;
        }
        setCompilingProgress(60);

        editor.statusNotice(Translate.t("Extracting EEPROM..."));
        if (!compileEEP()) {
            editor.statusNotice(Translate.t("Error Extracting EEPROM"));
            return false;
        }
        setCompilingProgress(70);


        if (all.get("compile.lss") != null) {
            if (Base.preferences.getBoolean("compiler.generate_lss")) {
                editor.statusNotice(Translate.t("Generating Listing..."));
                File redirectTo = new File(buildFolder, name + ".lss");
                if (redirectTo.exists()) {
                    redirectTo.delete();
                }                

                boolean result = false;
                try {
                    redirectChannel(1, new PrintWriter(redirectTo));
                    result = compileLSS();
                    unredirectChannel(1);
                } catch (Exception e) {
                    result = false;
                }
                unredirectChannel(1);

                if (!result) {
                    editor.statusNotice(Translate.t("Error Generating Listing"));
                    return false;
                }
                if (Base.preferences.getBoolean("export.save_lss")) {
                    try {
                        Base.copyFile(new File(buildFolder, name + ".lss"), new File(folder, name + ".lss"));
                    } catch (Exception e) {
                        message("Error copying LSS file: " + e.getMessage() + "\n", 2);
                    }
                }
            }
        }

        setCompilingProgress(80);

        editor.statusNotice(Translate.t("Converting to HEX..."));
        if (!compileHEX()) {
            editor.statusNotice(Translate.t("Error Converting to HEX"));
            return false;
        }
        setCompilingProgress(90);

        if (Base.preferences.getBoolean("export.save_hex")) {
            try {
                Base.copyFile(new File(buildFolder, name + ".hex"), new File(folder, name + ".hex"));
            } catch (Exception e) {
                message("Error copying HEX file: " + e.getMessage() + "\n", 2);
            }
        }

        editor.statusNotice(Translate.t("Compiling Done"));
        return true;
    }

    public String parseString(String in)
    {
        int iStart;
        int iEnd;
        int iTest;
        String out;
        String start;
        String end;
        String mid;

        HashMap<String, String> tokens = mergeAllProperties();

        out = in;

        if (out == null) {
            return null;
        }

        iStart = out.indexOf("${");
        if (iStart == -1) {
            return out;
        }

        iEnd = out.indexOf("}", iStart);
        iTest = out.indexOf("${", iStart+1);
        while ((iTest > -1) && (iTest < iEnd)) {
            iStart = iTest;
            iTest = out.indexOf("${", iStart+1);
        }

        while (iStart != -1) {
            start = out.substring(0, iStart);
            end = out.substring(iEnd+1);
            mid = out.substring(iStart+2, iEnd);

            if (mid.equals("compiler.root")) {
                mid = editor.compiler.getFolder().getAbsolutePath();
            } else if (mid.equals("cache.root")) {
                mid = getCacheFolder().getAbsolutePath();
            } else if (mid.equals("core.root")) {
                mid = editor.core.getFolder().getAbsolutePath();
            } else if (mid.equals("board.root")) {
                mid = editor.board.getFolder().getAbsolutePath();
            } else if ((mid.length() >8) && (mid.substring(0, 8).equals("replace:"))) {
                // replace:string,find,replace
                String[] bits = mid.substring(3).split(",");
                if (bits.length != 3) {
                    mid = "Syntax error in replace - bad arg count";
                } else {
                    mid = bits[0].replaceAll(bits[1], bits[2]);
                }
            } else if ((mid.length() >3) && (mid.substring(0, 3).equals("if:"))) {
                String[] bits = mid.substring(3).split(",");
                if (bits.length < 2 || bits.length > 3) {
                    mid = "Syntax Error in if - bad arg count";
                } else {
                    String condition = bits[0];
                    String trueVal = bits[1];
                    String falseVal = bits.length == 3 ? bits[2] : "";

                    String[] conditionBits = condition.split("=");
                    if (conditionBits.length != 2) {
                        mid = "Syntax Error in if - bad comparison";
                    } else {
                        String leftVal = conditionBits[0].trim();
                        String rightVal = conditionBits[1].trim();
                        if (leftVal.equals(rightVal)) {
                            mid = trueVal;
                        } else {
                            mid = falseVal;
                        }
                    }
                }
            } else if ((mid.length() >8) && (mid.substring(0, 8).equals("foreach:"))) {
                // ${foreach:${variable.name},pattern-to, replace with %0}
                String content = mid.substring(8);
                int commaPos = content.indexOf(',');
                if (commaPos > 0) {
                    String data = content.substring(0, commaPos);
                    String format = content.substring(commaPos + 1);
                    String[] each = data.split("::");
                    String outString = "";
                    for (String chunk : each) {
                        String ns = format.replaceAll("%0", chunk);
                        if (outString.equals("")) {
                            outString = ns;
                        } else {    
                            outString = outString + "::" + ns;
                        }
                    }
                    mid = outString;
                } else {
                    mid = "Syntax Error in foreach";
                }
            } else if ((mid.length() > 5) && (mid.substring(0,5).equals("find:"))) {
                // ${find:${path.list},${file.name}}
                String content = mid.substring(5);
                int commaPos = content.indexOf(',');
                if (commaPos > 0) {
                    String data = content.substring(0, commaPos);
                    String fname = content.substring(commaPos + 1);
                    String[] each = data.split("::");

                    System.err.println("Looking for: " + fname);

                    String outString = "";
                    for (String chunk : each) {
                        System.err.println(chunk);
                        File f = new File(chunk);
                        if (f.exists() && f.isDirectory()) {
                            File ff = new File(f, fname);
                            System.err.println(ff.getAbsolutePath());
                            if (ff.exists()) {
                                mid = ff.getAbsolutePath();
                                break;
                            }
                        }
                    }
                } else {
                    mid = "Syntax Error in find";
                }
            } else if (mid.equals("verbose")) {
                if (Base.preferences.getBoolean("export.verbose")) 
                    mid = tokens.get("upload." + editor.programmer + ".verbose");
                else 
                    mid = tokens.get("upload." + editor.programmer + ".quiet");
            } else if (mid.equals("port.base")) {
                if (Base.isWindows()) {
                    mid = editor.getSerialPort();
                } else {
                    String sp = editor.getSerialPort();
                    mid = sp.substring(sp.lastIndexOf('/') + 1);
                }
            } else if (mid.equals("port")) {
                if (Base.isWindows()) {
                    mid = "\\\\.\\" + editor.getSerialPort();
                } else {
                    mid = editor.getSerialPort();
                }
            } else {
                mid = tokens.get(mid);
            }

            if (mid != null) {
                out = start + mid + end;
            } else {
                out = start + end;
            }
            iStart = out.indexOf("${");
            iEnd = out.indexOf("}", iStart);
            iTest = out.indexOf("${", iStart+1);
            while ((iTest > -1) && (iTest < iEnd)) {
                iStart = iTest;
                iTest = out.indexOf("${", iStart+1);
            }
        }

        // This shouldn't be needed as the methodology should always find any tokens put in
        // by other token replacements.  But just in case, eh?
        if (out != in) {
            out = parseString(out);
        }

        return out;
    }

    private File compileFile(File src) {

        String fileName = src.getName();
        String recipe = null;

        HashMap<String, String> all = mergeAllProperties();

        if (fileName.endsWith(".cpp")) {
            recipe = all.get("compile.cpp");
        }
    
        if (fileName.endsWith(".cxx")) {
            recipe = all.get("compile.cpp");
        }
    
        if (fileName.endsWith(".cc")) {
            recipe = all.get("compile.cpp");
        }
    
        if (fileName.endsWith(".c")) {
            recipe = all.get("compile.c");
        }
    
        if (fileName.endsWith(".S")) {
            recipe = all.get("compile.S");
        }

        if (recipe == null) {
            message("Error: I don't know how to compile " + fileName);
            return null;
        }

        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        File dest = new File(buildFolder, baseName + ".o");

        if (dest.exists()) {
            if (dest.lastModified() > src.lastModified()) {
                return dest;
            }
        }

        settings.put("build.path", buildFolder.getAbsolutePath());
        settings.put("source.name", src.getAbsolutePath());
        settings.put("object.name", dest.getAbsolutePath());

        String compiledString = parseString(recipe);

        if (!execAsynchronously(compiledString)) {
            return null;
        }
        if (!dest.exists()) {
            return null;
        }

        return dest;
    }

    public File getCacheFolder() {
        File cacheRoot = Base.getUserCacheFolder();
        File coreCache = new File(cacheRoot, editor.core.getName());
        File boardCache = new File(coreCache, editor.board.getName());
        if (!boardCache.exists()) {  
            boardCache.mkdirs();
        }
        return boardCache;
    }

    public File getCacheFile(String fileName) {
        File cacheFolder = getCacheFolder();
        File out = new File(cacheFolder, fileName);
        return out;
    }

    public boolean compileCore(File core, String name) {
        String[] f = new String[1];
        f[0] = core.getAbsolutePath();
        return compileCore(f, name);
    }

    public boolean compileCore(String[] core, String name) {
        File archive = getCacheFile("lib" + name + ".a");

        HashMap<String, String> all = mergeAllProperties();
        String recipe = all.get("compile.ar");

        settings.put("library", archive.getAbsolutePath());

        long archiveDate = 0;
        if (archive.exists()) {
            archiveDate = archive.lastModified();
        }

        ArrayList<File> fileList = new ArrayList<File>();

        for (String c : core) {
            File f = new File(editor.core.getFolder(), c);
            if (f.exists()) {
                fileList.addAll(findFilesInFolder(f, "S", false));
                fileList.addAll(findFilesInFolder(f, "c", false));
                fileList.addAll(findFilesInFolder(f, "cpp", false));
                fileList.addAll(findFilesInFolder(f, "cxx", false));
                fileList.addAll(findFilesInFolder(f, "cc", false));
            }
        }

        for (File f : fileList) {
            if (f.lastModified() > archiveDate) {
                File out = compileFile(f);
                if (out == null) {
                    return false;
                }
                settings.put("object.name", out.getAbsolutePath());
                String command = parseString(recipe);
                boolean ok = execAsynchronously(command);
                if (!ok) {
                    return false;
                }
                out.delete();
            }
        }
        return true;
    }

    public boolean compileLibrary(Library lib) {
        File archive = getCacheFile("lib" + lib.getName() + ".a");
        File utility = lib.getUtilityFolder();
        HashMap<String, String> all = mergeAllProperties();

        String recipe = all.get("compile.ar");

        settings.put("library", archive.getAbsolutePath());

        long archiveDate = 0;
        if (archive.exists()) {
            archiveDate = archive.lastModified();
        }

        ArrayList<File> fileList = lib.getSourceFiles();

        String origIncs = settings.get("includes");
        settings.put("includes", origIncs + "::" + "-I" + utility.getAbsolutePath());

        for (File f : fileList) {
            if (f.lastModified() > archiveDate) {
                File out = compileFile(f);
                if (out == null) {
                    return false;
                }
                settings.put("object.name", out.getAbsolutePath());
                String command = parseString(recipe);
                boolean ok = execAsynchronously(command);
                if (!ok) {
                    return false;
                }
                out.delete();
            }
        }
        settings.put("includes", origIncs);
        return true;
    }

    private List<File> convertFiles(File dest, List<File> sources) {
        List<File> objectPaths = new ArrayList<File>();
        HashMap<String, String> all = mergeAllProperties();

        settings.put("build.path", dest.getAbsolutePath());

        for (File file : sources) {
            File objectFile = new File(dest, file.getName() + ".o");
            objectPaths.add(objectFile);
            if (objectFile.exists() && objectFile.lastModified() > file.lastModified()) {
                if (Base.preferences.getBoolean("compiler.verbose")) {
                    message("Skipping " + file.getAbsolutePath() + " as not modified.\n", 1);
                }
                continue;
            }
            String sp = file.getAbsolutePath();
            String sf = folder.getAbsolutePath();
            sp = sp.substring(sf.length() + 1);

            String spf = file.getParentFile().getAbsolutePath();
            spf = spf.substring(sf.length() + 1);

            File destObjects = new File(dest, spf);
            destObjects.mkdirs();

            File destFile = new File(destObjects, file.getName());
            Base.copyFile(file, destFile);

            settings.put("source.name", sp);
            settings.put("object.name", objectFile.getAbsolutePath());

            if(!execAsynchronously(parseString(all.get("compile.bin"))))
                return null;
            if (!objectFile.exists()) 
                return null;
        }
        return objectPaths;
    } 

    private List<File> compileFiles(File dest, List<File> sSources, List<File> cSources, List<File> cppSources) {

        List<File> objectPaths = new ArrayList<File>();
        HashMap<String, String> all = mergeAllProperties();

        settings.put("build.path", dest.getAbsolutePath());

        for (File file : sSources) {
            File objectFile = new File(dest, file.getName() + ".o");
            objectPaths.add(objectFile);

            settings.put("source.name", file.getAbsolutePath());
            settings.put("object.name", objectFile.getAbsolutePath());

            if (objectFile.exists() && objectFile.lastModified() > file.lastModified()) {
                if (Base.preferences.getBoolean("compiler.verbose")) {
                    message("Skipping " + file.getAbsolutePath() + " as not modified.\n", 1);
                }
                continue;
            }

            if(!execAsynchronously(parseString(all.get("compile.S"))))
                return null;
            if (!objectFile.exists()) 
                return null;
        }

        for (File file : cSources) {
            File objectFile = new File(dest, file.getName() + ".o");
            objectPaths.add(objectFile);

            settings.put("source.name", file.getAbsolutePath());
            settings.put("object.name", objectFile.getAbsolutePath());

            if (objectFile.exists() && objectFile.lastModified() > file.lastModified()) {
                if (Base.preferences.getBoolean("compiler.verbose")) {
                    message("Skipping " + file.getAbsolutePath() + " as not modified.\n", 1);
                }
                continue;
            }

            if(!execAsynchronously(parseString(all.get("compile.c"))))
                return null;
            if (!objectFile.exists()) 
                return null;
        }

        for (File file : cppSources) {
            File objectFile = new File(dest, file.getName() + ".o");
            objectPaths.add(objectFile);

            settings.put("source.name", file.getAbsolutePath());
            settings.put("object.name", objectFile.getAbsolutePath());

            if (objectFile.exists() && objectFile.lastModified() > file.lastModified()) {
                if (Base.preferences.getBoolean("compiler.verbose")) {
                    message("Skipping " + file.getAbsolutePath() + " as not modified.\n", 1);
                }
                continue;
            }

            if(!execAsynchronously(parseString(all.get("compile.cpp"))))
                return null;
            if (!objectFile.exists()) 
                return null;
        }

        return objectPaths;
    }

    private String preparePaths(ArrayList<String> includePaths) {
        String includes = "";
        File suf = new File(folder, "utility");
        if (suf.exists()) {
            includes = includes + "-I" + suf.getAbsolutePath() + "::";
        }
        if (parameters.get("extension") != null) {
            includes = includes + "-I" + parameters.get("extension") + "::";
        }
        for (int i = 0; i < includePaths.size(); i++)
        {
            includes = includes + ("-I" + (String) includePaths.get(i)) + "::";
        }
        return includes;
    }

    private List<File> compileSketch() {
        List<File> sf = new ArrayList<File>();

        HashMap<String, String> all = mergeAllProperties();

        // We can only do this if the core supports binary conversions
        if (all.get("compile.bin") != null) {
            File obj = new File(folder, "objects");
            if (obj.exists()) {
                File buf = new File(buildFolder, "objects");
                buf.mkdirs();
                List<File> uf = convertFiles(buildFolder, findFilesInFolder(obj, null, true));
                if (uf != null) {
                    sf.addAll(uf);
                }
            }
        }

        ArrayList<File> compiledFiles = (ArrayList<File>) compileFiles(
                buildFolder,
                findFilesInFolder(buildFolder, "S", false),
                findFilesInFolder(buildFolder, "c", false),
                findFilesInFolder(buildFolder, "cpp", false)
        );
        if (compiledFiles != null) {
            sf.addAll(compiledFiles);
        }

        
        String boardFiles = all.get("build.files");
        if (boardFiles != null) {
            if (!boardFiles.equals("")) {
                ArrayList<File> sFiles = new ArrayList<File>();
                ArrayList<File> cFiles = new ArrayList<File>();
                ArrayList<File> cppFiles = new ArrayList<File>();

                String[] bfs = boardFiles.split("::");
                for (String bf : bfs) {
                    if (bf.endsWith(".c")) {
                        File f = new File(editor.board.getFolder(), bf);
                        if (f.exists()) {
                            cFiles.add(f);
                        }
                    } else if (bf.endsWith(".cpp")) {
                        File f = new File(editor.board.getFolder(), bf);
                        if (f.exists()) {
                            cppFiles.add(f);
                        }
                    } else 
                    if (bf.endsWith(".S")) {
                        File f = new File(editor.board.getFolder(), bf);
                        if (f.exists()) {
                            sFiles.add(f);
                        }
                    } 
                }
                sf.addAll(compileFiles(buildFolder, sFiles, cFiles, cppFiles));
            }
        }

        File suf = new File(folder, "utility");
        if (suf.exists()) {
            File buf = new File(buildFolder, "utility");
            buf.mkdirs();
            List<File> uf = compileFiles(
                buf,
                findFilesInFolder(suf, "S", true),
                findFilesInFolder(suf, "c", true),
                findFilesInFolder(suf, "cpp", true)
            );
            sf.addAll(uf);
        }

        return sf;
    } 

    static public ArrayList<File> findFilesInFolder(File folder,
            String extension, boolean recurse) {
        ArrayList<File> files = new ArrayList<File>();

        if (folder.listFiles() == null)
            return files;

        for (File file : folder.listFiles()) {
            if (file.getName().startsWith("."))
                continue; // skip hidden files

            if (file.isDirectory()) {
                if (recurse) {
                    files.addAll(findFilesInFolder(file, extension, recurse));
                }
                continue;
            }

            if (extension == null) {
                files.add(file);
                continue;
            }

            if (file.getName().endsWith("." + extension)) {
                files.add(file);
                continue;
            }

        }

        return files;
    }

    static private boolean createFolder(File folder) {
        if (folder.isDirectory())
            return false;
        if (!folder.mkdir())
            return false;
        return true;
    }

    private boolean compileLibraries () {
        for (Library lib : getImportedLibraries()) {
            if (!compileLibrary(lib)) {
                return false;
            }
        }
        return true;
    }

    private boolean compileLink(List<File> objectFiles, String coreLibs) {
        HashMap<String, String> all = mergeAllProperties();
        String baseCommandString = all.get("compile.link");
        String commandString = "";
        String objectFileList = "";

        settings.put("libraries.path", getCacheFolder().getAbsolutePath());

        String neverInclude = all.get("neverinclude");
        if (neverInclude == null) {
            neverInclude = "";
        }
        neverInclude.replaceAll(" ", "::");
        String neverIncludes[] = neverInclude.split("::");

        String liboption = all.get("compile.liboption");
        if (liboption == null) {
            liboption = "-l${library}";
        }

        String liblist = "";
        for (Library lib : getImportedLibraries()) {
            File aFile = getCacheFile("lib" + lib.getName() + ".a");
            String headerName = lib.getName() + ".h";
            boolean inc = true;
            for (String ni : neverIncludes) {
                if (ni.equals(headerName)) {
                    inc = false;
                }
            }

            if (aFile.exists() && inc) {
                settings.put("library", lib.getName());
                liblist += "::" + parseString(liboption);
            }
        }
        
        liblist += "::" + coreLibs;

        settings.put("libraries", liblist);

        for (File file : objectFiles) {
            objectFileList = objectFileList + file.getAbsolutePath() + "::";
        }

        settings.put("build.path", buildFolder.getAbsolutePath());
        settings.put("object.filelist", objectFileList);

        commandString = parseString(baseCommandString);
        return execAsynchronously(commandString);
    }

    private boolean compileEEP() {
        HashMap<String, String> all = mergeAllProperties();
        return execAsynchronously(parseString(all.get("compile.eep")));
    }

    private boolean compileLSS() {
        HashMap<String, String> all = mergeAllProperties();
        return execAsynchronously(parseString(all.get("compile.lss")));
    }

    private boolean compileHEX() {
        HashMap<String, String> all = mergeAllProperties();
        return execAsynchronously(parseString(all.get("compile.hex")));
    }

    public boolean execAsynchronously(String command) {
        HashMap<String, String> all = mergeAllProperties();
        if (command == null) {
            return true;
        }

        String[] commandArray = command.split("::");
        List<String> stringList = new ArrayList<String>();
        for(String string : commandArray) {
            string = string.trim();
            if(string != null && string.length() > 0) {
                stringList.add(string);
            }
        }

        stringList.set(0, stringList.get(0).replace("//", "/"));

        ProcessBuilder process = new ProcessBuilder(stringList);
//        process.redirectOutput(ProcessBuilder.Redirect.PIPE);
//        process.redirectError(ProcessBuilder.Redirect.PIPE);
        if (buildFolder != null) {
            process.directory(buildFolder);
        }
        Map<String, String> environment = process.environment();

        String pathvar = "PATH";
        if (Base.isWindows()) {
            pathvar = "Path";
        }

        String paths = all.get("path");
        if (paths != null) {
            for (String p : paths.split("::")) {
                String oPath = environment.get(pathvar);
                if (oPath == null) {
                    oPath = System.getenv(pathvar);
                }
                environment.put(pathvar, oPath + File.pathSeparator + parseString(p));
            }
        }

        String env = all.get("environment");
        if (env != null) {
            for (String ev : env.split("::")) {
                String[] bits = ev.split("=");
                if (bits.length == 2) {
                    environment.put(bits[0], parseString(bits[1]));
                }
            }
        }

        if (Base.preferences.getBoolean("compiler.verbose")) {
            message("\n", 1);
            for (String component : stringList) {
                message(component + " ", 1);
            }
            message("\n", 1);
        }

        Process proc;
        try {
            proc = process.start();
        } catch (Exception e) {
            message(e.toString(), 2);
            return false;
        }

        Base.processes.add(proc);

        MessageSiphon in = new MessageSiphon(proc.getInputStream(), this);
        MessageSiphon err = new MessageSiphon(proc.getErrorStream(), this);
        in.setChannel(1);
        err.setChannel(2);
        boolean running = true;
        int result = -1;
        while (running) {
            try {
                if (in.thread != null)
                    in.thread.join();
                if (err.thread != null)
                    err.thread.join();
                result = proc.waitFor();
                running = false;
            } catch (Exception ignored) { }
        }
        Base.processes.remove(proc);
        if (result == 0) {
            return true;
        }
        return false;
    }

    public boolean isUntitled() {
        if (folder.getParentFile().equals(Base.getTmpDir())) {
            if (name.startsWith("untitled")) {
                return true;
            }
        }
        return false;
    }


    // Scan all the "common" areas where examples may be found, and see if the 
    // start of the sketch folder's path matches any of them.

    public boolean isInternal() {
        String path = folder.getAbsolutePath();
        String basePath = Base.getContentFile(".").getAbsolutePath() + File.separator;
        String libsPath = Base.getSketchbookLibrariesPath() + File.separator;
        String cachePath = getCacheFolder().getAbsolutePath() + File.separator;
        String corePath = editor.core.getFolder().getAbsolutePath() + File.separator;
        String boardPath = editor.board.getFolder().getAbsolutePath() + File.separator;

        if (path.startsWith(basePath)) return true;
        if (path.startsWith(libsPath)) return true;
        if (path.startsWith(cachePath)) return true;
        if (path.startsWith(corePath)) return true;
        if (path.startsWith(boardPath)) return true;

        return false;    
    }

    public boolean validSourceFile(File f) {
        return validSourceFile(f.getName());
    }

    public boolean validSourceFile(String f) {
        if (f.endsWith(".ino")) return true;
        if (f.endsWith(".pde")) return true;
        if (f.endsWith(".cpp")) return true;
        if (f.endsWith(".cxx")) return true;
        if (f.endsWith(".cc")) return true;
        if (f.endsWith(".c")) return true;
        if (f.endsWith(".h")) return true;
        if (f.endsWith(".hh")) return true;
        if (f.endsWith(".S")) return true;
        return false;
    }

    public void cleanBuild() {
        System.gc();
        Base.removeDescendants(buildFolder);
    }

    // Merge all the layers into one single property file
    // class.  This will be the compiler, then the core, then
    // the board, then the pragmas and finally the run-time settings.

    public HashMap<String, String> mergeAllProperties() {
        HashMap<String, String> total = new HashMap<String, String>();

        if (editor.compiler == null) {
            System.err.println("==> No compiler");
        } else if (editor.compiler.getProperties() == null) {
            System.err.println("==> No compiler properties");
        } else {
            total.putAll(editor.compiler.getProperties().toHashMap(true));
        }

        if (editor.core == null) { 
            Base.error("==> No core");
        } else if (editor.core.getProperties() == null) {
            Base.error("==> No core properties");
        } else {
            total.putAll(editor.core.getProperties().toHashMap(true));
        }

        if (editor.board == null) {
            Base.error("==> No board");
        } else if (editor.board.getProperties() == null) {
            Base.error("==> No board properties");
        } else {
            total.putAll(editor.board.getProperties().toHashMap(true));
        }

        if (parameters != null) {
            total.putAll(parameters);
        }
        if (settings != null) {
            total.putAll(settings);
        }

        return total;
    }

    public void about() {
        editor.message("Sketch folder: " + folder.getAbsolutePath() + "\n", 0);
        editor.message("Selected board: " + editor.board.getName() + "\n", 0);
        editor.message("Board folder: " + editor.board.getFolder().getAbsolutePath() + "\n", 0);
    }

    public void handleNewFile() {
        String filename = JOptionPane.showInputDialog("Enter Filename");
        if (filename == null) {
            return;
        }
        File newFile = new File(folder, filename);
        if (!validSourceFile(newFile)) {
            JOptionPane.showMessageDialog(null, "File is not a valid source file", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (newFile.exists()) {
            JOptionPane.showMessageDialog(null, "File already exists", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            newFile.createNewFile();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error creating file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!newFile.exists()) {
            JOptionPane.showMessageDialog(null, "Error creating file", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        loadFile(newFile);
    }

    public void handleRenameTab() {
        SketchEditor activeTab = editor.getActiveTab();
        File oldFile = activeTab.getFile();
        if (oldFile.equals(getMainFile().getFile())) {
            JOptionPane.showMessageDialog(null, "You cannot rename the main sketch file.  Use 'File -> Save As' instead.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String oldName = oldFile.getName();
        String newName = JOptionPane.showInputDialog("Enter New Filename", oldName);
        if (newName == null) {
            return;
        }
        File newFile = new File(folder, newName);
        if (!validSourceFile(newFile)) {
            JOptionPane.showMessageDialog(null, "File is not a valid source file", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (newFile.exists()) {
            JOptionPane.showMessageDialog(null, "File already exists", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SketchFile sf = getFileByName(oldName);
        try {
            oldFile.renameTo(newFile);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error renaming file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
            
        if (!newFile.exists()) {
            JOptionPane.showMessageDialog(null, "Error renaming file", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        sf.setFile(newFile);
    }

    public File getLibrariesFolder() {
        return new File(folder, "libraries");
    }

    public void programBootloader(String programmer) {
        String uploadCommand;

        File bootloader = editor.board.getBootloader();
        String blName = bootloader.getAbsolutePath();

        HashMap<String, String> all = mergeAllProperties();
        editor.statusNotice(Translate.t("Burning Bootloader..."));
        settings.put("filename", blName);

        boolean isJava = true;
        uploadCommand = editor.board.get("upload." + programmer + ".command.java");
        if (uploadCommand == null) {
            uploadCommand = editor.core.get("upload." + programmer + ".command.java");
        }
        if (uploadCommand == null) {
            isJava = false;
            uploadCommand = editor.board.get("upload." + programmer + ".command." + Base.getOSFullName());
        }
        if (uploadCommand == null) {
            uploadCommand = editor.board.get("upload." + programmer + ".command." + Base.getOSName());
        }
        if (uploadCommand == null) {
            uploadCommand = editor.board.get("upload." + programmer + ".command");
        }
        if (uploadCommand == null) {
            uploadCommand = editor.core.get("upload." + programmer + ".command." + Base.getOSFullName());
        }
        if (uploadCommand == null) {
            uploadCommand = editor.core.get("upload." + programmer + ".command." + Base.getOSName());
        }
        if (uploadCommand == null) {
            uploadCommand = editor.core.get("upload." + programmer + ".command");
        }

        if (uploadCommand == null) {
            message("No upload command defined for board\n", 2);
            editor.statusNotice(Translate.t("Upload Failed"));
            return;
        }

   
        if (isJava) {
            Plugin uploader;
            uploader = Base.plugins.get(uploadCommand);
            if (uploader == null) {
                message("Upload class " + uploadCommand + " not found.\n", 2);
                editor.statusNotice(Translate.t("Upload Failed"));
                return;
            }
            try {
                if ((uploader.flags() & BasePlugin.LOADER) == 0) {
                    message(uploadCommand + "is not a valid loader plugin.\n", 2);
                    editor.statusNotice(Translate.t("Upload Failed"));
                    return;
                }
                uploader.run();
            } catch (Exception e) {
                editor.statusNotice(Translate.t("Upload Failed"));
                message(e.toString(), 2);
                return;
            }
            editor.statusNotice(Translate.t("Upload Complete"));
            return;
        }

        String[] spl;
        spl = parseString(uploadCommand).split("::");

        String executable = spl[0];
        if (Base.isWindows()) {
            executable = executable + ".exe";
        }

        File exeFile = new File(folder, executable);
        File tools;
        if (!exeFile.exists()) {
            tools = new File(folder, "tools");
            exeFile = new File(tools, executable);
        }
        if (!exeFile.exists()) {
            exeFile = new File(editor.core.getFolder(), executable);
        }
        if (!exeFile.exists()) {
            tools = new File(editor.core.getFolder(), "tools");
            exeFile = new File(tools, executable);
        }
        if (!exeFile.exists()) {
            exeFile = new File(executable);
        }
        if (exeFile.exists()) {
            executable = exeFile.getAbsolutePath();
        }

        spl[0] = executable;

        // Parse each word, doing String replacement as needed, trimming it, and
        // generally getting it ready for executing.

        String commandString = executable;
        for (int i = 1; i < spl.length; i++) {
            String tmp = spl[i];
            tmp = tmp.trim();
            if (tmp.length() > 0) {
                commandString += "::" + tmp;
            }
        }

        boolean dtr = false;
        boolean rts = false;


        String ulu = all.get("upload." + programmer + ".using");
        if (ulu == null) ulu = "serial";

        String doDtr = all.get("upload." + programmer + ".dtr");
        if (doDtr != null) {
            if (doDtr.equals("yes")) {
                dtr = true;
            }
        }
        String doRts = all.get("upload." + programmer + ".rts");
        if (doRts != null) {
            if (doRts.equals("yes")) {
                rts = true;
            }
        }

        if (ulu.equals("serial") || ulu.equals("usbcdc"))
        {
            editor.grabSerialPort();
            if (dtr || rts) {
                assertDTRRTS(dtr, rts);
            }
        }
        if (ulu.equals("usbcdc")) {
            try {
                String baud = all.get("upload." + programmer + ".reset.baud");
                if (baud != null) {
                    int b = Integer.parseInt(baud);
          //          editor.grabSerialPort();
                    
                    SerialPort serialPort = Serial.requestPort(editor.getSerialPort(), this, b);
                    if (serialPort == null) {
                        Base.error("Unable to lock serial port");
                        return;
                    }
                    Thread.sleep(1000);
                    Serial.releasePort(serialPort);
                    serialPort = null;
                    System.gc();
                    Thread.sleep(1500);
                }
            } catch (Exception e) {
                Base.error(e);
            }
        }

        boolean res = execAsynchronously(commandString);
        if (ulu.equals("serial") || ulu.equals("usbcdc"))
        {
            if (dtr || rts) {
                assertDTRRTS(false, false);
            }
            editor.releaseSerialPort();
        }
        if (res) {
            editor.statusNotice(Translate.t("Upload Complete"));
        } else {
            editor.statusNotice(Translate.t("Upload Failed"));
        }
    }

    public void exportSAR() {
        FileDialog fd = new FileDialog(editor,
                                   Translate.t("Export Sketch..."),
                                   FileDialog.SAVE);

        fd.setDirectory(System.getProperty("user.home"));

        fd.setFile(folder.getName() + ".sar");
        fd.setFilenameFilter(new FilenameFilter() {
            public boolean accept(File f, String n) {
                File nf = new File(f, n);
                if (nf.isDirectory()) {
                    return true;
                }
                if (nf.getName().endsWith(".sar")) {
                    return true;
                }
                return false;
            }
        });

        fd.setVisible(true);

        String newParentDir = fd.getDirectory();
        String newFileName = fd.getFile();

        if (newFileName == null) {
            return;
        }

        try {
            File archiveFile = new File(newParentDir, newFileName);

            if (archiveFile.exists()) {
                archiveFile.delete();
            }
            FileOutputStream outfile = new FileOutputStream(archiveFile);
            ZipOutputStream sar = new ZipOutputStream(outfile);
            sar.putNextEntry(new ZipEntry(folder.getName() + "/"));
            sar.closeEntry();
            addTree(folder, folder.getName(), sar);

            sar.putNextEntry(new ZipEntry("libraries" + "/"));
            sar.closeEntry();

            prepare();

            String libList = "";

            File sblp = Base.getSketchbookLibrariesFolder();

            for (Library lib : getImportedLibraries()) {
                File sbl = new File(sblp, lib.getFolder().getName());
                if (lib.getFolder().equals(sbl)) {
                    sar.putNextEntry(new ZipEntry("libraries" + "/" + lib.getFolder().getName() + "/"));
                    sar.closeEntry();
                    addTree(lib.getFolder(), "libraries/" + lib.getFolder().getName(), sar);
                    if (libList.equals("")) {
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
            mf.append("Sketch-Name: " + folder.getName() + "\n");
            mf.append("Libraries: " + libList + "\n");
            mf.append("Board: " + editor.board.getName() + "\n");
            mf.append("Core: " + editor.core.getName() + "\n");
            mf.append("Archived: " + timeStamp + "\n");

            String mfData = mf.toString();
            byte[] bytes = mfData.getBytes();
            sar.write(bytes, 0, bytes.length);
            sar.closeEntry();
            sar.flush();
            sar.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addTree(File dir, String sofar, ZipOutputStream zos) throws IOException {
        String files[] = dir.list();
        for (int i = 0; i < files.length; i++) {
            if (files[i].equals(".") || files[i].equals("..")) continue;

            File sub = new File(dir, files[i]);
            String nowfar = (sofar == null) ?  files[i] : (sofar + "/" + files[i]);

            if (sub.isDirectory()) {
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

    public void importSAR() {
        FileDialog fd = new FileDialog(editor,
                                   Translate.t("Import Sketch..."),
                                   FileDialog.LOAD);

        fd.setDirectory(System.getProperty("user.home"));

        fd.setFile(folder.getName() + ".sar");
        fd.setFilenameFilter(new FilenameFilter() {
            public boolean accept(File f, String n) {
                File nf = new File(f, n);
                if (nf.isDirectory()) {
                    return true;
                }
                if (nf.getName().endsWith(".sar")) {
                    return true;
                }
                return false;
            }
        });

        fd.setVisible(true);

        String newParentDir = fd.getDirectory();
        String newFileName = fd.getFile();

        if (newFileName == null) {
            return;
        }

        File newFile = new File(fd.getDirectory(), fd.getFile());
        if (!newFile.exists()) {
            return;
        }

        doImportSAR(newFile);
    }
    public boolean willDoImport = false;
    public void doImportSAR(File sarFile) {

        try {
            JarFile sarfile = new JarFile(sarFile);
            Manifest manifest = sarfile.getManifest();
            Attributes manifestContents = manifest.getMainAttributes();

            String sketchName = manifestContents.getValue("Sketch-Name");
            String author = manifestContents.getValue("Author");
            String libs = manifestContents.getValue("Libraries");
            String brd = manifestContents.getValue("Board");
            String cr = manifestContents.getValue("Core");
            String archived = manifestContents.getValue("Archived");

            final JDialog dialog = new JDialog(editor, "Import SAR", true);

            EmptyBorder bdr = new EmptyBorder(4, 4, 4, 4);
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBorder(bdr);

            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridwidth = 1;
            c.gridheight = 1;
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 1.0;

            JLabel label = new JLabel("Sketch Name:");
            label.setBorder(bdr);
            panel.add(label, c);
            c.gridx = 1;
            label = new JLabel(sketchName);
            label.setBorder(bdr);
            panel.add(label, c);
            c.gridx = 0;
            c.gridy++;

            label = new JLabel("Author:");
            label.setBorder(bdr);
            panel.add(label, c);
            c.gridx = 1;
            label = new JLabel(author);
            label.setBorder(bdr);
            panel.add(label, c);
            c.gridx = 0;
            c.gridy++;

            label = new JLabel("Board:");
            label.setBorder(bdr);
            panel.add(label, c);
            c.gridx = 1;
            label = new JLabel(brd);
            label.setBorder(bdr);
            panel.add(label, c);
            c.gridx = 0;
            c.gridy++;

            label = new JLabel("Core:");
            label.setBorder(bdr);
            panel.add(label, c);
            c.gridx = 1;
            label = new JLabel(cr);
            label.setBorder(bdr);
            panel.add(label, c);
            c.gridx = 0;
            c.gridy++;

            c.gridwidth = 2;
            label = new JLabel("Libraries:");
            label.setBorder(bdr);
            panel.add(label, c);
            c.gridwidth = 1;
            c.gridy++;

            HashMap<String, JCheckBox> libcheck = new HashMap<String, JCheckBox>();
            String[] libarr = libs.split(" ");
            c.gridx = 1;
            c.gridwidth = 2;
            for (String l : libarr) {
                JCheckBox cb = new JCheckBox(l);
                cb.setBorder(bdr);
                File elib = new File(Base.getSketchbookLibrariesFolder(), l);
                if (elib.exists() && elib.isDirectory()) {
                    cb.setSelected(false);
                } else {
                    cb.setSelected(true);
                }
                panel.add(cb, c);
                libcheck.put(l, cb);
                c.gridy++;
            }
            c.gridx = 0;
            c.gridwidth = 1;
            final Sketch me = this;

            JButton cancel = new JButton(Translate.t("Cancel"));
            cancel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    me.willDoImport = false;
                    dialog.dispose();
                }
            });
            JButton impt = new JButton(Translate.t("Import"));
            impt.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    me.willDoImport = true;
                    dialog.dispose();
                }
            });

            panel.add(cancel, c);
            c.gridx = 1;
            panel.add(impt, c);

            dialog.setContentPane(panel);
            dialog.pack();
            dialog.setLocationRelativeTo(editor);
            dialog.setVisible(true);

            if (willDoImport) {
                File targetDir = new File(Base.getSketchbookFolder(), sketchName);
                int n = 0;
                if (targetDir.exists()) {
                    Object[] options = { "Yes", "No" };
                    n = JOptionPane.showOptionDialog(editor,
                        "The sketch " + sketchName + " already exists.\n" +
                        "Do you want to overwrite it?",
                        "Sketch Exists",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[1]);
                    if (n == 0) {
                        Base.removeDir(targetDir);
                    }
                }

                if (n == 0) {
                    byte[] buffer = new byte[1024];
                    ZipInputStream zis = new ZipInputStream(new FileInputStream(sarFile));
                    ZipEntry ze = zis.getNextEntry();
                    while (ze != null) {
                        String fileName = ze.getName();
                        String spl[] = fileName.split("/");

                        if (spl[0].equals("META-INF")) {
                            ze = zis.getNextEntry();
                            continue;
                        }


                        if (spl[0].equals("libraries")) {
                            if (spl.length > 1) {
                                if (libcheck.get(spl[1]) == null) {
                                    // This is a library we don't know about - ignore it
                                    ze = zis.getNextEntry();
                                    continue;
                                }
                                if (libcheck.get(spl[1]).isSelected() == false) {
                                    // The library isn't selected for import
                                    ze = zis.getNextEntry();
                                    continue;
                                }
                            }
                        }

                        File newFile = new File(Base.getSketchbookFolder(), fileName);

                        new File(newFile.getParent()).mkdirs();

                        if (ze.isDirectory()) {
                            newFile.mkdirs();
                        } else {
                            FileOutputStream fos = new FileOutputStream(newFile);
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                            fos.close();
                        }
                        ze = zis.getNextEntry();
                    }
                    zis.closeEntry();
                    zis.close();
                    Base.gatherLibraries();
                    editor.rebuildImportMenu();
                    Base.createNewEditor(new File(Base.getSketchbookFolder(), sketchName).getAbsolutePath());
                }                
            }
        } catch (Exception e) {
        }
    }

}
