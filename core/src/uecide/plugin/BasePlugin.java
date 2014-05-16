/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Tool - interface implementation for the Processing tools menu
  Part of the Processing project - http://processing.org

  Copyright (c) 2008 Ben Fry and Casey Reas

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package uecide.plugin;

import uecide.app.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;


public class BasePlugin implements Plugin {

    public static final int MENU_FILE = 1;
    public static final int MENU_EDIT = 2;
    public static final int MENU_SKETCH = 3;
    public static final int MENU_HARDWARE = 4;
    public static final int MENU_TOOLS = 5;
    public static final int MENU_HELP = 6;
    
    public static final int MENU_TOP = 256;
    public static final int MENU_MID = 512;
    public static final int MENU_BOTTOM = 768;

    public static final int MENU_PLUGIN_MAIN = 1;
    public static final int LOADER = 2;
    public static final int MENU_PLUGIN_TOP = 3;
    public static final int MENU_EDIT_TOP = 4;
    public static final int MENU_EDIT_MID = 5;
    public static final int MENU_EDIT_LOW = 6;
    public static final int MENU_EDIT_BOT = 7;

    public Editor editor;
    public Map pluginInfo;
    public URLClassLoader loader;

    public int flags() {
        return MENU_TOOLS | MENU_TOP;
    }

    public void init(Editor editor)
    {
        this.editor = editor;
    }

    public void run()
    {
    }

    public String getMenuTitle()
    {
        return "My Unnamed Plugin";
    }

    public void setInfo(Map pluginInfo)
    {
        this.pluginInfo = pluginInfo;
    }

    public String getVersion()
    {
        return (String) pluginInfo.get("version");
    }

    public String getCompiled()
    {
        return (String) pluginInfo.get("compiler");
    }

    public void setLoader(URLClassLoader loader)
    {
        this.loader = loader;
    }

    public URLClassLoader getLoader()
    {
        return loader;
    }

    public InputStream getResourceAsStream(String file) 
    {
        if (loader == null) {
            System.err.println("I don't have a loader!!!");
            return null;
        }
        try {
            return loader.getResourceAsStream(file);
        } catch (Exception e) {
            System.err.println("Resource not found: " + file);
        }
        return null;
    }

    public URL getResourceURL(String file)
    {
        if (loader == null) {
            System.err.println("I don't have a loader!!!");
            return null;
        }
        URL out;
        try {
            out = loader.getResource(file);
            if (out == null) {
                System.err.println("Unable to locate " + file);
            }   
            return out;
        } catch (Exception e) {
            System.err.println("Resource not found: " + file);
        }
        return null;
    }

    public char getShortcut()
    {
        if (pluginInfo.get("shortcut") != null) {
            return ((String) pluginInfo.get("shortcut")).charAt(0);
        } 
        return 0;
    }

    public ImageIcon toolbarIcon()
    {
        return null;
    }

    public int getModifier()
    {
        String mod = (String)pluginInfo.get("modifier");
        if (mod == null) {
            return 0;
        }
        if (mod.toLowerCase().equals("shift")) {
            return ActionEvent.SHIFT_MASK;
        }
        return 0;
    }

    public File getJarFile() {
        return new File((String)pluginInfo.get("jarfile"));
    }
}

