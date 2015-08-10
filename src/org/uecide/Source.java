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
            System.err.println("Error downloading " + url + ": " + e.getMessage());
        }
        return inData.toString();
    }

    String getCompressedFileLocal(String url) {
        return "";
    }

    public Package[] getPackages() {
        StringBuilder inData = new StringBuilder();
        HashMap<String, Package> packages = new HashMap<String, Package>();
        for (String url : sectionUrls.values()) {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                inData.append(getCompressedFileHTTP(url + "Packages.gz"));
            } else if (url.startsWith("res://")) {
                inData.append(getCompressedFileRes(url.substring(6) + "Packages.gz"));
            } else if (url.startsWith("file://")) {
                inData.append(getCompressedFileLocal(url.substring(7) + "Packages.gz"));
            } else {
                System.err.println("No URI Handler for URL");
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

