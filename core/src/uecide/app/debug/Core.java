package uecide.app.debug;

import java.io.*;
import java.util.*;

import uecide.app.Preferences;
import uecide.app.Base;
import uecide.app.Sketch;
import uecide.app.SketchFile;
import java.text.MessageFormat;

public class Core implements MessageConsumer {
    private String name;
    private File folder;
    public Map corePreferences;
    private boolean valid;
    private File api;
    private boolean runInVerboseMode;

    public Core(File folder) {
        this.folder = folder;

        File coreFile = new File(folder,"core.txt");

        valid = false;

        try {
            if(coreFile.exists()) {
                corePreferences = new LinkedHashMap();
                Preferences.load(coreFile, corePreferences);
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

    static private boolean createFolder(File folder) {
        if (folder.isDirectory())
            return false;
        if (!folder.mkdir())
            return false;
        return true;
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
