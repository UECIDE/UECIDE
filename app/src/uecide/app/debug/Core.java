
/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Core - represents a hardware platform
  Part of the Arduino project - http://www.arduino.cc/

  Copyright (c) 2009 David A. Mellis

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
  
  $Id$
*/

package uecide.app.debug;

import java.io.*;
import java.util.*;

import uecide.app.Preferences;
import uecide.app.Base;
import uecide.app.Sketch;
import uecide.app.SketchCode;
import java.text.MessageFormat;




import org.apache.log4j.BasicConfigurator;
//import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;


public class Core implements MessageConsumer {
    private String name;
    private File folder;
    private Map corePreferences;
    private boolean valid;
    private File api;
    private boolean runInVerboseMode;

    static Logger logger = Logger.getLogger(Base.class.getName());
  
    public Core(File folder) {
        this.folder = folder;

        File coreFile = new File(folder,"core.txt");

        valid = false;

        try {
            if(coreFile.exists()) {
                corePreferences = new LinkedHashMap();
                Preferences.load(new FileInputStream(coreFile), corePreferences);
                this.name = folder.getName();
                this.api = new File(folder, get("library.core.path","api"));
            }
            valid = true;
        } catch (Exception e) {
            System.err.println("Error loading core from " + coreFile + ": " + e);
        }

    }

    public File getLibraryFolder()
    {
        File lf = new File(folder, get("library.path", "libraries"));
        return lf;
    }

    public String getName() { 
        return name; 
    }

    public File getFolder() { 
        return folder; 
    }

    public File getAPIFolder() {
        return api;
    }

    public boolean isValid() {
        return valid;
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

    public String get(String k) {
        return (String) corePreferences.get(k);
    }

    public String get(String k, String d) {
        if (get(k) == null) {
            return d;
        }
        return get(k);
    }

    public boolean compile(Sketch sketch, String buildPath,String primaryClassName) {
        ArrayList<String> includePaths;
        List<File> objectFiles = new ArrayList<File>();
        List<File> tobjs;

        includePaths = getIncludes(sketch);

        Base.selectedBoard.set("filename", primaryClassName);
        Base.selectedBoard.set("includes", preparePaths(includePaths));

        tobjs = compileSketch(buildPath);
        if (tobjs == null) {
            return false;
        }
        objectFiles.addAll(tobjs);
        sketch.setCompilingProgress(30);

        tobjs = compileLibraries(sketch, buildPath);
        if (tobjs == null) {
            return false;
        }

        objectFiles.addAll(tobjs);
        sketch.setCompilingProgress(40);

        if (!compileCore(buildPath)) {
            return false;
        }
        sketch.setCompilingProgress(50);

        Base.selectedBoard.set("filename", primaryClassName);
        if (!compileLink(buildPath, objectFiles)) {
            return false;
        }
        sketch.setCompilingProgress(60);

        if (!compileEEP(buildPath)) {
            return false;
        }
        sketch.setCompilingProgress(70);

        if (!compileHEX(buildPath)) {
            return false;
        }
        sketch.setCompilingProgress(80);

        return true;
    }

    static public ArrayList<File> findFilesInPath(String path,
            String extension, boolean recurse) {
        return findFilesInFolder(new File(path), extension, recurse);
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


    public ArrayList<String> getIncludes(Sketch s) {
        ArrayList<String> paths = new ArrayList();
        paths.add(s.getFolder().toString());
        paths.add(api.getAbsolutePath());
        paths.add(Base.selectedBoard.getFolder().getAbsolutePath());

        for (File file : s.getImportedLibraries()) {
            paths.add(file.getPath());
        }
        return paths;
    }

    private List<File> compileFiles(
            String buildPath,
            List<File> sSources, List<File>
            cSources,
            List<File> cppSources) {

        List<File> objectPaths = new ArrayList<File>();

        Base.selectedBoard.set("build.path", buildPath);

        for (File file : sSources) {
            String objectPath = buildPath + File.separator + file.getName()
                    + ".o";
            File objectFile = new File(objectPath);
            objectPaths.add(objectFile);

            Base.selectedBoard.set("source.name", file.getAbsolutePath());
            Base.selectedBoard.set("object.name", objectPath);

            if(!Base.selectedBoard.execAsynchronously(Base.selectedBoard.parseString(get("recipe.S.o.pattern"))))
                return null;
            if (!objectFile.exists()) 
                return null;
        }

        for (File file : cSources) {
            String objectPath = buildPath + File.separator + file.getName()
                    + ".o";
            File objectFile = new File(objectPath);
            objectPaths.add(objectFile);

            Base.selectedBoard.set("source.name", file.getAbsolutePath());
            Base.selectedBoard.set("object.name", objectPath);

            if(!Base.selectedBoard.execAsynchronously(Base.selectedBoard.parseString(get("recipe.c.o.pattern"))))
                return null;
            if (!objectFile.exists()) 
                return null;
        }

        for (File file : cppSources) {
            String objectPath = buildPath + File.separator + file.getName()
                    + ".o";
            File objectFile = new File(objectPath);
            objectPaths.add(objectFile);

            Base.selectedBoard.set("source.name", file.getAbsolutePath());
            Base.selectedBoard.set("object.name", objectPath);

            if(!Base.selectedBoard.execAsynchronously(Base.selectedBoard.parseString(get("recipe.cpp.o.pattern"))))
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

    private boolean execAsynchronously(String command) {
        String[] commandArray = command.split("::");
        List<String> stringList = new ArrayList<String>();
        Process process;

        for(String string : commandArray) {
            string = string.trim();
            if(string != null && string.length() > 0) {
                stringList.add(string);
            }
        }

        if (runInVerboseMode) {
            for (int i = 0; i < commandArray.length; i++) {
                System.out.print(commandArray[i] + " ");
            }
            System.out.println("");
        }
        commandArray = stringList.toArray(new String[stringList.size()]);

        int result = -1;
        try {
            process = Runtime.getRuntime().exec(commandArray);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return false;
        }

        MessageSiphon in = new MessageSiphon(process.getInputStream(), this);
        MessageSiphon err = new MessageSiphon(process.getErrorStream(), this);
        in.setChannel(1);
        err.setChannel(2);
        boolean running = true;
        while (running) {
            try {
                if (in.thread != null)
                    in.thread.join();
                if (err.thread != null)
                    err.thread.join();
                result = process.waitFor();
                running = false;
            } catch (InterruptedException ignored) { }
        }
    
        if (result == 0) {
            return true;
        }
        return false;
    }

    private List<File> compileSketch(String buildPath) {
        return compileFiles(buildPath,
                findFilesInPath(buildPath, "S", false),
                findFilesInPath(buildPath, "c", false),
                findFilesInPath(buildPath, "cpp", false));
    } 

    static private boolean createFolder(File folder) {
        if (folder.isDirectory())
            return false;
        if (!folder.mkdir())
            return false;
        return true;
    }

    private List<File> compileLibraries (Sketch sketch, String buildPath) {
        List<File> objectFiles = new ArrayList<File>();
        List<File> tobjs;

        String origIncs = Base.selectedBoard.get("includes");
        
        for (File libraryFolder : sketch.getImportedLibraries())
        {
            File outputFolder = new File(buildPath, libraryFolder.getName());
            File utilityFolder = new File(libraryFolder, "utility");
            if (!createFolder(outputFolder))
                return null;
            // this library can use includes in its utility/ folder
            Base.selectedBoard.set("includes", origIncs + "::-I" + utilityFolder.getAbsolutePath());
            tobjs = compileFiles(
                    outputFolder.getAbsolutePath(),
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
                    outputFolder.getAbsolutePath(),
                    findFilesInFolder(utilityFolder, "S", false),
                    findFilesInFolder(utilityFolder, "c", false),
                    findFilesInFolder(utilityFolder, "cpp", false));
            // other libraries should not see this library's utility/ folder
            if (tobjs == null) {
                return null;
            }
            objectFiles.addAll(tobjs);
            Base.selectedBoard.set("includes", origIncs);
        }
        return objectFiles;
    }

    private boolean compileCore (String buildPath) {

        List<File> objectFiles = new ArrayList<File>();
        ArrayList<String> includePaths =  new ArrayList();

        String corePath = api.getAbsolutePath();

        includePaths.add(corePath);
        includePaths.add(Base.selectedBoard.getFolder().getAbsolutePath());

        List<File> coreObjectFiles   = compileFiles(
                buildPath,
                findFilesInPath(corePath, "S", true),
                findFilesInPath(corePath, "c", true),
                findFilesInPath(corePath, "cpp", true));

        if (coreObjectFiles == null) {
            return false;
        }

        for (File file : coreObjectFiles) {
            Base.selectedBoard.set("object.name", file.getAbsolutePath());

            if (!Base.selectedBoard.execAsynchronously(Base.selectedBoard.parseString(get("recipe.ar.pattern")))) {
                return false;
            }
        }
        return true;
    }

    private boolean compileLink(String buildPath, List<File> objectFiles) {
        String baseCommandString = get("recipe.c.combine.pattern", "");
        String commandString = "";
        String objectFileList = "";

        for (File file : objectFiles) {
            objectFileList = objectFileList + file.getAbsolutePath() + "::";
        }

        File ldscript = Base.selectedBoard.getLDScript();

        String ldScriptFile;
        String ldScriptPath;
        if (ldscript == null) {
            ldScriptFile = "";
            ldScriptPath = "";
        } else {
            ldScriptFile = ldscript.getName();
            ldScriptPath = ldscript.getParentFile().getAbsolutePath();
        }
        Base.selectedBoard.set("build.path", buildPath);
        Base.selectedBoard.set("object.filelist", objectFileList);

        commandString = Base.selectedBoard.parseString(baseCommandString);
        return Base.selectedBoard.execAsynchronously(commandString);
    }

    private boolean compileEEP(String buildPath) {
        Base.selectedBoard.set("build.path", buildPath);
        return Base.selectedBoard.execAsynchronously(Base.selectedBoard.parseString(get("recipe.objcopy.eep.pattern")));
    }

    private boolean compileHEX(String buildPath) {
        Base.selectedBoard.set("build.path", buildPath);
        return Base.selectedBoard.execAsynchronously(Base.selectedBoard.parseString(get("recipe.objcopy.hex.pattern")));
    }

    static public String[] headerListFromIncludePath(String path) {
        FilenameFilter onlyHFiles = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".h");
            }
        };

        return (new File(path)).list(onlyHFiles);
    }

}
