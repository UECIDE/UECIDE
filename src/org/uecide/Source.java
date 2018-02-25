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
import java.util.zip.*;
import java.net.*;
import java.io.*;

public class Source {
    HashMap<String, String> sectionUrls = new HashMap<String, String>();
    String urlRoot;
    String codename;
    String[] sectionList;

    public Source(String root, String dist, String arch, String[] sections) {
        urlRoot = root;
        codename = dist;
        sectionList = sections;
        for (String sec : sections) {
            String url = root;
            if (!url.endsWith("/")) {
                url += "/";
            }
            url += "dists/" + dist + "/" + sec + "/binary-" + arch + "/";
            sectionUrls.put(sec, url);
        }
    }

    String getCompressedFileHTTP(String url) {
        StringBuilder inData = new StringBuilder();
        try {
            byte[] buffer = new byte[1024];
            URI uri = new URI(url);
            HttpURLConnection conn = (HttpURLConnection)(uri.toURL().openConnection());
            int contentLength = conn.getContentLength();
            InputStream rawIn = (InputStream)conn.getInputStream();
            GZIPInputStream in = new GZIPInputStream(rawIn);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int n;
            while ((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
            }
            in.close();
            inData.append(out.toString("UTF-8"));
        } catch (Exception e) {
            Base.error(e);
            System.err.println("Error downloading " + url + ": " + e.getMessage());
        }
        return inData.toString();
    }

    String getCompressedFileRes(String url) {
        if (!url.startsWith("/")) {
            url = "/" + url;
        }
        StringBuilder inData = new StringBuilder();
        try {
            byte[] buffer = new byte[1024];
            InputStream rawIn = Base.class.getResourceAsStream(url);
            GZIPInputStream in = new GZIPInputStream(rawIn);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int n;
            while ((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
            }
            in.close();
            inData.append(out.toString("UTF-8"));
        } catch (Exception e) {
            Base.error(e);
            System.err.println("Error downloading " + url + ": " + e.getMessage());
        }
        return inData.toString();
    }

    String getCompressedFileLocal(String url) {
        return "";
    }

    public Package[] getPackages() {
        return getPackages(false);
    }

    public Package[] getPackages(boolean silent) {
        StringBuilder inData = new StringBuilder();
        HashMap<String, Package> packages = new HashMap<String, Package>();
        for (String url : sectionUrls.values()) {
            if (!silent) {
                System.out.print("Update: " + url + "Packages.gz" + " ... ");
            }
            if (url.startsWith("http://") || url.startsWith("https://")) {
                inData.append(getCompressedFileHTTP(url + "Packages.gz"));
                if (!silent) {
                    System.out.println("done");
                }
            } else if (url.startsWith("res://")) {
                String s = (getCompressedFileRes(url.substring(6) + "Packages.gz"));
                if (s != null) {
                    inData.append(s);
                }
                if (!silent) {
                    System.out.println("done");
                }
            } else if (url.startsWith("file://")) {
                inData.append(getCompressedFileLocal(url.substring(7) + "Packages.gz"));
                if (!silent) {
                    System.out.println("done");
                }
            } else {
                if (!silent) {
                    System.err.println("No URI Handler for URL");
                }
            }
        }

        String[] lines = inData.toString().split("\n");
        StringBuilder onePackage = new StringBuilder();
        for (String line : lines) {
            if (line.equals("")) {
                if (onePackage.toString().length() > 10) {
                    Package thisPackage = new Package(urlRoot, onePackage.toString());
                    if (packages.get(thisPackage.getName()) != null) {
                        Package testPackage = packages.get(thisPackage.getName());
                        if (thisPackage.getVersion().compareTo(testPackage.getVersion()) > 0) {
                            packages.put(thisPackage.getName(), thisPackage);
                        }
                    } else {
                        packages.put(thisPackage.getName(), thisPackage);
                    }
                }
                onePackage = new StringBuilder();
                continue;
            }
            onePackage.append(line + "\n");
        }

        if (onePackage.toString().length() > 10) {
            Package thisPackage = new Package(urlRoot, onePackage.toString());
            if (packages.get(thisPackage.getName()) != null) {
                Package testPackage = packages.get(thisPackage.getName());
                if (thisPackage.getVersion().compareTo(testPackage.getVersion()) > 0) {
                    packages.put(thisPackage.getName(), thisPackage);
                }
            } else {
                packages.put(thisPackage.getName(), thisPackage);
            }
        }

        Package[] list = packages.values().toArray(new Package[0]);
        Arrays.sort(list);
        return list;
    }

    public String toString() {
        String out = urlRoot;
        for (String sec : sectionUrls.keySet()) {
            out += " " + sec;
        }
        return out;
    }            

    public String getRoot() {
        return urlRoot;
    }

    public String getCodename() {
        return codename;
    }

    public String[] getSections() {
        return sectionList;
    }
        
}

