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

package org.uecide.macosx;

import java.awt.Insets;
import java.io.*;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.regex.*;

import javax.swing.UIManager;

import com.apple.eio.FileManager;

import org.uecide.Base;
import org.uecide.PropertyFile;

/**
 * Platform handler for Mac OS X.
 */
public class Platform extends org.uecide.Platform {

    public void setLookAndFeel() {
        try {
            // Use the Quaqua L & F on OS X to make JFileChooser less awful
            String laf = Base.theme.get("window.laf.macosx");

            if((laf != null) && (laf != "default")) {
                UIManager.setLookAndFeel(laf);
            } else {
                UIManager.setLookAndFeel("ch.randelshofer.quaqua.QuaquaLookAndFeel");
            }

            // undo quaqua trying to fix the margins, since we've already
            // hacked that in, bit by bit, over the years
            UIManager.put("Component.visualMargin", new Insets(1, 1, 1, 1));
        } catch(Exception e) {
            Base.error(e);
        }
    }


    public void init(Base base) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        ThinkDifferent.init(base);
        probeInfo();
    }


    public File getSettingsFolder() {
        return new File(getLibraryFolder(), Base.theme.get("product"));
    }


    public File getDefaultSketchbookFolder() {
        return new File(getDocumentsFolder(), Base.theme.get("product"));
    }


    public void openURL(String url) {
        try {
            Float javaVersion = new Float(System.getProperty("java.version").substring(0, 3)).floatValue();

            if(javaVersion < 1.6f) {
                if(url.startsWith("http://")) {
                    // formerly com.apple.eio.FileManager.openURL(url);
                    // but due to deprecation, instead loading dynamically
                    try {
                        Class<?> eieio = Class.forName("com.apple.eio.FileManager");
                        Method openMethod =
                            eieio.getMethod("openURL", new Class[] { String.class });
                        openMethod.invoke(null, new Object[] { url });
                    } catch(Exception e) {
                        Base.error(e);
                    }
                } else {
                    // Assume this is a file instead, and just open it.
                    // Extension of http://dev.processing.org/bugs/show_bug.cgi?id=1010
                    Base.open(url);
                }
            } else {
                try {
                    Class<?> desktopClass = Class.forName("java.awt.Desktop");
                    Method getMethod = desktopClass.getMethod("getDesktop");
                    Object desktop = getMethod.invoke(null, new Object[] { });

                    // for Java 1.6, replacing with java.awt.Desktop.browse()
                    // and java.awt.Desktop.open()
                    if(url.startsWith("http://")) {   // browse to a location
                        Method browseMethod =
                            desktopClass.getMethod("browse", new Class[] { URI.class });
                        browseMethod.invoke(desktop, new Object[] { new URI(url) });
                    } else {  // open a file
                        Method openMethod =
                            desktopClass.getMethod("open", new Class[] { File.class });
                        openMethod.invoke(desktop, new Object[] { new File(url) });
                    }
                } catch(Exception e) {
                    Base.error(e);
                }
            }
        } catch(Exception ex) {
            Base.error(ex);
        }
    }


    public boolean openFolderAvailable() {
        return true;
    }


    public void openFolder(File file) {
        Base.open(file.getAbsolutePath());
    }


    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


    // Some of these are supposedly constants in com.apple.eio.FileManager,
    // however they don't seem to link properly from Eclipse.

    static final int kDocumentsFolderType =
        ('d' << 24) | ('o' << 16) | ('c' << 8) | 's';
    //static final int kPreferencesFolderType =
    //  ('p' << 24) | ('r' << 16) | ('e' << 8) | 'f';
    static final int kDomainLibraryFolderType =
        ('d' << 24) | ('l' << 16) | ('i' << 8) | 'b';
    static final short kUserDomain = -32763;


    // apple java extensions documentation
    // http://developer.apple.com/documentation/Java/Reference/1.5.0
    //   /appledoc/api/com/apple/eio/FileManager.html

    // carbon folder constants
    // http://developer.apple.com/documentation/Carbon/Reference
    //   /Folder_Manager/folder_manager_ref/constant_6.html#/
    //   /apple_ref/doc/uid/TP30000238/C006889

    // additional information found int the local file:
    // /System/Library/Frameworks/CoreServices.framework
    //   /Versions/Current/Frameworks/CarbonCore.framework/Headers/


    protected String getLibraryFolder() {
        try {
            return FileManager.findFolder(kUserDomain, kDomainLibraryFolderType);
        } catch(Exception e) {
            Base.error(e);
            return null;
        }
    }


    protected String getDocumentsFolder() {
        try {
            return FileManager.findFolder(kUserDomain, kDocumentsFolderType);
        } catch(Exception e) {
            Base.error(e);
            return null;
        }
    }


    public PropertyFile platformInfo = new PropertyFile();

    public void probeInfo() {
        File f = new File("/System/Library/CoreServices/SystemVersion.plist");
        if (f.exists()) {

            Pattern key = Pattern.compile("<key>(.*)</key>");
            Pattern value = Pattern.compile("<string>(.*)</string>");

            PropertyFile prop = new PropertyFile();
            try {
                BufferedReader in = new BufferedReader(new FileReader(f));
                String line;

                String thisKey = "";

                while((line = in.readLine()) != null) {
                    Matcher m = key.matcher(line);
                    if (m.find()) {
                        thisKey = m.group(1);
                        continue;
                    }

                    m = value.matcher(line);
                    if (m.find()) {
                        prop.set(thisKey, m.group(1));
                        continue;
                    }
                }
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            platformInfo.set("flavour", "osx");
            platformInfo.set("version", "unknown");

            String version = prop.get("ProductVersion");

            if (version.startsWith("10.0.")) {
                platformInfo.set("version", "cheetah");
            }

            if (version.startsWith("10.1.")) {
                platformInfo.set("version", "puma");
            }
           
            if (version.startsWith("10.2.")) {
                platformInfo.set("version", "jaguar");
            }
           
            if (version.startsWith("10.3.")) {
                platformInfo.set("version", "panther");
            }
           
            if (version.startsWith("10.4.")) {
                platformInfo.set("version", "tiger");
            }
           
            if (version.startsWith("10.5.")) {
                platformInfo.set("version", "leopard");
            }
           
            if (version.startsWith("10.6.")) {
                platformInfo.set("version", "snowleopard");
            }
           
            if (version.startsWith("10.7.")) {
                platformInfo.set("version", "lion");
            }
           
            if (version.startsWith("10.8.")) {
                platformInfo.set("version", "mountainlion");
            }

            if (version.startsWith("10.9.")) {
                platformInfo.set("version", "mavericks");
            }
           
            if (version.startsWith("10.10.")) {
                platformInfo.set("version", "yosemite");
            }
           
        }
    }

    public String getVersion() {
        return platformInfo.get("version");
    }

    public String getFlavour() {
        return platformInfo.get("flavour");
    }


}
