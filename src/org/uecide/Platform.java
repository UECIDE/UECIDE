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

package org.uecide;

import java.io.*;

import javax.swing.UIManager;

import com.sun.jna.Library;
import com.sun.jna.Native;

import java.util.regex.*;


/**
 * Used by Base for platform-specific tweaking, for instance finding the
 * sketchbook location using the Windows registry, or OS X event handling.
 *
 * The methods in this implementation are used by default, and can be
 * overridden by a subclass, if loaded by UECIDE.main().
 *
 * These methods throw vanilla-flavored Exceptions, so that error handling
 * occurs inside UECIDE.
 *
 * There is currently no mechanism for adding new platforms, as the setup is
 * not automated. We could use getProperty("os.arch") perhaps, but that's
 * debatable (could be upper/lowercase, have spaces, etc.. basically we don't
 * know if name is proper Java package syntax.)
 */
public class Platform {
    UECIDE base;

    public void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch(Exception e) {
            UECIDE.error(e);
        }
    }

    public void init(UECIDE base) {
        this.base = base;
        Debug.message("-> Platform Init Start");
        Debug.message("-> Probe Info (Super)");
        probeInfo();
    }


    public File getSettingsFolder() {
        try {
            // otherwise make a .uecide directory int the user's home dir
            File home = new File(System.getProperty("user.home"));
            File dataFolder = new File(home, ".uecide");

            if(!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            return dataFolder;
        } catch(Exception e) {
            UECIDE.error(e);
            return null;
        }
    }


    /**
     * @return null if not overridden, which will cause a prompt to show instead.
     */
    public File getDefaultSketchbookFolder() {
        File docs = new File(System.getProperty("user.home"), "Documents");
        return new File(docs, "UECIDE");
    }

    public void setSettingsFolderEnvironmentVariable() {
        String ev = System.getenv("UECIDE");
        File fn = UECIDE.getDataFolder();
        String fp = fn.getAbsolutePath();

        if(ev == null || !(ev.equals(fp))) {
            File bashrc = new File(System.getProperty("user.home"), ".bashrc");
            File temprc = new File(System.getProperty("user.home"), ".bashrc.tmp");

            if(!bashrc.exists()) {
                bashrc = new File(System.getProperty("user.home"), ".profile");
                temprc = new File(System.getProperty("user.home"), ".profile.tmp");
            }

            if(!bashrc.exists()) {
                Debug.message("Unable to configure environment - neither .bashrc nor .profile seem to exist");
                System.err.println("Unable to configure environment - neither .bashrc nor .profile seem to exist");
                return;
            }

            try {

                boolean haveAdded = false;
                BufferedReader in = new BufferedReader(new FileReader(bashrc));
                BufferedWriter out = new BufferedWriter(new FileWriter(temprc));

                String line;

                while((line = in.readLine()) != null) {
                    if(line.startsWith("export UECIDE=")) {
                        if(!haveAdded) {
                            if(fp.contains(" ")) {
                                out.write("export UECIDE=\"" + fp + "\"\n");
                            } else {
                                out.write("export UECIDE=" + fp + "\n");
                            }

                            haveAdded = true;
                        }
                    } else {
                        out.write(line + "\n");
                    }
                }

                if(!haveAdded) {
                    if(fp.contains(" ")) {
                        out.write("export UECIDE=\"" + fp + "\"\n");
                    } else {
                        out.write("export UECIDE=" + fp + "\n");
                    }

                    haveAdded = true;
                }

                in.close();
                out.close();
                temprc.renameTo(bashrc);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    public PropertyFile platformInfo = new PropertyFile();

    public void probeInfo() {
    }

    public String getVersion() {
        return platformInfo.get("version");
    }

    public String getFlavour() {
        return platformInfo.get("flavour");
    }

}
