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

package org.uecide.windows;

import java.io.*;
import java.io.UnsupportedEncodingException;
import java.util.*;

import com.sun.jna.Library;
import com.sun.jna.Native;

import javax.swing.*;
import java.util.regex.*;

import org.uecide.Base;
import org.uecide.PropertyFile;
import org.uecide.windows.Registry.REGISTRY_ROOT_KEY;

import javax.swing.UIManager;


// http://developer.apple.com/documentation/QuickTime/Conceptual/QT7Win_Update_Guide/Chapter03/chapter_3_section_1.html
// HKEY_LOCAL_MACHINE\SOFTWARE\Apple Computer, Inc.\QuickTime\QTSysDir

// HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Development Kit\CurrentVersion -> 1.6 (String)
// HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Development Kit\CurrentVersion\1.6\JavaHome -> c:\jdk-1.6.0_05

public class Platform extends org.uecide.Platform {

    static final String openCommand =
        System.getProperty("user.dir").replace('/', '\\') +
        "\\processing.exe \"%1\"";
    static final String DOC = "Processing.Document";
    static final String shellFolders = "Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders";

    public void setLookAndFeel() {
        try {
            // Use the Quaqua L & F on OS X to make JFileChooser less awful
            String laf = Base.theme.get("window.laf.windows");

            if((laf != null) && (laf != "default")) {
                UIManager.setLookAndFeel(laf);
            }
        } catch(Exception e) {
            Base.error(e);
        }
    }

    public void init(Base base) {
        super.init(base);

        checkAssociations();
        checkQuickTime();
        checkPath();
        probeInfo();
    }


    /**
     * Make sure that .pde files are associated with processing.exe.
     */
    protected void checkAssociations() {
        try {
            String knownCommand =
                Registry.getStringValue(REGISTRY_ROOT_KEY.CLASSES_ROOT,
                                        DOC + "\\shell\\open\\command", "");

            if(knownCommand == null) {
                if(Base.preferences.getBoolean("platform.auto_file_type_associations")) {
                    setAssociations();
                }

            } else if(!knownCommand.equals(openCommand)) {
                // If the value is set differently, just change the registry setting.
                if(Base.preferences.getBoolean("platform.auto_file_type_associations")) {
                    setAssociations();
                }
            }
        } catch(Exception e) {
            Base.error(e);
        }
    }


    /**
     * Associate .pde files with this version of Processing.
     */
    protected void setAssociations() {
        try {
            if(Registry.createKey(REGISTRY_ROOT_KEY.CLASSES_ROOT,
                                  "", ".pde") &&
                    Registry.setStringValue(REGISTRY_ROOT_KEY.CLASSES_ROOT,
                                            ".pde", "", DOC) &&

                    Registry.createKey(REGISTRY_ROOT_KEY.CLASSES_ROOT, "", DOC) &&
                    Registry.setStringValue(REGISTRY_ROOT_KEY.CLASSES_ROOT, DOC, "",
                                            "Processing Source Code") &&

                    Registry.createKey(REGISTRY_ROOT_KEY.CLASSES_ROOT,
                                       DOC, "shell") &&
                    Registry.createKey(REGISTRY_ROOT_KEY.CLASSES_ROOT,
                                       DOC + "\\shell", "open") &&
                    Registry.createKey(REGISTRY_ROOT_KEY.CLASSES_ROOT,
                                       DOC + "\\shell\\open", "command") &&
                    Registry.setStringValue(REGISTRY_ROOT_KEY.CLASSES_ROOT,
                                            DOC + "\\shell\\open\\command", "",
                                            openCommand)) {
                // everything ok
                // hooray!

            } else {
                Base.preferences.setBoolean("platform.auto_file_type_associations", false);
            }
        } catch(Exception e) {
            Base.error(e);
        }
    }


    /**
     * Find QuickTime for Java installation.
     */
    protected void checkQuickTime() {
        try {
            String qtsystemPath =
                Registry.getStringValue(REGISTRY_ROOT_KEY.LOCAL_MACHINE,
                                        "Software\\Apple Computer, Inc.\\QuickTime",
                                        "QTSysDir");

            // Could show a warning message here if QT not installed, but that
            // would annoy people who don't want anything to do with QuickTime.
            if(qtsystemPath != null) {
                File qtjavaZip = new File(qtsystemPath, "QTJava.zip");

                if(qtjavaZip.exists()) {
                    String qtjavaZipPath = qtjavaZip.getAbsolutePath();
                    String cp = System.getProperty("java.class.path");
                    System.setProperty("java.class.path",
                                       cp + File.pathSeparator + qtjavaZipPath);
                }
            }
        } catch(Exception e) {
            Base.error(e);
        }
    }

    public void setSettingsFolderEnvironmentVariable() {
        File settingsFolder = Base.getSettingsFolder();
        String variablePath = Registry.getStringValue(REGISTRY_ROOT_KEY.CURRENT_USER, "Environment", "UECIDE");

        if(variablePath == null || !(variablePath.equals(settingsFolder.getAbsolutePath()))) {
            Registry.setStringExpandValue(REGISTRY_ROOT_KEY.CURRENT_USER, "Environment", "UECIDE", settingsFolder.getAbsolutePath());
        }
    }


    /**
     * Remove extra quotes, slashes, and garbage from the Windows PATH.
     */
    protected void checkPath() {
        ArrayList<String> legit = new ArrayList<String>();
        String path = System.getProperty("java.library.path");
        String[] pieces = path.split(File.pathSeparator);

        for(String item : pieces) {
            if(item.startsWith("\"")) {
                item = item.substring(1);
            }

            if(item.endsWith("\"")) {
                item = item.substring(0, item.length() - 1);
            }

            if(item.endsWith(File.separator)) {
                item = item.substring(0, item.length() - File.separator.length());
            }

            File directory = new File(item);

            if(!directory.exists()) {
                continue;
            }

            if(item.trim().length() == 0) {
                continue;
            }

            legit.add(item);
        }

        StringBuilder newPath = new StringBuilder();

        for(String s : legit) {
            newPath.append(s);
            newPath.append(File.separator);
        }

        String calcPath = newPath.toString();

        if(calcPath.endsWith(File.separator)) {
            calcPath = calcPath.substring(0, calcPath.length() - 1 - File.separator.length());
        }

        if(!calcPath.equals(path)) {
            System.setProperty("java.library.path", calcPath);
        }
    }


    // looking for Documents and Settings/blah/Application Data/Processing
    public File getSettingsFolder() {

        try {
            String localAppDataPath = Registry.getStringValue(REGISTRY_ROOT_KEY.CURRENT_USER, shellFolders, "Local AppData");
            String roamingAppDataPath = Registry.getStringValue(REGISTRY_ROOT_KEY.CURRENT_USER, shellFolders, "AppData");

            File localDataFolder = new File(localAppDataPath, Base.theme.get("product.cap"));
            File roamingDataFolder = new File(roamingAppDataPath, Base.theme.get("product.cap"));

            // We don't want old installations to suddenly lose all their data, so stick with the roaming if it
            // already exists.  A user can delete it or move it if they want.
            if(roamingDataFolder.exists() && roamingDataFolder.isDirectory()) {
                return roamingDataFolder;
            }

            return localDataFolder;
        } catch(Exception e) {
            Base.error(e);
            return null;
        }
    }


    // looking for Documents and Settings/blah/My Documents/Processing
    // (though using a reg key since it's different on other platforms)
    public File getDefaultSketchbookFolder() {
        try {
            String personalPath = Registry.getStringValue(REGISTRY_ROOT_KEY.CURRENT_USER, shellFolders, "Personal");

            return new File(personalPath, Base.theme.get("product.cap"));
        } catch(Exception e) {
            Base.error(e);
            return null;
        }
    }


    public void openURL(String url) {
        try {
            if(url.startsWith("http://") || url.startsWith("https://") || url.startsWith("ftp://")) {
                Runtime.getRuntime().exec("cmd /c start " + url);
            } else {
                Runtime.getRuntime().exec("explorer \"" + url + "\""); //if not a URL, open with explorer.exe - I have test built this and it cures the problem in Windows.
            }
        } catch(Exception e) {
            Base.error(e);
        }
    }


    public boolean openFolderAvailable() {
        return true;
    }


    public void openFolder(File file) {
        try {
            String folder = file.getAbsolutePath(); //There seems to be a weird discrepency here, in the older code when this function was called, loadedSketch.getFolder().getAbsolutePath() was passed here - A 'String' is not a 'File' which is presumably why it failed to work.

            Runtime.getRuntime().exec("explorer \"" + folder + "\"");
        } catch(Exception e) {
            Base.error(e);
        }
    }


    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


    // Code partially thanks to Richard Quirk from:
    // http://quirkygba.blogspot.com/2009/11/setting-environment-variables-in-java.html

    static WinLibC clib = (WinLibC) Native.loadLibrary("msvcrt", WinLibC.class);

    public interface WinLibC extends Library {
        //WinLibC INSTANCE = (WinLibC) Native.loadLibrary("msvcrt", WinLibC.class);
        //libc = Native.loadLibrary("msvcrt", WinLibC.class);
        public int _putenv(String name);
    }


    public void setenv(String variable, String value) {
        //WinLibC clib = WinLibC.INSTANCE;
        clib._putenv(variable + "=" + value);
    }


    public String getenv(String variable) {
        return System.getenv(variable);
    }


    public int unsetenv(String variable) {
        //WinLibC clib = WinLibC.INSTANCE;
        //clib._putenv(variable + "=");
        //return 0;
        return clib._putenv(variable + "=");
    }

    public PropertyFile platformInfo = new PropertyFile();

    public void probeInfo() {
        Runtime rt; 
        Process pr; 
        BufferedReader in;
        String line = "";
        String sysInfo = "";
        String edition = "";
        String version = "";
        final String   SEARCH_TERM = "OS Name:";
        final String[] EDITIONS = { "Basic", "Home", "Professional", "Enterprise" };

        try {
            rt = Runtime.getRuntime();
            pr = rt.exec("SYSTEMINFO");
            in = new BufferedReader(new InputStreamReader(pr.getInputStream()));

            Pattern pat = Pattern.compile("Microsoft Windows ([^\\s]+) ([^\\s]+)");

            //add all the lines into a variable
            while((line=in.readLine()) != null) {
                Matcher mat = pat.matcher(line);
                if (mat.find()) {
                    version = mat.group(1).toLowerCase().trim();
                    edition = mat.group(2).toLowerCase().trim();
                } 
            }

            platformInfo.set("version", version);
            platformInfo.set("flavour", edition);

        } catch (IOException ioe) {   
            System.err.println(ioe.getMessage());
        }
    }

    public String getVersion() {
        return platformInfo.get("version");
    }

    public String getFlavour() {
        return platformInfo.get("flavour");
    }

}


