package uecide.app;

import uecide.plugin.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.Method;

import javax.swing.*;
import javax.swing.border.*;

import say.swing.*;

public class PropertyFile {

    Properties defaultProperties;
    Properties properties;
    File userFile;
    boolean doPlatformOverride = false;

    public PropertyFile(File user) {
        this(user, null);
    }

    public void setPlatformAutoOverride(boolean f) {
        doPlatformOverride = f;
    }

    public PropertyFile(File user, File defaults) {

        userFile = user;

        defaultProperties = new Properties();
        if (defaults != null) {
            if (defaults.exists()) {
                try {
                    defaultProperties.load(new FileReader(defaults));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        properties = new Properties(defaultProperties);
        if (user != null) {
            if (user.exists()) {
                try {
                    properties.load(new FileReader(user));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void save() {
        if (userFile != null) {
            try {
                properties.store(new FileWriter(userFile), null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String getPlatformSpecific(String attribute) {
        String t = properties.getProperty(attribute + "." + Base.getOSName());
        if (t != null) {
            return t.trim();
        }
        t  = properties.getProperty(attribute);
        if (t != null) {
            return t.trim();
        }
        return null;
    }

    public String get(String attribute) {
        if (doPlatformOverride) {
            return getPlatformSpecific(attribute);
        }
        String t = properties.getProperty(attribute);
        if (t != null) {
            return t.trim();
        }
        return null;
    }

    public String getDefault(String attribute) {
        String t = defaultProperties.getProperty(attribute);
        if (t != null) {
            return t.trim();
        }
        return null;
    }

    public void set(String attribute, String value) {
        properties.setProperty(attribute, value);
    }

    public void unset(String attribute) {
        properties.remove(attribute);
    }

    public boolean getBoolean(String attribute) {
        String value = get(attribute);
        return (new Boolean(value)).booleanValue();
    }

    public void setBoolean(String attribute, boolean value) {
        set(attribute, value ? "true" : "false");
    }

    public int getInteger(String attribute) {
        try {
            return Integer.parseInt(get(attribute));
        } catch (Exception e) {
            return 0;
        }
    }

    public void setInteger(String key, int value) {
        set(key, String.valueOf(value));
    }

    public Color getColor(String name) {
        Color parsed = Color.GRAY;
        String s = get(name);
        if ((s != null) && (s.indexOf("#") == 0)) {
            try {
                parsed = new Color(Integer.parseInt(s.substring(1), 16));
            } catch (Exception e) { }
        }
        return parsed;
    }

    public void setColor(String attr, Color what) {
        set(attr, String.format("#%06X",what.getRGB() & 0xffffff));
    }

    public Font getFont(String attr) {
        return stringToFont(get(attr));
    }

    public void setFont(String attr, Font value) {
        set(attr, fontToString(value));
    }

    public String fontToString(Font f)
    {
        String font = f.getName();
        String style = "";
        font += ",";
        if ((f.getStyle() & Font.BOLD) != 0) {
            style += "bold";
        }
        if ((f.getStyle() & Font.ITALIC) != 0) {
            style += "italic";
        }
        if (style.equals("")) {
            style = "plain";
        }
        font += style;
        font += ",";
        font += Integer.toString(f.getSize());
        return font;
    }

    public Font stringToFont(String value) {
        if (value == null) {
            value="Monospaced,plain,12";
        }

        String[] pieces = value.split(",");
        if (pieces.length != 3) {
            return new Font("Monospaced", Font.PLAIN, 12);
        }

        String name = pieces[0];
        int style = Font.PLAIN;  // equals zero
        if (pieces[1].indexOf("bold") != -1) {
            style |= Font.BOLD;
        }
        if (pieces[1].indexOf("italic") != -1) {
            style |= Font.ITALIC;
        }
        int size;
        try {
            size = Integer.parseInt(pieces[2]);
            if (size <= 0) size = 12;
        } catch (Exception e) {
            size = 12;
        }
 
        Font font = new Font(name, style, size);

        if (font == null) {
            font = new Font("Monospaced", Font.PLAIN, 12);
        }
        return font;
    }

    Properties getProperties() {
        return properties;
    }
}
