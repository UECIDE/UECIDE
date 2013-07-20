/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

package uecide.app;

import java.awt.*;
import java.io.*;
import java.util.*;

/**
 * Storage class for theme settings. This was separated from the Preferences
 * class for 1.0 so that the coloring wouldn't conflict with previous releases
 * and to make way for future ability to customize.
 */
public class Theme {

  /** Copy of the defaults in case the user mangles a preference. */
  static public HashMap<String,String> defaults;
  /** Table of attributes/values for the theme. */
  static public HashMap<String,String> table = new HashMap<String,String>();;


  static protected void init() {
    try {
      load(Base.getContentFile("lib/theme/theme.txt"));
    } catch (Exception te) {
      Base.showError(null, "Could not read color theme settings.\n" +
                           "You'll need to reinstall Processing.", te);
    }

    // check for platform-specific properties in the defaults
    String platformExt = "." + Base.osName();
    int platformExtLength = platformExt.length();
    for (String key : table.keySet()) {
      if (key.endsWith(platformExt)) {
        // this is a key specific to a particular platform
        String actualKey = key.substring(0, key.length() - platformExtLength);
        String value = get(key);
        table.put(actualKey, value);
      }
    }

    // other things that have to be set explicitly for the defaults
    setColor("run.window.bgcolor", SystemColor.control);

    // clone the hash table
    defaults = (HashMap<String, String>) table.clone();
  }


  static protected void load(File input) throws IOException {
    ArrayList<String> lines = Preferences.loadStrings(input);
    for (String line : lines) {
      if ((line.length() == 0) ||
          (line.charAt(0) == '#')) continue;

      // this won't properly handle = signs being in the text
      int equals = line.indexOf('=');
      if (equals != -1) {
        String key = line.substring(0, equals).trim();
        String value = line.substring(equals + 1).trim();
        table.put(key, value);
      }
    }
  }


  static public String get(String attribute, String def) {
    String v = get(attribute);
    if (v == null) {
        return def;
    }
    return v;
  }

  static public String get(String attribute) {
    return (String) table.get(attribute);
  }


  static public String getDefault(String attribute) {
    return (String) defaults.get(attribute);
  }


  static public void set(String attribute, String value) {
    table.put(attribute, value);
  }


  static public boolean getBoolean(String attribute) {
    String value = get(attribute);
    return (new Boolean(value)).booleanValue();
  }


  static public void setBoolean(String attribute, boolean value) {
    set(attribute, value ? "true" : "false");
  }


  static public int getInteger(String attribute) {
    return Integer.parseInt(get(attribute));
  }


  static public void setInteger(String key, int value) {
    set(key, String.valueOf(value));
  }


  static public Color getColor(String name) {
    Color parsed = null;
    String s = get(name);
    if ((s != null) && (s.indexOf("#") == 0)) {
      try {
        int v = Integer.parseInt(s.substring(1), 16);
        parsed = new Color(v);
      } catch (Exception e) {
      }
    }
    return parsed;
  }


  static public void setColor(String attr, Color what) {
    set(attr, String.format("#%06X", what.getRGB() & 0xffffff));
  }

  static public Font getFont(String attr) {
    boolean replace = false;
    String value = get(attr);
    if (value == null) {
      value = getDefault(attr);
      replace = true;
    }

    if (value == null) {
        return null;
    }

    String[] pieces = value.split(",");
    if (pieces.length != 3) {
      value = getDefault(attr);
      pieces = value.split(",");
      replace = true;
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

    // replace bad font with the default
    if (replace) {
      set(attr, value);
    }

    return font;
  }

}
