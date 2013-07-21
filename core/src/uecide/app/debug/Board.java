package uecide.app.debug;

import java.io.*;
import java.util.*;

import uecide.app.*;
import uecide.plugin.*;

import java.util.regex.*;

import uecide.app.Serial;
import uecide.app.SerialException;
import uecide.app.SerialNotFoundException;


public class Board {
    private String name;
    private String longname;
    private Core core;
    private String group;
    private File folder;
    private boolean valid;
    private boolean runInVerboseMode;
    public PropertyFile boardPreferences;

    public Board(File folder) {
        this.folder = folder;

        File boardFile = new File(folder,"board.txt");
        try {
            valid = false;
            if (boardFile.exists()) {
                boardPreferences = new PropertyFile(boardFile);
            }
            this.name = folder.getName();
            this.longname = (String) boardPreferences.get("name");
            this.core = Base.cores.get(boardPreferences.get("build.core"));
            this.group = (String) boardPreferences.get("group");
            if (this.core != null) {
                valid = true;
            }
        } catch (Exception e) {
            System.err.print("Bad board file format: " + folder);
        }
    }

    public void setVerbose(boolean v) {
        runInVerboseMode = v;
    }

    public String getGroup() {
        return group;
    }

    public File getFolder() {
        return folder;
    }

    public Core getCore() {
        return core;
    }
  
    public String getName() { 
        return name; 
    }

    public String getLongName() {
        return longname;
    }

    public boolean isValid() {
        return valid;
    }

    public String get(String k) {
        return boardPreferences.get(k);

    }

    public void set(String k, String d) {
        boardPreferences.set(k, d);
    }

    public String get(String k, String d) {
        if ((String) boardPreferences.get(k) == null) {
            return d;
        }
        return (String) boardPreferences.get(k);
    }

    public File getLDScript() {
        String fn = get("ldscript", "");
        File found;

        if (fn == null) {
            return null;
        }

        found = new File(folder, fn);
        if (found != null) {
            if (found.exists()) {
                return found;
            }
        }

        found = new File(core.getAPIFolder(), fn);
        if (found != null) {
            if (found.exists()) {
                return found;
            }
        }

        System.err.print("Link script not found: " + fn);

        return null;
    }

    public String getAny(String key) {
        return getAny(key, "");
    }
    public String getAny(String key, String def) {
        return get(key, core.get(key, def));
    }

    public PropertyFile getPreferences() {
        return boardPreferences;
    }
}
