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

package uecide.app;

import uecide.app.preproc.*;
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

public class Library {
    public ArrayList<String> requiredLibraries;
    public File folder;
    public String name;
    public File examplesFolder = null;
    public ArrayList<File> sourceFiles;
    public ArrayList<File> archiveFiles;
    public File mainInclude;
    public File utilityFolder;
    public HashMap<String, File>examples;
    public String type;
    public String core;
    public int compiledPercent = 0;

    public boolean valid = false;

    public boolean buildLibrary = false;

    public Library(File hdr, String t, String c) {
        type = t;
        core = c;
        File root = hdr.getParentFile();
        name = hdr.getName().substring(0, hdr.getName().indexOf(".h"));;
        folder = root;
        mainInclude = hdr;
        if (!mainInclude.exists()) {
            return;
        }
        utilityFolder = new File(root, "utility");
        examplesFolder = new File(root, "examples");
        rescan();
        valid = true;
    }

    public void rescan() {
        requiredLibraries = new ArrayList<String>();
        sourceFiles = new ArrayList<File>();
        archiveFiles = new ArrayList<File>();
        examples = new HashMap<String, File>();

        sourceFiles.addAll(Sketch.findFilesInFolder(folder, "cpp", false));
        sourceFiles.addAll(Sketch.findFilesInFolder(folder, "c", false));
        sourceFiles.addAll(Sketch.findFilesInFolder(folder, "S", false));
        archiveFiles.addAll(Sketch.findFilesInFolder(folder, "a", false));

        if (utilityFolder.exists() && utilityFolder.isDirectory()) {
            sourceFiles.addAll(Sketch.findFilesInFolder(utilityFolder, "cpp", true));
            sourceFiles.addAll(Sketch.findFilesInFolder(utilityFolder, "c", true));
            sourceFiles.addAll(Sketch.findFilesInFolder(utilityFolder, "S", true));
            archiveFiles.addAll(Sketch.findFilesInFolder(utilityFolder, "a", true));
        }

        if (examplesFolder.exists() && examplesFolder.isDirectory()) {
            File[] list = examplesFolder.listFiles();
            for (File f : list) {
                if (f.isDirectory()) {
                    String sketchName = f.getName();
                    File sketchFile = new File(f, sketchName + ".pde");
                    if (sketchFile.exists()) {
                        examples.put(sketchName, f);
                    } else {
                        sketchFile = new File(f, sketchName + ".ino");
                        if (sketchFile.exists()) {
                            examples.put(sketchName, f);
                        }
                    }
                }
            }
        }

        probedFiles = new ArrayList<String>();

        gatherIncludes(mainInclude);

        for (File f : sourceFiles) {
            gatherIncludes(f);
        }
    }

    ArrayList<String> probedFiles;

    public void gatherIncludes(File f) {
        String[] data;
        try {
            FileReader in = new FileReader(f);
            StringBuilder contents = new StringBuilder();
            char[] buffer = new char[4096];
            int read = 0;
            do {
                contents.append(buffer, 0, read);
                read = in.read(buffer);
            } while (read >= 0);
            data = contents.toString().split("\n");
        } catch (Exception e) {
            Base.error(e);
            return;
        }
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

                // If the file is not local to the library then go ahead and add it.
                // Local files override other libraries.
                File localFile = new File(folder, i);
                if (!localFile.exists()) {
                    if (requiredLibraries.indexOf(i) == -1) {
                        requiredLibraries.add(i);
                    }
                } else {
                    // This is a local header.  We should check it for libraries.
                    if (probedFiles.indexOf(localFile.getAbsolutePath()) == -1) {
                        probedFiles.add(localFile.getAbsolutePath());
                        gatherIncludes(localFile);
                    }
                }
            }
        }
    }

    public boolean isValid() {
        return valid;
    }

    public String getName() {
        return name;
    }

    public File getFolder() {
        return folder;
    }

    public File getUtilityFolder() {
        return utilityFolder;
    }

    public String getInclude() {
        return "#include <" + mainInclude.getName() + ">\n";
    }

    public ArrayList<File> getSourceFiles() {
        return sourceFiles;
    }

    public ArrayList<String> getRequiredLibraries() {
        return requiredLibraries;
    }

    public String getType() {
        return type;
    }

    public boolean isCore() {
        return type.equals("core");
    }

    public boolean isContributed() {
        return type.equals("contributed");
    }

    public boolean isSketch() {
        return type.equals("sketch");
    }

    public String getCore() {
        return core;
    }

    public boolean worksWith(String c) {
        if (core.equals("all")) {
            return true;
        }
        return core.equals(c);
    }

    public String getArchiveName() {
        return "lib" + type + "_" + name + ".a";
    }

    public String getLinkName() {
        return type + "_" + name;
    }

    public File getExamplesFolder() {
        return examplesFolder;
    }

    public String toString() {
        return getName();
    }

    public void setCompiledPercent(int p) {
        compiledPercent = p;
    }

    public int getCompiledPercent() {
        return compiledPercent;
    }
}

