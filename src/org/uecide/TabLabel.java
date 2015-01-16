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
import org.uecide.debug.*;
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
    ImageIcon fileIcon = null;
    JLabel nameLabel;
    File sketchFile;
    String name;
    long expectedFileTime;
    boolean modified = false;
    boolean fileWatchMutex = false;
    Editor editor;

    boolean isSelected = false;

    public TabLabel(Editor e, File sf) {
        editor = e;
        sketchFile = sf;
        name = sketchFile.getName();
        this.setLayout(new BorderLayout());
        nameLabel = new JLabel(name);
        fileIcon = Base.getIcon("mime",(FileType.getIcon(sketchFile)), 16);

        if(fileIcon != null) {
            nameLabel.setIcon(fileIcon);
        }

        JLabel blab = new JLabel();
        nameLabel.setOpaque(false);
        blab.setIcon(Base.getIcon("actions", "close", 16));

        blab.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                int tab = editor.getTabByFile(sketchFile);
                editor.closeTab(tab);
            }
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
        });

        blab.setOpaque(false);
        this.setOpaque(false);
        this.add(nameLabel, BorderLayout.CENTER);
        this.add(blab, BorderLayout.EAST);
        expectedFileTime = sf.lastModified();
        update();
        isSelected = true; // Just been made, so must be selected!
    }

    public TabLabel(Editor e, String tabname) {
        editor = e;
        sketchFile = null;
        name = tabname;
        this.setLayout(new BorderLayout());
        nameLabel = new JLabel(name);
        JLabel blab = new JLabel();
        nameLabel.setOpaque(false);
        blab.setIcon(Base.loadIconFromResource("tabs/close.png"));

        blab.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                int tab = editor.getTabByLabel(TabLabel.this);
                editor.closeTab(tab);
            }
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
        });

        blab.setOpaque(false);
        this.setOpaque(false);
        this.add(nameLabel, BorderLayout.CENTER);
        this.add(blab, BorderLayout.EAST);
        isSelected = true; // Just been made, so must be selected!
    }

    public void update() {
        Font labelFont = nameLabel.getFont();

        if(modified) {
            nameLabel.setFont(new Font(labelFont.getName(), Font.BOLD, labelFont.getSize()));
            nameLabel.setText(sketchFile.getName() + " * ");
        } else {
            nameLabel.setFont(new Font(labelFont.getName(), Font.PLAIN, labelFont.getSize()));
            nameLabel.setText(sketchFile.getName());
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
                    "Reload File?",
                    Translate.w("The file %1 has been modified outside UECIDE.  Do you want to reload it??", 40, "\n", sketchFile.getName()),
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
                expectedFileTime = sketchFile.lastModified();
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
        Debug.message("Savibg tab " + name + " to " + sketchFile.getAbsolutePath());
        eb.saveTo(sketchFile);
        expectedFileTime = sketchFile.lastModified();
        update();
    }

    public void changeState(boolean state) {
        isSelected = state;
    } 
}

