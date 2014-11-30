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
 * overridden by a subclass, if loaded by Base.main().
 *
 * These methods throw vanilla-flavored Exceptions, so that error handling
 * occurs inside Base.
 *
 * There is currently no mechanism for adding new platforms, as the setup is
 * not automated. We could use getProperty("os.arch") perhaps, but that's
 * debatable (could be upper/lowercase, have spaces, etc.. basically we don't
 * know if name is proper Java package syntax.)
 */
public class Platform {
    Base base;


    /**
     * Set the default L & F. While I enjoy the bounty of the sixteen possible
     * exception types that this UIManager method might throw, I feel that in
     * just this one particular case, I'm being spoiled by those engineers
     * at Sun, those Masters of the Abstractionverse. It leaves me feeling sad
     * and overweight. So instead, I'll pretend that I'm not offered eleven dozen
     * ways to report to the user exactly what went wrong, and I'll bundle them
     * all into a single catch-all "Exception". Because in the end, all I really
     * care about is whether things worked or not. And even then, I don't care.
     *
     * @throws Exception Just like I said.
     */
    public void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch(Exception e) {
            Base.error(e);
        }
    }


    public void init(Base base) {
        this.base = base;
        probeInfo();
    }


    public File getSettingsFolder() throws Exception {
        try {
            // otherwise make a .uecide directory int the user's home dir
            File home = new File(System.getProperty("user.home"));
            File dataFolder = new File(home, "." + Base.theme.get("product"));

            if(!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            return dataFolder;
        } catch(Exception e) {
            Base.error(e);
            return null;
        }
    }


    /**
     * @return null if not overridden, which will cause a prompt to show instead.
     */
    public File getDefaultSketchbookFolder() {
        File docs = new File(System.getProperty("user.home"), "Documents");
        return new File(docs, Base.theme.get("product.cap"));
    }

    public void setSettingsFolderEnvironmentVariable() {
        String ev = System.getenv("UECIDE");
        File fn = Base.getSettingsFolder();
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


    public void openURL(String url) {
        try {
            String launcher = Base.preferences.get("launcher");

            if(launcher != null) {
                Runtime.getRuntime().exec(new String[] { launcher, url });
            } else {
                showLauncherWarning();
            }
        } catch(Exception e) {
            Base.error(e);
        }
    }


    public boolean openFolderAvailable() {
        return Base.preferences.get("launcher") != null;
    }


    public void openFolder(File file) {
        try {
            String launcher = Base.preferences.get("launcher");

            if(launcher != null) {
                String folder = file.getAbsolutePath();
                Runtime.getRuntime().exec(new String[] { launcher, folder });
            } else {
                showLauncherWarning();
            }
        } catch(Exception e) {
            Base.error(e);
        }
    }


    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


    public interface CLibrary extends Library {
        CLibrary INSTANCE = (CLibrary)Native.loadLibrary("c", CLibrary.class);
        int setenv(String name, String value, int overwrite);
        String getenv(String name);
        int unsetenv(String name);
        int putenv(String string);
    }


    public void setenv(String variable, String value) {
        CLibrary clib = CLibrary.INSTANCE;
        clib.setenv(variable, value, 1);
    }


    public String getenv(String variable) {
        CLibrary clib = CLibrary.INSTANCE;
        return clib.getenv(variable);
    }


    public int unsetenv(String variable) {
        CLibrary clib = CLibrary.INSTANCE;
        return clib.unsetenv(variable);
    }


    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


    protected void showLauncherWarning() {
        Base.showWarning("No launcher available",
                         "Unspecified platform, no launcher available.\n" +
                         "To enable opening URLs or folders, add a \n" +
                         "\"launcher=/path/to/app\" line to preferences.txt",
                         null);
    }

    public PropertyFile platformInfo = new PropertyFile();

    public void probeInfo() {
        File f = new File("/etc/os-release");
        if (f.exists()) {
            PropertyFile p = new PropertyFile(f);
            String flav = p.get("ID");
            if (flav != null) {
                platformInfo.set("flavour", flav);
            }

            if (flav.equals("debian")) {
                String v = p.get("VERSION");
                Pattern pat = Pattern.compile("\\d+\\s+\\((.*)\\)");
                Matcher m = pat.matcher(v);
                if (m.find()) {
                    platformInfo.set("version", m.group(1).toLowerCase());
                }
            } else if (flav.equals("ubuntu")) {
                String v = p.get("VERSION");
                Pattern pat = Pattern.compile(",\\s+([^\\s]+)\\s+.+");
                Matcher m = pat.matcher(v);
                if (m.find()) {
                    platformInfo.set("version", m.group(1).toLowerCase());
                }
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
