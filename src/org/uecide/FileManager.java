package org.uecide;

import java.io.*;
import java.util.*;
import java.net.*;

public class FileManager {

    public static String loadTextFile(String path) 
            throws IOException, FileNotFoundException, MalformedURLException {
        if (path.startsWith("res://")) {
            return loadTextFileFromResource(path);
        }
        if (path.startsWith("http://")) {
            return loadTextFileFromHTTP(path);
        }
        if (path.startsWith("https://")) {
            return loadTextFileFromHTTPS(path);
        }
        return loadTextFileFromDisk(path);
    }

    public static String loadTextFileFromResource(String path) 
            throws IOException {
        InputStream is = FileManager.class.getResourceAsStream(path.substring(5));
        int bytesRead;
        StringBuilder sb = new StringBuilder();
        String line = null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        while ((line = reader.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        reader.close();
        is.close();
        return sb.toString();
    }

    public static String loadTextFileFromHTTP(String path) 
            throws IOException, MalformedURLException {
        HttpRequest req = new HttpRequest(path);
        return req.getText();
    }

    public static String loadTextFileFromHTTPS(String path)
            throws IOException, MalformedURLException {
        HttpRequest req = new HttpRequest(path);
        return req.getText();
    }

    public static String loadTextFileFromDisk(String path) 
            throws IOException, FileNotFoundException {
        StringBuilder sb = new StringBuilder();
        String line = null;
        BufferedReader reader = new BufferedReader(new FileReader(path));
        while ((line = reader.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public static URL getURLFromPath(String path) 
            throws MalformedURLException {
        if (path.startsWith("res://")) {
            return FileManager.class.getResource(path.substring(5));
        }
        return (new File(path)).toURI().toURL();
    }

    static public ArrayList<File> findFilesInFolder(File sourceFolder, boolean recurse, int... accept) {
        ArrayList<File> out = new ArrayList<File>();
        if (!sourceFolder.isDirectory()) return out;

        File[] files = sourceFolder.listFiles();
        for (File f : files) {
            if (f.getName().startsWith(".")) continue;
            if (f.isDirectory()) {
                if (recurse) {
                    ArrayList<File> sub = findFilesInFolder(f, recurse, accept);
                    if (sub.size() > 0) {
                        out.addAll(sub);
                    }
                }
            } else {
                int ft = FileType.getType(f);
                for (int acc : accept) {
                    if (ft == acc) {
                        out.add(f);
                    }
                }
            }
        }
        return out;
    }

}
