package org.uecide;

import java.io.File;

import java.util.HashMap;
import java.util.ArrayList;

import java.awt.Font;
import java.awt.Color;
import java.awt.GraphicsEnvironment;

public class Preferences {

    public static PropertyFile preferences = new PropertyFile();
    public static PropertyFile preferencesTree = new PropertyFile();


    static public String fontToString(Font f) {
        String font = f.getName();
        String style = "";
        font += ",";

        if((f.getStyle() & Font.BOLD) != 0) {
            style += "bold";
        }

        if((f.getStyle() & Font.ITALIC) != 0) {
            style += "italic";
        }

        if(style.equals("")) {
            style = "plain";
        }

        font += style;
        font += ",";
        font += Integer.toString(f.getSize());
        return font;
    }

    static public Font stringToFont(String value) {
        if(value == null) {
            value = "Monospaced,plain,12";
        }

        if (value.equals("default")) {
            if (get("theme.editor.fonts.default.font").equals("default")) {
                return new Font("Monospaced", Font.PLAIN, 12);
            } 
            return stringToFont(get("theme.editor.fonts.default.font"));
        }

        String[] pieces = value.split(",");

        if(pieces.length != 3) {
            return new Font("Monospaced", Font.PLAIN, 12);
        }

        String name = pieces[0];
        int style = Font.PLAIN;  // equals zero

        if(pieces[1].indexOf("bold") != -1) {
            style |= Font.BOLD;
        }

        if(pieces[1].indexOf("italic") != -1) {
            style |= Font.ITALIC;
        }

        int size;

        try {
            size = Integer.parseInt(pieces[2]);

            if(size <= 0) size = 12;
        } catch(Exception e) {
            size = 12;
        }

        if (!fontExists(name)) {
            return new Font("Monospaced", Font.PLAIN, 12);
        }

        Font font = new Font(name, style, size);

        if(font == null) {
            font = new Font("Monospaced", Font.PLAIN, 12);
        }

        return font;
    }

    public Preferences() {
    }

    static class PrefTreeEntry {
        String key;
        String name;
    
        PrefTreeEntry(String k, String n) {
            key = k;
            name = n;
        }
        
        public String getName() { return name; }
        public String getKey() { return key; }
        public String toString() { return name; }
    }

    // Interface to the global preferences.  These will first check the user's preferences
    // file for the value and if not found then will get the value from the .default entry
    // for the key in the preferences tree.

    public static String get(String key) {
        String data = preferences.get(key);
        if (data == null) {
            data = preferencesTree.get(key + ".default");
        }
        return data;
    }

    public static String[] getArray(String key) {
        String data = get(key);
        if (data == null || data.equals("")) {
            return null;
        }
        return data.split("::");
    }

    public static Boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public static Boolean getBoolean(String key, boolean def) {
        String data = get(key);
        if (data == null || data.equals("")) {
            return def;
        }
        data = data.toLowerCase();
        if (data.startsWith("y") || data.startsWith("t")) {
            return true;
        }
        return false;
    }

    public static Color getColor(String key) {
        Color parsed = Color.GRAY;
        String s = get(key);

        if((s != null) && (s.indexOf("#") == 0)) {
            try {
                parsed = new Color(Integer.parseInt(s.substring(1), 16));
            } catch(Exception e) { }
        }

        return parsed;
    }

    public static File getFile(String key) {
        String data = get(key);
        if (data == null || data.equals("")) {
            return null;
        }
        return new File(data);
    }

    public static String getFontCSS(String key) {
        Font f = getFont(key);

        String out = "font-family: " + f.getFamily() + ";";
        out += " font-size: " + f.getSize() + "px;";

        if (f.isBold()) {
            out += " font-weight: bold;";
        }

        if (f.isItalic()) {
            out += " font-style: italic;";
        }

        return out;
    }

    public static Font getFontNatural(String key) {
        String data = get(key);
        Font font = null;
        if (data == null || data.equals("")) {
            font = stringToFont("Monospaced,plain,12");
        } else {
            font = stringToFont(data);
        }
        return font;
    }

    static HashMap<String, Boolean> cachedFontList = new HashMap<String, Boolean>();

    public static boolean fontExists(String fontName) {
        if (cachedFontList.get(fontName) != null) {
            return cachedFontList.get(fontName);
        }
        GraphicsEnvironment g = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fonts = g.getAvailableFontFamilyNames();
        for (String availableFont : fonts) {
            if (availableFont.equals(fontName)) {
                cachedFontList.put(fontName, true);
                return true;
            }
        }
        cachedFontList.put(fontName, false);
        return false;
    }

    public static Font getScaledFont(String key, int scale) {
        String data = get(key);
        Font font = null;
        if (data == null || data.equals("")) {
            font = stringToFont("Monospaced,plain,12");
        } else {
            font = stringToFont(data);
        }
        float size = font.getSize();
        if (scale == 0) {
            scale = 100;
        }
        Font out = font.deriveFont(size * (float)scale / 100f);
        if (out.getSize() <= 0) {
            out = font.deriveFont(1f);
        }
        return out;
    }

    public static Font getFont(String key) {
        String data = get(key);
        Font font = null;
        if (data == null || data.equals("")) {
            font = stringToFont("Monospaced,plain,12");
        } else {
            font = stringToFont(data);
        }
        return font;
    }

    public static Integer getInteger(String key) {
        return getInteger(key, 0);
    }

    public static Integer getInteger(String key, Integer def) {
        String data = get(key);
        if (data == null || data.equals("")) {
            return def;
        }
        int val = def;
        try {
            val = Integer.parseInt(data);
        } catch (Exception e) {
        }
        return val;
    }

    public static Float getFloat(String key) {
        String data = get(key);
        if (data == null || data.equals("")) {
            return 0.0f;
        }
        float val = 0.0f;
        try {
            val = Float.parseFloat(data);
        } catch (Exception e) {
        }
        return val;
    }

    public static void set(String key, String value) { preferences.set(key, value); preferences.saveDelay(); }
    public static void setBoolean(String key, Boolean value) { preferences.setBoolean(key, value); preferences.saveDelay(); }
    public static void setColor(String key, Color value) { preferences.setColor(key, value); preferences.saveDelay(); }
    public static void setFile(String key, File value) { preferences.setFile(key, value); preferences.saveDelay(); }

    public static void setFont(String key, Font value) { 
        if (preferences != null) {
            preferences.setFont(key, value); 
            preferences.saveDelay(); 
        }
    }

    public static void setInteger(String key, int value) { 
        if (preferences != null) {
            preferences.setInteger(key, value); 
            preferences.saveDelay(); 
        }
    }

    public static void setFloat(String key, float value) { preferences.setFloat(key, value); preferences.saveDelay(); }

    public static void save() { preferences.save(); }

    public static void unset(String key) { preferences.unset(key); preferences.saveDelay(); }
        
    public static void importThemeData(File f) {
        PropertyFile pf = new PropertyFile(f);
        preferences.mergeData(pf);
    }

    public void exportThemeData(File f) {
        String fpath = f.getAbsolutePath();
        if (!fpath.endsWith(".utheme")) {
            fpath = fpath + ".utheme";
            f = new File(fpath);
        }

        PropertyFile pf = preferencesTree.getChildren("theme");
        PropertyFile newFile = new PropertyFile();

        ArrayList<String> keys = pf.keySet();

        for (String key : keys) {
            if (key.endsWith(".default")) {
                String trimmed = "theme." + key.substring(0, key.length()-8);
                String value = get(trimmed);

                newFile.set(trimmed, get(trimmed));
            }
        }
        newFile.save(f);
    }

    public static void buildPreferencesTree() {
        preferencesTree = new PropertyFile();


        for(Programmer c : UECIDE.programmers.values()) {
            PropertyFile prefs = c.getProperties().getChildren("prefs");
            for (String k : prefs.keySet()) {
                prefs.setSource(k, "programmer:" + c.getName());
            }
            preferencesTree.mergeData(prefs);
        }

        for(Compiler c : UECIDE.compilers.values()) {
            PropertyFile prefs = c.getProperties().getChildren("prefs");
            for (String k : prefs.keySet()) {
                prefs.setSource(k, "compiler:" + c.getName());
            }
            preferencesTree.mergeData(prefs);
        }

        for(Core c : UECIDE.cores.values()) {
            PropertyFile prefs = c.getProperties().getChildren("prefs");
            for (String k : prefs.keySet()) {
                prefs.setSource(k, "core:" + c.getName());
            }
            preferencesTree.mergeData(prefs);
        }

        for(Board c : UECIDE.boards.values()) {
            PropertyFile prefs = c.getProperties().getChildren("prefs");
            for (String k : prefs.keySet()) {
                prefs.setSource(k, "board:" + c.getName());
            }
            preferencesTree.mergeData(prefs);
        }

        loadPreferencesTree("/org/uecide/config/prefs.txt");
    }

    public static void registerPreference(String key, String type, String name, String def) {
        registerPreference(key, type, name, def, null);
    }

    public static void registerPreference(String key, String type, String name, String def, String plat) {
        preferencesTree.set(key + ".type", type);
        preferencesTree.set(key + ".name", name);
        if (plat == null) {
            preferencesTree.set(key + ".default", def);
        } else {
            preferencesTree.set(key + ".default." + plat, def);
        }
    }

    public static void loadPreferencesTree(String res) {
        PropertyFile pf = new PropertyFile(res);
        preferencesTree.mergeData(pf);
    }


    public static void init() {
        preferences = new PropertyFile(UECIDE.getDataFile("preferences.txt"), "/org/uecide/config/preferences.txt");
        preferences.setPlatformAutoOverride(true);
    }

    public static PropertyFile getChildren(String key) {
        return preferences.getChildren(key);
    }

}



