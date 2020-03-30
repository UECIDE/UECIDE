/*
 * Copyright (c) 2015, Majenko Technologies
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

import org.uecide.UECIDE;
import org.uecide.Debug;
import org.uecide.Preferences;
import org.uecide.PropertyFile;
import org.uecide.windows.Registry.REGISTRY_ROOT_KEY;

import javax.swing.UIManager;


// http://developer.apple.com/documentation/QuickTime/Conceptual/QT7Win_Update_Guide/Chapter03/chapter_3_section_1.html
// HKEY_LOCAL_MACHINE\SOFTWARE\Apple Computer, Inc.\QuickTime\QTSysDir

// HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Development Kit\CurrentVersion -> 1.6 (String)
// HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Development Kit\CurrentVersion\1.6\JavaHome -> c:\jdk-1.6.0_05

public class Platform extends org.uecide.Platform {

    static final String shellFolders = "Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders";

    public void setLookAndFeel() {
    }

    public void setSettingsFolderEnvironmentVariable() {
        File settingsFolder = UECIDE.getDataFolder();
        String variablePath = Registry.getStringValue(REGISTRY_ROOT_KEY.CURRENT_USER, "Environment", "UECIDE");

        if(variablePath == null || !(variablePath.equals(settingsFolder.getAbsolutePath()))) {
            Registry.setStringExpandValue(REGISTRY_ROOT_KEY.CURRENT_USER, "Environment", "UECIDE", settingsFolder.getAbsolutePath());
        }
    }


    // looking for Documents and Settings/blah/Application Data/Processing
    public File getSettingsFolder() {

        try {
            String localAppDataPath = Registry.getStringValue(REGISTRY_ROOT_KEY.CURRENT_USER, shellFolders, "Local AppData");
            String roamingAppDataPath = Registry.getStringValue(REGISTRY_ROOT_KEY.CURRENT_USER, shellFolders, "AppData");

            File localDataFolder = new File(localAppDataPath, "UECIDE");
            File roamingDataFolder = new File(roamingAppDataPath, "UECIDE");

            // We don't want old installations to suddenly lose all their data, so stick with the roaming if it
            // already exists.  A user can delete it or move it if they want.
            if(roamingDataFolder.exists() && roamingDataFolder.isDirectory()) {
                return roamingDataFolder;
            }

            return localDataFolder;
        } catch(Exception e) {
            Debug.exception(e);
            UECIDE.error(e);
            return null;
        }
    }


    // looking for Documents and Settings/blah/My Documents/Processing
    // (though using a reg key since it's different on other platforms)
    public File getDefaultSketchbookFolder() {
        try {
            String personalPath = Registry.getStringValue(REGISTRY_ROOT_KEY.CURRENT_USER, shellFolders, "Personal");

            return new File(personalPath, "UECIDE");
        } catch(Exception e) {
            Debug.exception(e);
            UECIDE.error(e);
            return null;
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

    public String getVersion() {
        return platformInfo.get("version");
    }

    public String getFlavour() {
        return platformInfo.get("flavour");
    }

}


