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

package org.uecide.plugin;

import org.uecide.*;
import org.uecide.editors.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;
import java.util.jar.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.tree.*;



/**
 * Interface for items to be shown in the Tools menu.
 */
public abstract class Plugin {
    public Editor editor = null;
    public EditorBase editorTab = null;

    // Main system menu entries
    public static final int MENU_FILE       = 0x0001;
    public static final int MENU_EDIT       = 0x0002;
    public static final int MENU_SKETCH     = 0x0003;
    public static final int MENU_HARDWARE   = 0x0004;
    public static final int MENU_TOOLS      = 0x0005;
    public static final int MENU_HELP       = 0x0006;

    // Project tree folder entries
    public static final int MENU_TREE_SKETCH    = 0x0010; // Root level sketch folder
    public static final int MENU_TREE_SOURCE    = 0x0011; // Source files
    public static final int MENU_TREE_HEADERS   = 0x0012; // Header files
    public static final int MENU_TREE_LIBRARIES = 0x0013; // Imported libraries
    public static final int MENU_TREE_BINARIES  = 0x0014; // Imported libraries
    public static final int MENU_TREE_OUTPUT    = 0x0015; // Output files
    public static final int MENU_TREE_FILE      = 0x0016; // Any file in the tree

    // File tree entries
    public static final int MENU_FILE_FOLDER    = 0x0021; // A folder in the file tree
    public static final int MENU_FILE_FILE      = 0x0022; // A file in the file tree

    // Menus are split into three regions - top middle and bottom.
    public static final int MENU_TOP    = 0x0100;
    public static final int MENU_MID    = 0x0200;
    public static final int MENU_BOTTOM = 0x0300;

    public static final int TOOLBAR_EDITOR = 1;
    public static final int TOOLBAR_TAB = 2;


    public Plugin() { }
    public Plugin(Editor e) {
        editor = e;
    }
    public Plugin(EditorBase eb) {
        editorTab = eb;
    }

    public static void populatePreferences(JPanel panel) { }
    public static String getPreferencesTitle() {
        return null;
    }
    public static void savePreferences() { }

    public static boolean wantEditorInstance() {
        return false;
    }
    public static boolean wantTabInstance() {
        return false;
    }
    public abstract void addToolbarButtons(JToolBar toolbar, int flags);
    public abstract void populateMenu(JMenu menu, int flags);
    public abstract void populateContextMenu(JPopupMenu menu, int flags, DefaultMutableTreeNode node);
    public abstract ImageIcon getFileIconOverlay(File f);

    public void releasePort(String port) { }

    public void launch() {
        System.err.println("Not overridden");
    }

    public void catchEvent(int event) { };
}
