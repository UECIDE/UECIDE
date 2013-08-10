package uecide.app.debug;

import java.io.*;
import java.util.*;

import uecide.app.*;
import uecide.plugin.*;

import java.util.regex.*;

import uecide.app.Serial;
import uecide.app.SerialException;
import uecide.app.SerialNotFoundException;


public class Board implements Comparable {
    private String name;
    private String longname;
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
            this.group = (String) boardPreferences.get("group");
            valid = true;
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

    public PropertyFile getPreferences() {
        return boardPreferences;
    }

    public String getFamily() {
        return boardPreferences.get("family");
    }

    public PropertyFile getProperties() {
        return boardPreferences;
    }

    public String getVersion() {
        String v = boardPreferences.get("version");
        if (v == null) {
            v = "0";
        }
        return v;
    }

    public int compareTo(Object o) {
        if (o == null) {
            return 0;
        }
        if (o instanceof Board) {
            Board b = (Board)o;
            String foreignName = b.getLongName();
            if (longname != null && foreignName != null) {
                return longname.compareTo(foreignName);
            }
        }
        if (o instanceof String) {
            if (name != null) {
                return name.compareTo((String)o);
            }
        }
        return 0;
    }
}
