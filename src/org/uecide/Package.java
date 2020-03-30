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

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.net.*;

import java.security.MessageDigest;

import org.apache.commons.compress.archivers.ar.*;
import org.apache.commons.compress.archivers.tar.*;
import org.apache.commons.compress.compressors.gzip.*;
import org.apache.commons.compress.compressors.xz.*;
import org.apache.commons.compress.compressors.bzip2.*;

public class Package implements Comparable, Serializable {
    public HashMap<String, String> properties = new HashMap<String, String>();
    public boolean isValid = false;

    public int stateCode = 0;

    public int getState() { return stateCode; }
    public void setState(int c) { 
        stateCode = c; 
    }

    public Package() {
    }

    public Package(String data) {
        parseData(data);
    }

    public void parseData(String data) {
        String[] lines = data.split("\n");
        Pattern p = Pattern.compile("^([^:]+):\\s+(.*)$", Pattern.MULTILINE);

        String currentLine = "";
        for (String line : lines) {
            if (line.startsWith(" ")) {
                currentLine += "\n";
                currentLine += line;
            } else {
                int colon = currentLine.indexOf(":");
                if (colon > 0) {
                    properties.put(currentLine.substring(0, colon), currentLine.substring(colon+2));
                }
                currentLine = line;
            }
        }
        if (!currentLine.equals("")) {
            int colon = currentLine.indexOf(":");
            if (colon > 0) {
                properties.put(currentLine.substring(0, colon), currentLine.substring(colon+2));
            }
        }

        isValid = (getName() != null);
    }
                    

    public Package(String source, String data) {
        parseData(data);

        addRepository(source);

        isValid = (getName() != null);
    }

    public void addRepository(String repo) {
        String repos = properties.get("Repository");
        if (repos == null) {
            repos = repo;
        } else {
            repos = repos + ";" + repo;
        }
        properties.put("Repository", repos);
    }

    public String toString() {
        return properties.get("Package") + " " + properties.get("Version");
    }

    public Version getVersion() {
        return new Version(properties.get("Version"));
    }

    public String getName() {
        return properties.get("Package");
    }

    public String get(String k) {
        return properties.get(k);
    }

    public int compareTo(Object o) {
        if (o instanceof Package) {
            Package op = (Package)o;
            return getDescriptionLineOne().toLowerCase().compareTo(op.getDescriptionLineOne().toLowerCase());
        }
        return 0;
    }

    public String getPriority() {
        return properties.get("Priority");
    }

    public String getRepository() {
        return properties.get("Repository");
    }

    public String getDescription() {
        return properties.get("Description");
    }

    public String getDescriptionLineOne() {
        String description = properties.get("Description");
        if (description == null) {
            return "";
        }

        String lines[] = description.split("\n");
        return lines[0];
    }

    public String getInfo() {
        StringBuilder out = new StringBuilder();
        for (String k : properties.keySet()) {
            out.append(k + ": " + properties.get(k) + "\n");
        }
        return out.toString();
    }

    public String[] getReplaces() {
        String deps = properties.get("Replaces");
        if (deps == null) {
            return null;
        }
        ArrayList<String> out = new ArrayList<String>();
        String[] spl = deps.split(",");
        for (String dep : spl) {
            out.add(dep.trim());
        }
        return out.toArray(new String[0]);
    }

    public String[] getDependencies(boolean incRec) {
        String deps = properties.get("Depends");
        if (incRec) {
            String rec = properties.get("Recommends");
            if (rec != null) {
                if (deps == null) {
                    deps = rec;
                } else {
                    deps += ", " + rec;
                }
            }
        }
        if (deps == null) {
            return null;
        }
        ArrayList<String> out = new ArrayList<String>();
        String[] spl = deps.split(",");
        for (String dep : spl) {
            out.add(dep.trim());
        }
        return out.toArray(new String[0]);
    }

    public String getSection() {
        return properties.get("Section");
    }

    public String getArchitecture() {
        return properties.get("Architecture");
    }

    public String getFilename() {
        return getName() + "_" + getVersion().toString() + "_" + getArchitecture() + ".deb";
    }

    public void shuffleArray(String[] ar) {
        Random rnd = new Random();
        for (int i = ar.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            // Simple swap
            String a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
    }

    public boolean checkFileIntegrity(File cacheFolder) {
        File downloadTo = new File(cacheFolder, getFilename());

        // First off do some lightweight checks. If these fail the SHA check is bound to fail so
        // why bother trugding through that when we can catch a failure quickly with this?
        if (!downloadTo.exists()) {
            return false;
        }

        long size = 0;
        try {
            size = Integer.parseInt(properties.get("Size"));
        } catch (Exception ignored) {
            Base.exception(ignored);
        }

        if (downloadTo.length() != size) {
            return false;
        }

        return true; // What follows is way too slow on a Pi.
/*
        String existingSha = properties.get("SHA256");

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            FileInputStream fis = new FileInputStream(downloadTo);

            byte[] dataBytes = new byte[1024];

            int nread = 0;
            while ((nread = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            };
            byte[] mdbytes = md.digest();

            StringBuilder hexString = new StringBuilder();
            for (int i=0;i<mdbytes.length;i++) {
              hexString.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));

            }

            if (!hexString.toString().equalsIgnoreCase(existingSha)) {
                return false;
            }
        } catch (Exception ignored) {
        }
        return true;
*/
    }

    public boolean fetchPackage(File folder) {
        String errorMessage = "";
        if (!checkFileIntegrity(folder)) {
            File downloadTo = new File(folder, getFilename());
            if (downloadTo.exists()) {
                Base.tryDelete(downloadTo);
            }
            String[] repos = properties.get("Repository").split(";");
            shuffleArray(repos);
            int contentLength = -1;
            for (String repo : repos) {
                try {
                    InputStream in = null;
                    if (repo.startsWith("http://") || repo.startsWith("https://")) {
                        URI uri = new URI(repo + "/" + properties.get("Filename"));
                        URL downloadFrom = uri.toURL();
                        HttpURLConnection httpConn = (HttpURLConnection) downloadFrom.openConnection();
                        contentLength = httpConn.getContentLength();

//                        if (checkFileIntegrity(folder)) {
//                            return true;
//                        }
                        in = httpConn.getInputStream();
                    } else if (repo.startsWith("res://")) {
                        String reps = repo.substring(6);
                        if (!reps.startsWith("/")) {
                            reps = "/" + reps;
                        }
                        in = Base.class.getResourceAsStream(reps + "/" + properties.get("Filename"));
                        if (in == null) {
                            System.err.println("Error: Resource not found: " + reps + "/" + properties.get("Filename"));
                            return false;
                        }
                    } else if (repo.startsWith("file://")) {
                        return false;
                    } else {
                        System.err.println("Error: No URI handler for " + repo);
                        return false;
                    }

                    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(downloadTo));

                    byte[] buffer = new byte[1024];
                    int n;
                    long tot = 0;
                    int lastVal = -1;

                    String tname = getName();
                    if (tname.length() > 20) {
                        tname = tname.substring(0, 20);
                    }
                    while (tname.length() < 20) {
                        tname += " ";
                    }

                    System.out.print("\rDownloading " + tname + " [.........................]");

                    String existingSha = properties.get("SHA256");
                    MessageDigest md = MessageDigest.getInstance("SHA-256");


                    long start = System.currentTimeMillis() / 1000;
                    long ts = start;
                    while ((n = in.read(buffer)) > 0) {
                        long now = System.currentTimeMillis() / 1000;
                        tot += n;
                        if (contentLength != -1) {
                            int tpct = (int)((tot * 100) / contentLength) / 4;
                            if (now != ts) {
                                ts = now;
                                lastVal = tpct;
                                System.out.print("\rDownloading " + tname + " [");
                                for (int i = 0; i < 25; i++) {
                                    if (i <= tpct) {
                                        System.out.print("#");
                                    } else {
                                        System.out.print(".");
                                    }
                                }
                                System.out.print("] ");

                                long diff = now - start;
                                if (diff > 0) {
                                    long bps = tot / diff;
                                    long remain = contentLength - tot;
                                    long trem = remain / bps;
                                    long sec = trem % 60;
                                    long min = (trem / 60) % 60;
                                    long hour = (trem / 3600);
                                    if (bps >= (1024 * 1024)) {
                                        System.out.print(String.format("%7.2f MBps %02d:%02d:%02d", (float)bps / 1048576f, hour, min, sec));
                                    } else if (bps >= 1024) {
                                        System.out.print(String.format("%7.2f kBps %02d:%02d:%02d", (float)bps / 1024f, hour, min, sec));
                                    } else {
                                        System.out.print(String.format("%4d Bps    %02d:%02d:%02d", bps, hour, min, sec));
                                    }
                                }
                            }
                        }
                        md.update(buffer, 0, n);
                        out.write(buffer, 0, n);
                    }
                    in.close();
                    out.close();
                    byte[] mdbytes = md.digest();

                    StringBuilder hexString = new StringBuilder();
                    for (int i=0;i<mdbytes.length;i++) {
                      hexString.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
                    }

                    if (!hexString.toString().equalsIgnoreCase(existingSha)) {
                        System.out.println("\rDownloading " + tname + " [#########################] [31mchecksum error[0m[0K");
                        Base.tryDelete(downloadTo);
                        return false;
                    }

                    System.out.println("\rDownloading " + tname + " [#########################] done[0K");

                    if (checkFileIntegrity(folder)) {
                        return true;
                    }
                } catch (FileNotFoundException ignore) {
                    Base.exception(ignore);
                } catch (Exception e) {
                    Base.exception(e);
                    errorMessage = e.toString();
                    System.err.println();
                    System.err.println("[31mDownload failed: " + errorMessage + "[0m");
                    if (downloadTo.exists()) {
                        Base.tryDelete(downloadTo);
                    }
                }
            }
            return false;
        } else {
            return true;
        }
    }

    // Extract a package and install it. Returns the control file
    // contents as a string.
    public boolean extractPackage(File cache, File db, File root) {
        return doExtractPackage(new File(cache, getFilename()), db, root);
    }

    public boolean doExtractPackage(File src, File db, File root) {

        try {
            System.out.print("Installing " + getName() + " ... ");
            DebFile df = new DebFile(src);
            File pf = new File(db, getName());
            pf.mkdirs();
            df.extract(pf, root);
            System.out.println("done");
        } catch (Exception e) {
            Base.exception(e);
            Base.error(e);
            return false;
        }
        return true;
    }
}
