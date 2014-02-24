package uecide.app.debug;

import java.io.*;
import java.util.*;

import uecide.app.*;

public class Compiler {
    public PropertyFile compilerPreferences;
    public File folder;
    public File compilerFile;

    public boolean valid;

    public Compiler(File dir) {
        valid = false;
        folder = dir;
        compilerFile = new File(dir, "compiler.txt");
        if (!compilerFile.exists()) {
            return;
        }

        compilerPreferences = new PropertyFile(compilerFile);

        valid = true;
    }

    public String getName() {
        return compilerPreferences.get("name");
    }

    public PropertyFile getProperties() {
        return compilerPreferences;
    }

    public boolean isValid() {
        return valid;
    }

    public File getFolder() {
        return folder;
    }

    public String getErrorRegex() {
        String r = compilerPreferences.get("compiler.error");
        if (r == null) {
            r = "^([^:]+):(\\d+): error: (.*)";
        }
        return r;
    }

    public String getWarningRegex() {
        String r = compilerPreferences.get("compiler.warning");
        if (r == null) {
            r = "^([^:]+):(\\d+): warning: (.*)";
        }
        return r;
    }

    public String getFamily() {
        return compilerPreferences.get("family");
    }

    public boolean inFamily(String fam) {
        String fly = getFamily();
        if (fly == null) {
            return false;
        }
        String fams[] = fly.split("::");
        for (String thisfam : fams) {
            if (thisfam.equals(fam)) {
                return true;
            }
        }
        return false;
    }

    public String get(String k) {
        if (compilerPreferences == null) {
            System.err.println("No compiler data getting " + k);
            return "";
        }
        return (String) compilerPreferences.get(k);
    }

    public String get(String k, String d) {
        if (get(k) == null) {
            return d;
        }
        return get(k);
    }

    public String getRevision() {
        String v = compilerPreferences.get("revision");
        if (v == null) {
            v = "0";
        }
        return v;
    }

    public String getVersion() {
        String v = compilerPreferences.get("version");
        if (v == null) {
            v = "0";
        }
        return v;
    }

    public String getFullVersion() {
        return getVersion() + "-" + getRevision();
    }


}

