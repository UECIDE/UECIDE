package uecide.plugin;

import uecide.app.*;
import uecide.app.debug.*;
import uecide.app.editors.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import say.swing.*;
import org.json.simple.*;


public class PluginEntry implements Comparable {
    public String name;
    public Version installedVersion;
    public Version availableVersion;
    public URL url;
    public int type;
    public String mainClass;
    public JSONObject data;
    public File downloadFile = null;
    public File unpackFolder = null;
    public File installFolder = null;
    public int state = 0;

    public static final int UNKNOWN  = 0;
    public static final int PLUGIN   = 1;
    public static final int BOARD    = 2;
    public static final int CORE     = 3;
    public static final int COMPILER = 4;

    public static final int IDLE        = 0;
    public static final int DOWNLOADING = 1;
    public static final int UNPACKING   = 2;
    public static final int INSTALLING  = 3;
    public static final int UNINSTALLING = 4;

    public PluginEntry(JSONObject o, int type) {
        this.data = o;
        this.type = type;
        initData();
    }

    public void initData() {
        try {
            url = new URL((String)data.get("url"));
            availableVersion = new Version((String)data.get("Version"));
            if (availableVersion == null) {
                availableVersion = new Version(null);
            }

            installedVersion = null;

            if (type == PLUGIN) {
                mainClass = (String)data.get("Main-Class");
                name = mainClass.substring(mainClass.lastIndexOf(".")+1);
                installedVersion = Base.getPluginVersion(mainClass);
                installFolder = new File(Base.getUserPluginsFolder(), name + ".jar");
            } 

            if (type == CORE) {
                name = (String)data.get("Core");
                Core c = Base.cores.get(name);
                if (c != null) {    
                    installFolder = c.getFolder();
                    installedVersion = new Version(c.getFullVersion());
                }
            }

            if (type == BOARD) {
                name = (String)data.get("Board");
                Board c = Base.boards.get(name);
                if (c != null) {    
                    installFolder = c.getFolder();
                    installedVersion = new Version(c.getFullVersion());
                }
            }

            if (type == COMPILER) {
                name = (String)data.get("Compiler");
                uecide.app.Compiler c = Base.compilers.get(name);
                if (c != null) {    
                    installFolder = c.getFolder();
                    installedVersion = new Version(c.getFullVersion());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isNewer() {
        if (installedVersion == null) {
            return false;
        }
        if (installedVersion.compareTo(availableVersion) > 0) {
            return true;
        }
        return false;
    }

    public boolean isOutdated() {
        if (installedVersion == null) {
            return false;
        }
        if (installedVersion.compareTo(availableVersion) < 0) {
            return true;
        }
        return false;
    }

    public boolean isInstalled() {
        if (installedVersion == null) {
            return false;
        }
        if (installedVersion.compareTo(availableVersion) == 0) {
            return true;
        }
        return false;
    }

    public String get(String k) {
        return (String)data.get(k);
    }

    public String getAvailableVersion() {
        return availableVersion.toString();
    }

    public String getInstalledVersion() {
        if (installedVersion == null) {
            return "";
        }
        return installedVersion.toString();
    }

    public String toString() {
        return getDisplayName();
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        switch (type) {
            case PLUGIN:
                return name;
            case BOARD:
                return get("Description");
            case CORE:
                return name;
            case COMPILER:
                return name;
        }
        return "---";
    }

    public String getDescription() {
        String d = get("Description");
        if (d == null) {
            return getDisplayName();
        }
        return d;
    }

    public URL getURL() {
        return url;
    }

    public File getDownloadFile() {
        try {
            if (downloadFile == null) {
                String prefix = name;
                while (prefix.length() < 8) {
                    prefix += "_";
                }
                downloadFile = File.createTempFile(prefix, ".jar");
            }
        } catch (Exception e) {
        }
        return downloadFile;
    }

    public File getUnpackFolder() {
        File f = getDownloadFile();
        String n = f.getAbsolutePath();
        n = n.substring(0, n.lastIndexOf("."));
        f = new File(n);
        if (!f.exists()) {
            f.mkdirs();
        }
        return f;
    }

    public int compareTo(Object o) {
        if (o instanceof PluginEntry) {
            PluginEntry pe = (PluginEntry)o;
            String myName = getName();
            String theirName = pe.getName();
            return myName.compareTo(theirName);
        }
        return 0;

    }

    public int getType() {
        return type;
    }

    public File getInstallFolder() {
        return installFolder;
    }

    public boolean isIdle() {
        return state == IDLE;
    }

    public void setState(int i) {
        state = i;
    }

    public int getState() {
        return state;
    }

    public boolean equals(PluginEntry e) {
        if (
            (e.getName().equals(getName())) && 
            (e.getType() == getType())
        ) {
            return true;
        }
        return false;
    }

    public boolean canInstall() {
        if (!isIdle()) {
            return false;
        }
        if (isInstalled()) {
            return false;
        }
        if (isNewer()) {
            return false;
        }
        return true;
    }

    public boolean canUninstall() {
        if (!isIdle()) {
            return false;
        }
        if (isInstalled()) {
            return true;
        }
        if (isOutdated()) {
            return true;
        }
        if (isNewer()) {
            return true;
        }
        return false;
    }

    public boolean canUpgrade() {
        if (!isIdle()) {
            return false;
        }
        if (isOutdated()) {
            return true;
        }
        return false;
    }
}
