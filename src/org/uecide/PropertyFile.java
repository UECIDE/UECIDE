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

import org.uecide.plugin.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.Method;
import java.net.*;
import java.util.regex.*;

import javax.swing.*;
import javax.swing.border.*;

import say.swing.*;

import java.util.Timer;

/*! The PropertyFile class stores a set of key/value pairs.  The keys can be
 *  used verbatim, or traversed and searched in the form of a tree.  Nodes in
 *  the tree are separated with a period.
 */
public class PropertyFile {

    Properties defaultProperties;
    Properties properties;
    File userFile;
    boolean doPlatformOverride = false;

    /*! Create a new PropertyFile from a file on disk.  All properties are loaded and stored from the file. */
    public PropertyFile(File user) {
        this(user, (File)null);
    }

    public void setPlatformAutoOverride(boolean f) {
        doPlatformOverride = f;
    }

    /*! Create a new PropertyFile from a file in the class path described by a String. */
    public PropertyFile(String user) {
        userFile = null;

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(user)));
            properties = new Properties();

            if(br != null) {
                //loadProperties(properties, br);
                properties.load(br);
            }

            br.close();
        } catch(Exception e) {
            Base.error(e);
        }
    }

    /*! Create a new PropertyFile from a file on disk and a resource in the class path.  The defaults resource is first read, and
     *  then the user file is read and overlaid on the top.  This allows system defaults to be over-ridden
     *  by user specific options.
     */
    public PropertyFile(File user, String defaults) {
        userFile = user;

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(defaults)));
            defaultProperties = new Properties();

            if(br != null) {
//                loadProperties(defaultProperties, br);
                defaultProperties.load(br);
            }

            br.close();
            properties = new Properties(defaultProperties);

            if(user != null) {
                if(user.exists()) {
                    BufferedReader r = new BufferedReader(new FileReader(user));
//                    loadProperties(properties, r);
                    properties.load(r);
                    r.close();
                }
            }
        } catch(Exception e) {
            Base.error(e);
        }
    }

    /*! Create a new PropertyFile from a pair of files on disk.  The defaults file is first read, and
     *  then the user file is read and overlaid on the top.  This allows system defaults to be over-ridden
     *  by user specific options.
     */
    public PropertyFile(File user, File defaults) {

        userFile = user;

        defaultProperties = new Properties();

        if(defaults != null) {
            if(defaults.exists()) {
                try {
                    BufferedReader r = new BufferedReader(new FileReader(defaults));
//                    loadProperties(defaultProperties, r);
                    defaultProperties.load(r);
                    r.close();
                } catch(Exception e) {
                    Base.error(e);
                }
            }
        }

        properties = new Properties(defaultProperties);

        if(user != null) {
            if(user.exists()) {
                try {
                    BufferedReader r = new BufferedReader(new FileReader(user));
//                    loadProperties(properties, r);
                    properties.load(r);
                    r.close();
                } catch(Exception e) {
                    Base.error(e);
                }
            }
        }
    }

    /*! Create a new empty PropertyFile. */
    public PropertyFile() {
        userFile = null;
        defaultProperties = new Properties();
        properties = new Properties();
    }

    /*! Create a new PropertyFile from a set of properties stored in a TreeMap<String, String> object. */
    public PropertyFile(TreeMap<String, String>data) {
        userFile = null;
        defaultProperties = new Properties();
        properties = new Properties();
        mergeData(data);
    }

    /*! Create a new PropertyFile from the contents of an existing PropertyFile. */
    public PropertyFile(PropertyFile pf) {
        userFile = null;
        defaultProperties = new Properties();
        properties = new Properties();
        mergeData(pf);
    }

    /*! Merge the data from a TreeMap<String, String> object into this PropertyFile. */
    public void mergeData(TreeMap<String, String>data) {
        if(data == null) {
            return;
        }

        for(String key : data.keySet()) {
            set(key, data.get(key));
        }
    }

    /*! Merge the data from an existing PropertyFile into this PropertyFile. */
    public void mergeData(PropertyFile pf) {
        if(pf == null) {
            return;
        }

        for(String key : pf.getProperties().stringPropertyNames()) {
            set(key, pf.getProperties().getProperty(key));
        }
    }

    /*! Merge the data from an existing PropertyFile into this PropertyFile prepending *prefix* on to each key. */
    public void mergeData(PropertyFile pf, String prefix) {
        if(pf == null) {
            return;
        }

        if(!prefix.endsWith(".")) {
            prefix += ".";
        }

        for(String key : pf.getProperties().stringPropertyNames()) {
            set(prefix + key, pf.getProperties().getProperty(key));
        }
    }

    /*! Save the properties out to the currently registered backing file */
    public void save() {
        if(userFile != null) {
            try {
                Properties saveProps = new Properties() {
                    @Override

                    public synchronized Enumeration<Object> keys() {
                        return Collections.enumeration(new TreeSet<Object>(super.keySet()));
                    }
                };
                saveProps.putAll(properties);
                FileWriter w = new FileWriter(userFile);
                saveProps.store(w, null);
                w.close();
                Debug.message("Saved property file " + userFile.getAbsolutePath());
            } catch(Exception e) {
                Base.error(e);
            }
        }
    }

    Timer saveTimer = null;

    /*! Save the properties out to the currently registered backing file after a delay of 1 second.  If a save is already pending
     *  then the new save's delay overrides it extending the delay.
     */
    public void saveDelay() {
        if(saveTimer != null) {
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

    /*! Get a String value for the specified key after appending the OS details to the key.
     *  The tree is searched up from the most specific key.*os_arch* though the less specific
     *  key.*os* and finally the plain key by itself.
     */
    public String getPlatformSpecific(String attribute) {
        String t = properties.getProperty(attribute + "." + Base.getOSFullName());

        if(t != null) {
            return t.trim();
        }

        t = properties.getProperty(attribute + "." + Base.getOSName());

        if(t != null) {
            return t.trim();
        }

        t  = properties.getProperty(attribute);

        if(t != null) {
            return t.trim();
        }

        return null;
    }

    /*! Get a String[] array value from the specified key.  A String array is internally
     *  stored as a single String with each element separated by :: 
     */
    public String[] getArray(String attribute) {
        String rawData = null;

        if(doPlatformOverride) {
            rawData = getPlatformSpecific(attribute);
        } else {
            rawData = properties.getProperty(attribute);
        }

        if(rawData == null) {
            return null;
        }

        return rawData.split("::");
    }

    /*! Get a raw String value from a key.  If the value doesn't exist
     *  then return the provided *defaultValue* value.
     */
    public String get(String attribute, String defaultValue) {
        String val = get(attribute);

        if(val == null) {
            return defaultValue;
        }

        return val;
    }

    /*! Get a raw String value from a key.  If it doesn't exist then return
     *  *null*.
     */
    public String get(String attribute) {
        if(doPlatformOverride) {
            return getPlatformSpecific(attribute);
        }

        String t = properties.getProperty(attribute);

        if(t != null) {
            return t.trim();
        }

        return null;
    }

    /*! Get the default value for a key rather than the user over-ridden value. */
    public String getDefault(String attribute) {
        String t = defaultProperties.getProperty(attribute);

        if(t != null) {
            return t.trim();
        }

        return null;
    }

    /*! Set a key to the specified String value */
    public void set(String attribute, String value) {
        if(value == null) {
            return;
        }

        if(attribute == null) {
            return;
        }

        properties.setProperty(attribute, value);
    }

    /*! Unset a key. If a default exists that value will now be the current value. */
    public void unset(String attribute) {
        properties.remove(attribute);
    }

    /*! Get a boolean value from a key.  The value is true if it is stored as one of:
     *  * true
     *  * yes
     *  * y
     *  * on
     *  * 1
     *  All other values are false.
     */
    public boolean getBoolean(String attribute) {
        String value = get(attribute);

        if(value == null) {
            return false;
        }

        if(value.toLowerCase().equals("true")) {
            return true;
        }

        if(value.toLowerCase().equals("yes")) {
            return true;
        }

        if(value.toLowerCase().equals("y")) {
            return true;
        }

        if(value.toLowerCase().equals("on")) {
            return true;
        }

        if(value.toLowerCase().equals("1")) {
            return true;
        }

        return false;
    }

    /*! Set a key to either *true* or *false* depending on the provided boolean value. */
    public void setBoolean(String attribute, boolean value) {
        set(attribute, value ? "true" : "false");
    }

    /*! Parse a key's value as an integer. If the parsing fails the default value *def* is returned. */
    public int getInteger(String attribute, int def) {
        try {
            return Integer.parseInt(get(attribute));
        } catch(Exception e) {
            return def;
        }
    }

    /*! Parse a key's value as an integer. If the parsing fails the value 0 is returned. */
    public int getInteger(String attribute) {
        try {
            return Integer.parseInt(get(attribute));
        } catch(Exception e) {
            return 0;
        }
    }

    /*! Set a key to the specified integer value. */
    public void setInteger(String key, int value) {
        set(key, String.valueOf(value));
    }

    /*! Parse a key's value as a hexidecimal colour string in the format #RRGGBB.
     *  The result is returned as a Color object.  If the colour cannot be parsed
     *  then Color.GRAY is returned.
     */
    public Color getColor(String name) {
        Color parsed = Color.GRAY;
        String s = get(name);

        if((s != null) && (s.indexOf("#") == 0)) {
            try {
                parsed = new Color(Integer.parseInt(s.substring(1), 16));
            } catch(Exception e) { }
        }

        return parsed;
    }

    /*! Store a colour to the specified key. */
    public void setColor(String attr, Color what) {
        set(attr, String.format("#%06X", what.getRGB() & 0xffffff));
    }

    /*! Convert a key's value into a Font object.  A font is internally stored as 
     *  * <family>,<style>,<size>
     */
    public Font getFont(String attr) {
        return stringToFont(get(attr));
    }

    /*! Store a font specification in the key. */
    public void setFont(String attr, Font value) {
        set(attr, fontToString(value));
    }

    /*! Store the full absolute path of a file in the key. */
    public void setFile(String attr, File f) {
        set(attr, f.getAbsolutePath());
    }

    /*! Interpret the key's value as a path name and return a File object for it. */
    public File getFile(String attr) {
        String s = get(attr);

        if(s != null) {
            return new File(s);
        }

        return null;
    }

    /*! Convert a Font into the internal String representation. */
    public String fontToString(Font f) {
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

    /*! Convert a String representation of a Font into a Font. */
    public Font stringToFont(String value) {
        if(value == null) {
            value = "Monospaced,plain,12";
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

        Font font = new Font(name, style, size);

        if(font == null) {
            font = new Font("Monospaced", Font.PLAIN, 12);
        }

        return font;
    }

    /*! Return the internal Properties object used to store the data */
    public Properties getProperties() {
        return properties;
    }

    /*! Return all the properties as a TreeMap object */
    public TreeMap<String, String> toTreeMap() {
        return toTreeMap(false);
    }

    /*! Return all the properties as a TreeMap object.  If *ps* is *true* the platform
     *  specific variants of keys are used.
     */
    public TreeMap<String, String> toTreeMap(boolean ps) {
        TreeMap<String, String> map = new TreeMap<String, String>();

        for(String name : properties.stringPropertyNames()) {
            if(ps) {
                if(name.endsWith("." + Base.getOSFullName())) {
                    name = name.substring(0, name.length() - Base.getOSFullName().length() - 1);
                }

                if(name.endsWith("." + Base.getOSName())) {
                    name = name.substring(0, name.length() - Base.getOSName().length() - 1);
                }

                map.put(name, getPlatformSpecific(name));
            } else {
                map.put(name, properties.getProperty(name));
            }
        }

        return map;
    }

    /*! Replace the current user data with data from the provided File. */
    public void loadNewUserFile(File user) {
        if(user != null) {
            if(user.exists()) {
                try {
                    // If the new properties fail to load (syntax error, for example) we don't
                    // want to muller the old properties, so we load them into a fresh
                    // properties object and only replace the old ones with the new if it
                    // is all successful.
                    Properties newProperties = new Properties(defaultProperties);
                    BufferedReader r = new BufferedReader(new FileReader(user));
//                    loadProperties(newProperties, r);
                    newProperties.load(r);
                    r.close();
                    properties = newProperties;
                } catch(Exception e) {
                    Base.error(e);
                }
            }
        }
    }

    /*! Return a PropertyFile which is a subset of all the keys below the provided *path*. */
    public PropertyFile getChildren(String path) {
        if(path == null || path == "") {
            return new PropertyFile(this);
        }

        PropertyFile subset = new PropertyFile();

        if(!path.endsWith(".")) {
            path += ".";
        }

        for(String key : properties.stringPropertyNames()) {
            if(key.startsWith(path)) {
                subset.set(key.substring(path.length()), properties.getProperty(key));
            }
        }

        return subset;
    }

    /*! Return the top level of directly descendant child keys.  That is,
     * if the property file contains:
     * * foo.bar
     * * foo.baz
     * * bar.foo
     * it will return { "bar", "foo" }.
     */
    public String[] childKeys() {
        ArrayList<String> keys = new ArrayList<String>();

        for(String key : properties.stringPropertyNames()) {
            String[] bits = key.split("\\.");

            if(keys.indexOf(bits[0]) == -1) {
                keys.add(bits[0]);
            }
        }

        String[] keyArray = keys.toArray(new String[0]);
        Arrays.sort(keyArray);
        return keyArray;
    }

    /*! Return all the top-level child keys of the given path.  See: childKeys() */
    public String[] childKeysOf(String path) {
        PropertyFile pf = getChildren(path);
        return pf.childKeys();
    }

    /*! Return the number of elements in the user data set. */
    public int size() {
        return properties.size();
    }

    /*! Return a full Set of the keys in the user data. */
    public Set<Object> keySet() {
        return properties.keySet();
    }

    /*! Find if a key exists, either as an entry in its
     *  own right, or as a parent part of a key that
     *  exists.
     */
    public boolean keyExists(String key) {
        if(properties.get(key) != null) {
            return true;
        }

        for(Object ko : properties.keySet()) {
            String k = (String)ko;

            if(k.startsWith(key + ".")) {
                return true;
            }
        }

        return false;
    }

    /*! Obtain the best match key for the current operating system. */
    public String keyForOS(String key) {
        String os = Base.getOSName();
        String arch = Base.getOSArch();

        if(properties.get(key + "." + os + "_" + arch) != null) {
            return key + "." + os + "_" + arch;
        }

        if(properties.get(key + "." + os) != null) {
            return key + "." + os;
        }

        return key;
    }

    public String getParsed(String key) {
        String data = get(key);
        if (data == null) {
            return null;
        }
        return Base.parseString(data, this, null);
    }

    public void fullyParseFile() {
        for (String key : properties.stringPropertyNames()) {
            String data = get(key);
            data = Base.parseString(data, this, null);
            set(key, data);
        }
    }

    public boolean loadProperties(Properties p, BufferedReader r) {
        String line;
        Pattern keyval = Pattern.compile("^([^=\\s]+)\\s*=\\s*(.*)$");
        Pattern filename = Pattern.compile("file\\s*=\\s*([^\\s])+");
        Pattern format = Pattern.compile("format\\s*=\\s*([^\\s])+");
        try {
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("@")) {
                    if (line.startsWith("@include ")) {
                        Matcher fnmatch = filename.matcher(line);
                        Matcher fmtmatch = format.matcher(line);
                        if (fnmatch.find()) {
                            String fn = fnmatch.group(1);
                            String fmt = "propertyfile";
                            if (fmtmatch.find()) {
                                fmt = fmtmatch.group(1);
                            }
                            if (fmt.equals("propertyfile")) {
                            } else if (fmt.equals("arduino")) {
                            }
                        }
                    }
                    continue;
                }
                while (line.endsWith("\\")) {
                    line = line.substring(0, line.length() - 1);
                    String cont = r.readLine();
                    if (cont == null) {
                        return true;
                    }
                    line += cont.trim();
                }
                Matcher kvm = keyval.matcher(line);
                if (kvm.find()) {
                    String k = kvm.group(1);
                    String v = kvm.group(2);
                    v = v.replace("\\#", "#");
                    v = v.replace("\\\\", "\\");
                    if (k.contains("color")) {
                        System.err.println("[" + k + "] = [" + v + "]");
                    }
                    p.put(k,v);
                }
            }
        } catch (Exception e) {
            Base.error(e);
            return false;
        }
        return true;
    }

    public static PropertyFile parseArduinoFile(String filename) {
        try {
            FileReader fr = new FileReader(filename);
            if (fr == null) {
                return null;
            }
            BufferedReader br = new BufferedReader(fr);
            String line;
            PropertyFile props = new PropertyFile();
            Pattern keyval = Pattern.compile("^([^=\\s]+)\\s*=\\s*(.*)$");
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#")) {
                    continue;
                }
                Matcher kvm = keyval.matcher(line);
                if (kvm.find()) {
                    String key = kvm.group(1);
                    String val = kvm.group(2);

                    val = val.replace("{", "%%START%%");
                    val = val.replace("%%START%%", "${");
                    String[] sections = val.split("(?<=\")\\s+(?=\")");
                    boolean first = true;
                    String out = "";
                    for (String section : sections) {
                        if (!first) {
                            out += "::";
                        }
                        first = false;
                        if (section.startsWith("\"") && section.endsWith("\"")) {
                            section = section.substring(1, section.length()-2);
                        }
                        out += section;
                    }
                    props.set(key, out);
                }
            }
            br.close();
            return props;
        } catch (Exception e) {
            Base.error(e);
        }
        return null;
    }
}
