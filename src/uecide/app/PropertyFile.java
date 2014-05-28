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

package uecide.app;

import uecide.plugin.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.Method;
import java.net.*;

import javax.swing.*;
import javax.swing.border.*;

import say.swing.*;

import java.util.Timer;

public class PropertyFile {

    Properties defaultProperties;
    Properties properties;
    File userFile;
    boolean doPlatformOverride = false;

    public PropertyFile(File user) {
        this(user, (File)null);
    }

    public void setPlatformAutoOverride(boolean f) {
        doPlatformOverride = f;
    }

    public PropertyFile(String user) {
        userFile = null;

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(user)));
            properties = new Properties();
            if (br != null) {
                properties.load(br);
            }
            br.close();
        } catch (Exception e) {
            Base.error(e);
        }
    }

    public PropertyFile(File user, String defaults) {
        userFile = user;

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(defaults)));
            defaultProperties = new Properties();
            if (br != null) {
                defaultProperties.load(br);
            }
            br.close();
            properties = new Properties(defaultProperties);
            if (user != null) {
                if (user.exists()) {
                    properties.load(new FileReader(user));
                }
            }
        } catch (Exception e) {
            Base.error(e);
        }
    }

    public PropertyFile(File user, File defaults) {

        userFile = user;

        defaultProperties = new Properties();
        if (defaults != null) {
            if (defaults.exists()) {
                try {
                    defaultProperties.load(new FileReader(defaults));
                } catch (Exception e) {
                    Base.error(e);
                }
            }
        }
        properties = new Properties(defaultProperties);
        if (user != null) {
            if (user.exists()) {
                try {
                    properties.load(new FileReader(user));
                } catch (Exception e) {
                    Base.error(e);
                }
            }
        }
    }

    public PropertyFile() {
        userFile = null;
        defaultProperties = new Properties();
        properties = new Properties();
    }

    public PropertyFile(TreeMap<String, String>data) {
        userFile = null;
        defaultProperties = new Properties();
        properties = new Properties();
        mergeData(data);
    }

    public PropertyFile(PropertyFile pf) {
        userFile = null;
        defaultProperties = new Properties();
        properties = new Properties();
        mergeData(pf);
    }

    public void mergeData(TreeMap<String, String>data) {
        if (data == null) {
            return;
        }
        for (String key : data.keySet()) {
            set(key, data.get(key));
        }
    }

    public void mergeData(PropertyFile pf) {
        if (pf == null) {
            return;
        }
        for (String key: pf.getProperties().stringPropertyNames()) {
            set(key, pf.getProperties().getProperty(key));
        }
    }

    public void save() {
        if (userFile != null) {
            try {
                Properties saveProps = new Properties() {
                    @Override
                    public synchronized Enumeration<Object> keys() {
                        return Collections.enumeration(new TreeSet<Object>(super.keySet()));
                    }
                };
                saveProps.putAll(properties);
                saveProps.store(new FileWriter(userFile), null);
                Debug.message("Saved property file " + userFile.getAbsolutePath());
            } catch (Exception e) {
                Base.error(e);
            }
        }
    }

    Timer saveTimer = null;

    public void saveDelay() {
        if (saveTimer != null) {    
            saveTimer.cancel();
            saveTimer.purge();
        }
        saveTimer = new Timer();
        saveTimer.schedule(new TimerTask() {
            public void run() {
                save();
            }
        }, 1000);
    }

    public String getPlatformSpecific(String attribute) {
        String t = properties.getProperty(attribute + "." + Base.getOSFullName());
        if (t != null) {
            return t.trim();
        }
        t = properties.getProperty(attribute + "." + Base.getOSName());
        if (t != null) {
            return t.trim();
        }
        t  = properties.getProperty(attribute);
        if (t != null) {
            return t.trim();
        }
        return null;
    }

    public String[] getArray(String attribute) {
        String rawData = null;
        if (doPlatformOverride) {
            rawData = getPlatformSpecific(attribute);
        } else {
            rawData = properties.getProperty(attribute);
        }
        if (rawData == null) {
            return null;
        }
        return rawData.split("::");
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
        if (value == null) {
            return;
        }
        if (attribute == null) {
            return;
        }
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

    public int getInteger(String attribute, int def) {
        try {
            return Integer.parseInt(get(attribute));
        } catch (Exception e) {
            return def;
        }
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

    public void setFile(String attr, File f) {
        set(attr, f.getAbsolutePath());
    }

    public File getFile(String attr) {
        String s = get(attr);
        if (s != null) {
            return new File(s);
        }
        return null;
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

    public Properties getProperties() {
        return properties;
    }

    public TreeMap<String, String> toTreeMap() {
        return toTreeMap(false);
    }

    public TreeMap<String, String> toTreeMap(boolean ps) {
        TreeMap<String, String> map = new TreeMap<String, String>();
        for (String name: properties.stringPropertyNames()) {
            if (ps) {
                if (name.endsWith("." + Base.getOSFullName())) {
                    name = name.substring(0, name.length() - Base.getOSFullName().length() - 1);
                }
                if (name.endsWith("." + Base.getOSName())) {
                    name = name.substring(0, name.length() - Base.getOSName().length() - 1);
                }
                map.put(name, getPlatformSpecific(name));
            } else {
                map.put(name, properties.getProperty(name));
            }
        }
        return map;
    }

    public void loadNewUserFile(File user) {
        if (user != null) {
            if (user.exists()) {
                try {
                    // If the new properties fail to load (syntax error, for example) we don't
                    // want to muller the old properties, so we load them into a fresh
                    // properties object and only replace the old ones with the new if it
                    // is all successful.
                    Properties newProperties = new Properties(defaultProperties);
                    newProperties.load(new FileReader(user));
                    properties = newProperties;
                } catch (Exception e) {
                    Base.error(e);
                }
            }
        }
    }

    public PropertyFile getChildren(String path) {
        if (path == null || path == "") {
            return new PropertyFile(this);
        }

        PropertyFile subset = new PropertyFile();
        if (!path.endsWith(".")) {
            path += ".";
        }

        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(path)) {
                subset.set(key.substring(path.length()), properties.getProperty(key));
            }
        }
        return subset;
    }

    // Return the top level of directly descendant child keys.  That is,
    // if the property file contains:
    //      foo.bar
    //      foo.baz
    //      bar.foo
    // it will return { "bar", "foo" }.
    public String[] childKeys() {
        ArrayList<String> keys = new ArrayList<String>();
        for (String key : properties.stringPropertyNames()) {
            String[] bits = key.split("\\.");
            if (keys.indexOf(bits[0]) == -1) {
                keys.add(bits[0]);
            }
        }
        String[] keyArray = keys.toArray(new String[0]);
        Arrays.sort(keyArray);
        return keyArray;
    }

    public String[] childKeysOf(String path) {
        PropertyFile pf = getChildren(path);
        return pf.childKeys();
    }
}
