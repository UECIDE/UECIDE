/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-10 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package uecide.app;

import uecide.app.debug.*;
import uecide.app.preproc.*;
import processing.core.*;

import java.util.regex.*;



import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.apache.log4j.BasicConfigurator;
//import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

/**
 * Stores information about files in the current sketch
 */
public class Sketch implements MessageConsumer {
    public String name;     // The name of the sketch
    public File folder;
    public Editor editor;
    public File buildFolder;
    public String uuid; // used for temporary folders etc

    public ArrayList<SketchFile> sketchFiles = new ArrayList<SketchFile>();

    public HashMap<String, File> importedLibraries = new HashMap<String, File>();

    public HashMap<String, String> settings = new HashMap<String, String>();
    public HashMap<String, File> importToLibraryTable;
    public boolean runInVerboseMode = false;

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
        if (!loadFile(new File(folder, folder.getName() + ".ino"))) {
            loadFile(new File(folder, folder.getName() + ".pde"));
        }
        File fileList[] = folder.listFiles();
        for (File f : fileList){
            String lcn = f.getName().toLowerCase();
            if (lcn.endsWith(".h") || lcn.endsWith(".c") || lcn.endsWith(".cpp") || lcn.endsWith(".s")) {
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

    public void prepare() {
        importedLibraries = new HashMap<String, File>();
        for (SketchFile f : sketchFiles) {
            String lcn = f.file.getName().toLowerCase();
            if (lcn.endsWith(".h") || lcn.endsWith(".c") || lcn.endsWith(".cpp") || lcn.endsWith(".s")) {
                f.writeToFolder(buildFolder);
            }
            if (lcn.endsWith(".pde") || lcn.endsWith(".ino")) {

                String rawData = f.textArea.getText();
                PdePreprocessor proc = new PdePreprocessor();

                f.includes = gatherIncludes(f);
                f.prototypes = proc.prototypes(rawData).toArray(new String[0]);
                StringBuilder sb = new StringBuilder();
                f.headerLines = 0;
                String coreHeader = editor.board.getAny("core.header", "");
                if (coreHeader != "") {
                    sb.append("#include <" + coreHeader + ">\n");
                    f.headerLines ++;
                }

                sb.append("\n");
                f.headerLines ++;
                
                for (String prototype : f.prototypes) {
                    sb.append(prototype + "\n");
                    f.headerLines++;
                }

                sb.append("\n");
                f.headerLines ++;

                sb.append(f.textArea.getText());

                String newFileName = f.file.getName();
                int dot = newFileName.lastIndexOf(".");
                newFileName = newFileName.substring(0, dot);
                newFileName = newFileName + "." + editor.board.getAny("build.extension","cpp");

                try {
                    PrintWriter pw = new PrintWriter(new File(buildFolder, newFileName));
                    pw.print(sb.toString());
                    pw.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String stripComments(String data) {
        String out = data;
        boolean found = false;
        do {
            found = false;
            int start = out.indexOf("/*");
            int end = out.indexOf("*/");
            if (start >= 0 && end >= 0) {
                String front = out.substring(0, start);
                String back = out.substring(end);
                out = front + back;
                found = true;
            }
        } while(found);
        return out;
    }

    public String[] gatherIncludes(SketchFile f) {
        String[] data = stripComments(f.textArea.getText()).split("\n");
        ArrayList<String> includes = new ArrayList<String>();

        for (String line : data) {
            line = line.trim();
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
                if (importToLibraryTable.get(i) != null) {
                    includes.add(i);
                    if (importedLibraries.get(i) == null) {
                        importedLibraries.put(i, importToLibraryTable.get(i));
                    }
                }
            }
        }
        return includes.toArray(new String[includes.size()]);
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
        if (!build()) {
            return false;
        }
        return editor.board.upload(new File(buildFolder, name).getAbsolutePath());
    }

    public boolean build() {
        prepare();
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

    public File[] getImportedLibraries() {
        ArrayList<File> libFiles = new ArrayList<File>();
      
        String[] entries = (String[]) importedLibraries.keySet().toArray(new String[0]);

        for (String e : entries) {
            libFiles.add(importedLibraries.get(e));
        }
        return libFiles.toArray(new File[libFiles.size()]);
    }

    public ArrayList<String> getIncludePaths() {
        ArrayList<String> libFiles = new ArrayList<String>();
      
        String[] entries = (String[]) importedLibraries.keySet().toArray(new String[0]);
        libFiles.add(buildFolder.getAbsolutePath());
        libFiles.add(editor.board.getFolder().getAbsolutePath());
        libFiles.add(editor.core.getAPIFolder().getAbsolutePath());
        libFiles.add(editor.core.getLibraryFolder().getAbsolutePath());

        for (String e : entries) {
            libFiles.add(importedLibraries.get(e).getAbsolutePath());
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

        fd.setDirectory(Preferences.get("sketchbook.path"));

        fd.setFile(name);

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
        save();
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

        String lcn = sourceFile.getName().toLowerCase();
        if (!(lcn.endsWith(".h") || lcn.endsWith(".cpp") || lcn.endsWith(".c") || lcn.endsWith(".s"))) {
            Base.showWarning(Translate.t("Error Adding File"),Translate.w("Error: you can only add .c, .cpp, .h or .S files to a sketch", 40, "\n"), null);
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
        File testFile = new File(folder, ".testWrite");
        boolean canWrite = false;
        try {
            testFile.createNewFile();
            if (testFile.exists()) {
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

    public void message(String m) {
        message(m, 1);
    }

    public void message(String m, int chan) {
        if (m.trim() != "") {
            if (chan == 2) {
                System.err.print(m);
            } else {
                System.out.print(m);
            }
        }
    }

    public boolean compile() {
        ArrayList<String> includePaths;
        List<File> objectFiles = new ArrayList<File>();
        List<File> tobjs;

        includePaths = getIncludePaths();

        settings.put("filename", name);
        settings.put("includes", preparePaths(includePaths));

        tobjs = compileSketch();
        if (tobjs == null) {
            return false;
        }
        objectFiles.addAll(tobjs);
        setCompilingProgress(30);

        tobjs = compileLibraries();
        if (tobjs == null) {
            return false;
        }

        objectFiles.addAll(tobjs);
        setCompilingProgress(40);

        if (!compileCore()) {
            return false;
        }
        setCompilingProgress(50);

        settings.put("filename", name);
        if (!compileLink(objectFiles)) {
            return false;
        }
        setCompilingProgress(60);

        if (!compileEEP()) {
            return false;
        }
        setCompilingProgress(70);

        if (!compileHEX()) {
            return false;
        }
        setCompilingProgress(80);

        try {
            Sizer sizer = new Sizer(editor);
            sizer.computeSize();
            System.out.println("Program Size:");
            System.out.println("  Flash: " + sizer.progSize() + " bytes");
            System.out.println("  RAM:   " + sizer.ramSize() + " bytes");
        } catch (Exception e) {
            e.printStackTrace();
        }

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

        HashMap<String, String> tokens = new HashMap<String, String>();
        
        tokens.putAll(editor.core.corePreferences);
        tokens.putAll(editor.board.boardPreferences);
        tokens.putAll(settings);

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

            if (mid.equals("core.root")) {
                mid = editor.core.getFolder().getAbsolutePath();
            } else if ((mid.length() > 5) && (mid.substring(0,5).equals("find:"))) {
                String f = mid.substring(5);

                File found;
                found = new File(editor.board.getFolder(), f);
                if (!found.exists()) {
                    found = new File(editor.core.getAPIFolder(), f);
                }
                if (!found.exists()) {
                    mid = "NOTFOUND";
                } else {
                    mid = found.getAbsolutePath();
                }
            } else if (mid.equals("verbose")) {
                if (runInVerboseMode)
                    mid = editor.board.getAny("upload.verbose", "");
                else 
                    mid = editor.board.getAny("upload.quiet", "");
            } else if (mid.equals("port")) {
                if (Base.isWindows()) 
                    mid = "\\\\.\\" + Preferences.get("serial.port");
                else 
                    mid = Preferences.get("serial.port");
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
        // by other token replaceements.  But just in case, eh?
        if (out != in) {
            out = parseString(out);
        }

        return out;
    }

    private List<File> compileFiles(File dest, List<File> sSources, List<File> cSources, List<File> cppSources) {

        List<File> objectPaths = new ArrayList<File>();

        settings.put("build.path", dest.getAbsolutePath());

        for (File file : sSources) {
            File objectFile = new File(dest, file.getName() + ".o");
            objectPaths.add(objectFile);

            settings.put("source.name", file.getAbsolutePath());
            settings.put("object.name", objectFile.getAbsolutePath());

            if(!execAsynchronously(parseString(editor.core.get("recipe.S.o.pattern"))))
                return null;
            if (!objectFile.exists()) 
                return null;
        }

        for (File file : cSources) {
            File objectFile = new File(dest, file.getName() + ".o");
            objectPaths.add(objectFile);

            settings.put("source.name", file.getAbsolutePath());
            settings.put("object.name", objectFile.getAbsolutePath());

            if(!execAsynchronously(parseString(editor.core.get("recipe.c.o.pattern"))))
                return null;
            if (!objectFile.exists()) 
                return null;
        }

        for (File file : cppSources) {
            File objectFile = new File(dest, file.getName() + ".o");
            objectPaths.add(objectFile);

            settings.put("source.name", file.getAbsolutePath());
            settings.put("object.name", objectFile.getAbsolutePath());

            if(!execAsynchronously(parseString(editor.core.get("recipe.cpp.o.pattern"))))
                return null;
            if (!objectFile.exists()) 
                return null;
        }

        return objectPaths;
    }

    private String preparePaths(ArrayList<String> includePaths) {
        String includes = "";
        for (int i = 0; i < includePaths.size(); i++)
        {
            includes = includes + (" -I" + (String) includePaths.get(i)) + "::";
        }
        return includes;
    }

    private List<File> compileSketch() {
        return compileFiles(
                buildFolder,
                findFilesInFolder(buildFolder, "S", false),
                findFilesInFolder(buildFolder, "c", false),
                findFilesInFolder(buildFolder, "cpp", false));
    } 

    static public ArrayList<File> findFilesInFolder(File folder,
            String extension, boolean recurse) {
        ArrayList<File> files = new ArrayList<File>();

        if (folder.listFiles() == null)
            return files;

        for (File file : folder.listFiles()) {
            if (file.getName().startsWith("."))
                continue; // skip hidden files

            if (file.getName().endsWith("." + extension))
                files.add(file);

            if (recurse && file.isDirectory()) {
                files.addAll(findFilesInFolder(file, extension, true));
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

    private List<File> compileLibraries () {
        List<File> objectFiles = new ArrayList<File>();
        List<File> tobjs;

        String origIncs = settings.get("includes");
        
        for (File libraryFolder : getImportedLibraries())
        {
            File outputFolder = new File(buildFolder, libraryFolder.getName());
            File utilityFolder = new File(libraryFolder, "utility");
            if (!createFolder(outputFolder))
                return null;
            // this library can use includes in its utility/ folder
            settings.put("includes", origIncs + "::-I" + utilityFolder.getAbsolutePath());
            tobjs = compileFiles(
                    outputFolder,
                    findFilesInFolder(libraryFolder, "S", false),
                    findFilesInFolder(libraryFolder, "c", false),
                    findFilesInFolder(libraryFolder, "cpp", false));
            if (tobjs == null) {
                return null;
            }

            objectFiles.addAll(tobjs);

            outputFolder = new File(outputFolder, "utility");
            if (!createFolder(outputFolder))
                return null;
            tobjs = compileFiles(
                    outputFolder,
                    findFilesInFolder(utilityFolder, "S", false),
                    findFilesInFolder(utilityFolder, "c", false),
                    findFilesInFolder(utilityFolder, "cpp", false));
            // other libraries should not see this library's utility/ folder
            if (tobjs == null) {
                return null;
            }
            objectFiles.addAll(tobjs);
            settings.put("includes", origIncs);
        }
        return objectFiles;
    }

    private boolean compileCore() {

        List<File> objectFiles = new ArrayList<File>();
        ArrayList<String> includePaths =  new ArrayList();

        File corePath = editor.core.getAPIFolder();

        includePaths.add(corePath.getAbsolutePath());
        includePaths.add(editor.board.getFolder().getAbsolutePath());

        ArrayList<File> sFiles = findFilesInFolder(corePath, "S", true);
        ArrayList<File> cFiles = findFilesInFolder(corePath, "c", true);
        ArrayList<File> cppFiles = findFilesInFolder(corePath, "cpp", true);

        String boardFiles = editor.board.getAny("build.files", "");
        if (boardFiles != null) {
            String[] bfl = boardFiles.split("::");
            for (String bf : bfl) {
                File f = new File(editor.board.getFolder(), bf);
                if (f.exists()) {
                    if (bf.endsWith(".S")) {
                        sFiles.add(f);
                    }
                    if (bf.endsWith(".c")) {
                        cFiles.add(f);
                    }
                    if (bf.endsWith(".cpp")) {
                        cppFiles.add(f);
                    }
                }
            }
        }

        List<File> coreObjectFiles   = compileFiles(buildFolder, sFiles, cFiles, cppFiles);

        if (coreObjectFiles == null) {
            return false;
        }

        for (File file : coreObjectFiles) {
            settings.put("object.name", file.getAbsolutePath());

            if (!execAsynchronously(parseString(editor.core.get("recipe.ar.pattern")))) {
                return false;
            }
        }
        return true;
    }

    private boolean compileLink(List<File> objectFiles) {
        String baseCommandString = editor.board.getAny("recipe.c.combine.pattern", "");
        String commandString = "";
        String objectFileList = "";

        for (File file : objectFiles) {
            objectFileList = objectFileList + file.getAbsolutePath() + "::";
        }

        File ldscript = editor.board.getLDScript();

        String ldScriptFile;
        String ldScriptPath;
        if (ldscript == null) {
            ldScriptFile = "";
            ldScriptPath = "";
        } else {
            ldScriptFile = ldscript.getName();
            ldScriptPath = ldscript.getParentFile().getAbsolutePath();
        }
        settings.put("build.path", buildFolder.getAbsolutePath());
        settings.put("object.filelist", objectFileList);

        commandString = parseString(baseCommandString);
        return execAsynchronously(commandString);
    }

    private boolean compileEEP() {
        return execAsynchronously(parseString(editor.core.get("recipe.objcopy.eep.pattern")));
    }

    private boolean compileHEX() {
        return execAsynchronously(parseString(editor.core.get("recipe.objcopy.hex.pattern")));
    }

    public boolean execAsynchronously(String command) {
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
        if (buildFolder != null) {
            process.directory(buildFolder);
        }
        Map<String, String> environment = process.environment();
        
        String[] env = editor.board.getAny("environment","").split("::");
        for (String ev : env) {
            String[] bits = ev.split("=");
            if (bits.length == 2) {
                environment.put(bits[0], bits[1]);
            }
        }

        if (runInVerboseMode) {
            for (String component : stringList) {
                System.out.print("[" + component + "] ");
            }
            System.out.println("");
        }

        Process proc;
        try {
            proc = process.start();
        } catch (Exception e) {
            e.printStackTrace();
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
            } catch (InterruptedException ignored) { }
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
}

