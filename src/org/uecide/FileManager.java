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

}
