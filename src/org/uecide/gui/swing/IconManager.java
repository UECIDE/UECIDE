package org.uecide.gui.swing;

import org.uecide.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.net.*;
import javax.imageio.*;
import java.util.Timer;

public class IconManager {
    static String iconFamily = "gnomic";
    static Timer tickTimer = null;

    static HashMap<String, CleverIcon> iconCache = new HashMap<String, CleverIcon>();

    static HashMap<String, HashMap<String, String>> iconSets = new HashMap<String, HashMap<String, String>>();

    public static void setIconFamily(String name) throws IOException {
        iconFamily = name;
        updateIconCache();
    }

    public static void updateIconCache() throws IOException {
        synchronized (iconCache) {
            for (CleverIcon i : iconCache.values()) {
                i.updateIcon();
            }
        }
    }

    public static CleverIcon getIcon(int size, String... name) throws IOException, MalformedURLException {

        synchronized (iconCache) {
            String cacheName = String.join(",", name);

            if (iconCache.get(cacheName + "." + size) != null) {
                return iconCache.get(cacheName + "." + size);
            }
            CleverIcon i = new CleverIcon(size, name);
            iconCache.put(cacheName + "." + size, i);
            return i;
        }
    }

    public static void loadIconSets() throws IOException {
        iconSets = new HashMap<String, HashMap<String, String>>();
        HashMap<String, String> internalList = loadIconList("res://org/uecide/icons/internal.txt");
        for (String k : internalList.keySet()) {
            String p = internalList.get(k);
            HashMap<String, String> pf = loadIconList(p + "/icons.txt");
            pf.put("path", p);
            iconSets.put(pf.get("name"), pf);
        }

        ArrayList<File> externals = FileCache.getFilesByName("icons.txt");
        for (File f : externals) {
            HashMap<String, String> pf = loadIconList(f.getAbsolutePath());
            pf.put("path", f.getParentFile().getAbsolutePath());
            iconSets.put(pf.get("name"), pf);
        }

        initAnimation();
    }

    public static String getName(String set) {
        HashMap<String, String> pf = iconSets.get(set);
        if (pf == null) {
            return "Unknown";
        }
        return pf.get("name");
    }

    static HashMap<String, String> loadIconList(String path) throws IOException {
        HashMap<String, String> out = new HashMap<String, String>();
        String data = FileManager.loadTextFile(path);
        String[] lines = data.split("\n");
        for (String line : lines) {
            String[] parts = line.split("=");
            if (parts.length == 2) {
                out.put(parts[0], parts[1]);
            }
        }
        return out;
    }

    static public URL[] getIconPaths(String name) throws IOException {
        if (name.startsWith("internal:")) {
            String[] bits = name.split(":");
            URL[] paths = new URL[1];
            paths[0] = FileManager.getURLFromPath("res://org/uecide/icons/internal/" + bits[1] + ".png");
            System.err.println(paths[0]);
            return paths;
        }

        HashMap<String, String> iconData = iconSets.get(iconFamily);
        if (iconData == null) {
            iconData = iconSets.get("gnomic");
        }
        if (iconData == null) {
            throw new IOException("No icon family could be loaded!");
        }

        String filename = iconData.get("icon." + name);
        if (filename == null) {
            iconData = iconSets.get("gnomic");
            filename = iconData.get("icon." + name);
            if (filename == null) {
                URL[] out = new URL[1];
                out[0] = FileManager.getURLFromPath("res://org/uecide/icons/unknown.png");
                return out;
            }
        }

        String[] filenames = filename.split("::");
        ArrayList<URL> out = new ArrayList<URL>();

        for (String fn : filenames) {

            URL u = FileManager.getURLFromPath(getIconRoot() + "/" + fn);

            try {
                Object valid = u.getContent();
                if (valid == null) {
                    u = null;
                }
            } catch (Exception ex) {
                u = null;
            }

            if (u == null) {
                iconData = iconSets.get("gnomic");
                u = FileManager.getURLFromPath(iconData.get("path") + "/" + fn);
                if (u == null) {
                    u = FileManager.getURLFromPath("res://org/uecide/icons/unknown.png");
                }
            }
            out.add(u);
        }

        return out.toArray(new URL[0]);
    }

    static public String getIconRoot() {
        HashMap<String, String> iconData = iconSets.get(iconFamily);
        if (iconData == null) {
            iconData = iconSets.get("gnomic");
        }
        return iconData.get("path");
    }

    static public HashMap<String, String> getIconList() {
        HashMap<String, String> out = new HashMap<String, String>();

        for (String k : iconSets.keySet()) {
            HashMap<String, String> iconData = iconSets.get(k);
            out.put(k, iconData.get("description"));
        }
        return out;
    }

    static void initAnimation() {
        if (tickTimer != null) return;
        tickTimer = new Timer();
        tickTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                IconManager.tick();
            }
        }, 50, 50);
    }

    static public void tick() {
        synchronized (iconCache) {
            for (CleverIcon i : iconCache.values()) {
                i.animate();
            }
        }
    }

}
