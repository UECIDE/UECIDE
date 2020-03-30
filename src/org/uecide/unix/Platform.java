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

package org.uecide.unix;
import org.uecide.*;

import java.io.File;

import javax.swing.UIManager;
import java.awt.*;
import java.util.regex.*;


/**
 * Used by Base for platform-specific tweaking, for instance finding the
 * sketchbook location using the Windows registry, or OS X event handling.
 */
public class Platform extends org.uecide.Platform {

    // TODO Need to be smarter here since KDE people ain't gonna like that GTK.
    public void setLookAndFeel() {
        try {
            Toolkit xToolkit = Toolkit.getDefaultToolkit();
            java.lang.reflect.Field awtAppClassNameField =
                xToolkit.getClass().getDeclaredField("awtAppClassName");
            awtAppClassNameField.setAccessible(true);
            awtAppClassNameField.set(xToolkit, "UECIDE");
        } catch(Exception e) {
            Base.exception(e);
            Base.error(e);
        }
    }

    public void init() {
        probeInfo();
    }

    public void probeInfo() {
        File f = new File("/etc/os-release");
        if (f.exists()) {
            PropertyFile p = new PropertyFile(f);
            String flav = p.get("ID");
            if (flav == null) {
                return;
            }
            platformInfo.set("flavour", flav);

            if (flav.equals("debian")) {
                String v = p.get("VERSION");
                if (v == null) {
                    v = "unknown";
                }
                Pattern pat = Pattern.compile("\\d+\\s+\\((.*)\\)");
                Matcher m = pat.matcher(v);
                if (m.find()) {
                    platformInfo.set("version", m.group(1).toLowerCase());
                }
            } else if (flav.equals("ubuntu")) {
                String v = p.get("VERSION");
                if (v == null) {
                    v = "unknown";
                }
                Pattern pat = Pattern.compile(",\\s+([^\\s]+)\\s+.+");
                Matcher m = pat.matcher(v);
                if (m.find()) {
                    platformInfo.set("version", m.group(1).toLowerCase());
                }
            }
        }
    }


    public boolean openFolderAvailable() {
        if(Base.session.get("launcher") != null) {
            return true;
        }

        // Attempt to use xdg-open
        try {
            Process p = Runtime.getRuntime().exec(new String[] { "xdg-open" });
            p.waitFor();
            Base.session.set("launcher", "xdg-open");
            return true;
        } catch(Exception e) { Base.exception(e); }

        // Attempt to use gnome-open
        try {
            Process p = Runtime.getRuntime().exec(new String[] { "gnome-open" });
            p.waitFor();
            Base.session.set("launcher", "gnome-open");
            return true;
        } catch(Exception e) { Base.exception(e); }

        // Attempt with kde-open
        try {
            Process p = Runtime.getRuntime().exec(new String[] { "kde-open" });
            p.waitFor();
            Base.session.set("launcher", "kde-open");
            return true;
        } catch(Exception e) { Base.exception(e); }

        return false;
    }


    public void openFolder(File file) {
        try {
            if(openFolderAvailable()) {
                String lunch = Base.session.get("launcher");

                try {
                    String[] params = new String[] { lunch, file.getAbsolutePath() };
                    //processing.core.PApplet.println(params);
                    /*Process p =*/ Runtime.getRuntime().exec(params);
                    /*int result =*/ //p.waitFor();
                } catch(Exception e) {
                    Base.exception(e);
                    Base.error(e);
                }
            } else {
                System.out.println("No launcher set, cannot open " +
                                   file.getAbsolutePath());
            }
        } catch(Exception ex) {
            Base.exception(ex);
            Base.error(ex);
        }
    }
}
