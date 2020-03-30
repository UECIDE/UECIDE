/*
 * Copyright (c) 2015, Majenko Technologies
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
import org.uecide.editors.*;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

import java.util.*;
import java.io.*;
import java.net.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.text.*;
import javax.swing.border.*;
import java.lang.reflect.*;
import javax.imageio.*;

import java.awt.datatransfer.*;

import org.uecide.Compiler;

import java.beans.*;

public class TabLabel extends JPanel {
    JLabel nameLabel;
    File sketchFile = null;
    String name;
    long expectedFileTime;
    boolean modified = false;
    boolean fileWatchMutex = false;
    Editor editor;

    boolean isSelected = false;

    public TabLabel(Editor e, File sf) throws IOException {
        this(e, sf.getName(), sf);
    }

    public TabLabel(Editor e, String tabname) throws IOException {
        this(e, tabname, null);
    }

    public TabLabel(Editor e, String tabname, File sf) throws IOException {
        editor = e;
        sketchFile = sf;
        name = tabname;
        setLayout(new BorderLayout(5, 0));
        setOpaque(false);

        nameLabel = new JLabel(name);
        nameLabel.setOpaque(false);
        add(nameLabel, BorderLayout.CENTER);

        if (sketchFile != null) {
            JLabel fileIconLabel = new JLabel(IconManager.getIcon(12, "mime." + FileType.getIcon(sketchFile)));
            fileIconLabel.setOpaque(false);
            add(fileIconLabel, BorderLayout.WEST);
        }

        ToolbarButton closeButton = new ToolbarButton("tabs.close", "Close Tab", 12, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int tab = editor.getTabByFile(sketchFile);
                try {
                    editor.closeTab(tab);
                } catch (Exception ex) {
                    Base.exception(ex);
                }
            }
        });

        closeButton.setContentAreaFilled(false);

        add(closeButton, BorderLayout.EAST);

        isSelected = true;
        update();
    }

    public void update() {
        if (sketchFile == null) return;
        Font labelFont = nameLabel.getFont();
        if (sketchFile != null) {
            name = sketchFile.getName();
        }
        nameLabel.setText(name);

        if (modified) {
            try {
                nameLabel.setIcon(IconManager.getIcon(12, "tabs.modified"));
            } catch (IOException ex) {
                Base.exception(ex);
            }
        } else {
            nameLabel.setIcon(null);
        }

        if (isSelected) {
//            nameLabel.setForeground(Base.getTheme().getColor("tab.selected.fgcolor"));
        } else {
//            nameLabel.setForeground(Base.getTheme().getColor("tab.deselected.fgcolor"));
        }
    }

    public boolean needsReload() {
        if (sketchFile == null) {
            return false;
        }
        return (sketchFile.lastModified() > expectedFileTime);
    }

    public void setReloaded() {
        expectedFileTime = sketchFile.lastModified();
    }

    public void askReload() {
        int n = editor.twoOptionBox(
                    JOptionPane.WARNING_MESSAGE,
                    Base.i18n.string("msg.reload.title"),
                    Base.i18n.string("msg.reload.body", sketchFile.getName()),
                    "Yes", "No");

        if(n == 0) {
            reloadFile();
        } else {
            setReloaded();
        }
    }

    public void reloadFile() {
        int myTabNumber = editor.editorTabs.indexOfTabComponent(this);
        EditorBase eb = editor.getTab(myTabNumber);
        eb.reloadFile();
        setReloaded();
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean m) {
        if(modified != m) {
            if(!m) {
                if (sketchFile != null) {
                    expectedFileTime = sketchFile.lastModified();
                }
            }

            modified = m;
            update();
        }
    }

    public void setFile(File f) {
        sketchFile = f;
        Debug.message("Set file to " + f.getAbsolutePath());
        expectedFileTime = sketchFile.lastModified();
        update();
    }

    public File getFile() {
        return sketchFile;
    }

    public void save() {
        int myTabNumber = editor.editorTabs.indexOfTabComponent(this);
        EditorBase eb = editor.getTab(myTabNumber);
        Debug.message("Saving tab " + name + " to " + sketchFile.getAbsolutePath());
        eb.saveTo(sketchFile);
        expectedFileTime = sketchFile.lastModified();
        update();
    }

    public void changeState(boolean state) {
        isSelected = state;
        if (isSelected) {
//            nameLabel.setForeground(Base.getTheme().getColor("tab.selected.fgcolor"));
        } else {
//            nameLabel.setForeground(Base.getTheme().getColor("tab.deselected.fgcolor"));
        }
    } 
}

