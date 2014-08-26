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

package org.uecide.unix;
import org.uecide.Base;

import java.io.File;

import javax.swing.UIManager;
import java.awt.*;


/**
 * Used by Base for platform-specific tweaking, for instance finding the
 * sketchbook location using the Windows registry, or OS X event handling.
 */
public class Platform extends org.uecide.Platform {

    // TODO Need to be smarter here since KDE people ain't gonna like that GTK.
    public void setLookAndFeel() {
        try {
            String laf = Base.theme.get("window.laf");

            if((laf != null) && (laf != "default")) {
                UIManager.setLookAndFeel(laf);
            }

            Toolkit xToolkit = Toolkit.getDefaultToolkit();
            java.lang.reflect.Field awtAppClassNameField =
                xToolkit.getClass().getDeclaredField("awtAppClassName");
            awtAppClassNameField.setAccessible(true);
            awtAppClassNameField.set(xToolkit, Base.theme.get("product.cap"));
        } catch(Exception e) {
            Base.error(e);
        }
    }


    public void openURL(String url) {
        try {
            if(openFolderAvailable()) {
                String launcher = Base.preferences.get("launcher");

                if(launcher != null) {
                    Runtime.getRuntime().exec(new String[] { launcher, url });
                }
            }
        } catch(Exception e) {
            Base.error(e);
        }
    }


    public boolean openFolderAvailable() {
        if(Base.preferences.get("launcher") != null) {
            return true;
        }

        // Attempt to use xdg-open
        try {
            Process p = Runtime.getRuntime().exec(new String[] { "xdg-open" });
            p.waitFor();
            Base.preferences.set("launcher", "xdg-open");
            return true;
        } catch(Exception e) { }

        // Attempt to use gnome-open
        try {
            Process p = Runtime.getRuntime().exec(new String[] { "gnome-open" });
            p.waitFor();
            Base.preferences.set("launcher", "gnome-open");
            return true;
        } catch(Exception e) { }

        // Attempt with kde-open
        try {
            Process p = Runtime.getRuntime().exec(new String[] { "kde-open" });
            p.waitFor();
            Base.preferences.set("launcher", "kde-open");
            return true;
        } catch(Exception e) { }

        return false;
    }


    public void openFolder(File file) {
        try {
            if(openFolderAvailable()) {
                String lunch = Base.preferences.get("launcher");

                try {
                    String[] params = new String[] { lunch, file.getAbsolutePath() };
                    //processing.core.PApplet.println(params);
                    /*Process p =*/ Runtime.getRuntime().exec(params);
                    /*int result =*/ //p.waitFor();
                } catch(Exception e) {
                    Base.error(e);
                }
            } else {
                System.out.println("No launcher set, cannot open " +
                                   file.getAbsolutePath());
            }
        } catch(Exception ex) {
            Base.error(ex);
        }
    }
}
