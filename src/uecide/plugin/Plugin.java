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
import uecide.app.debug.*;
import uecide.app.editors.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;



/**
 * Interface for items to be shown in the Tools menu.
 */
public abstract class Plugin {
    public Editor editor = null;
    public EditorBase editorTab = null;

    public static final int MENU_FILE = 1;
    public static final int MENU_EDIT = 2;
    public static final int MENU_SKETCH = 3;
    public static final int MENU_HARDWARE = 4;
    public static final int MENU_TOOLS = 5;
    public static final int MENU_HELP = 6;

    public static final int MENU_TOP = 256;
    public static final int MENU_MID = 512;
    public static final int MENU_BOTTOM = 768;

    public static final int TOOLBAR_EDITOR = 1;
    public static final int TOOLBAR_TAB = 2;

    public Plugin() { }
    public Plugin(Editor e) { editor = e; }
    public Plugin(EditorBase eb) { editorTab = eb; }

    public static void setLoader(URLClassLoader l) { };
    public static void setInfo(HashMap<String, String>info) { };
    public static String getInfo(String item) { return null; };

    public static void populatePreferences(JPanel panel) { }
    public static String getPreferencesTitle() { return null; }
    public static void savePreferences() { }

    public static boolean wantEditorInstance() { return false; }
    public static boolean wantTabInstance() { return false; }
    public abstract void addToolbarButtons(JToolBar toolbar, int flags);
    public abstract void populateMenu(JMenu menu, int flags);

    public void releasePort(String port) { };
}
