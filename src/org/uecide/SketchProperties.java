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

import java.util.jar.*;
import java.util.zip.*;

public class SketchProperties extends JDialog {
    Sketch sketch;
    Editor editor;

    JPanel win;
    JPanel outer;
    JPanel buttonBar;

    JButton saveButton;
    JButton cancelButton;
    JCheckBox winPos;

    JTabbedPane tabs;
    JPanel overviewPane;
    JPanel objectsPane;

    HashMap<String, JComponent> fields;

    public SketchProperties(Editor e, Sketch s) {
        super();

        editor = e;
        sketch = s;

        fields = new HashMap<String, JComponent>();

        this.setPreferredSize(new Dimension(500, 400));
        this.setMinimumSize(new Dimension(500, 400));
        this.setMaximumSize(new Dimension(500, 400));
        this.setSize(new Dimension(500, 400));
        Point eLoc = editor.getLocation();
        int x = eLoc.x;
        int y = eLoc.y;

        Dimension eSize = editor.getSize();
        int w = eSize.width;
        int h = eSize.height;

        int cx = x + (w / 2);
        int cy = y + (h / 2);

        this.setLocation(new Point(cx - 250, cy - 200));
        this.setModal(true);

        outer = new JPanel(new BorderLayout());
        outer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        this.add(outer);

        win = new JPanel();
        win.setLayout(new BorderLayout());
        outer.add(win, BorderLayout.CENTER);

        buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));


        saveButton = new JButton("OK");
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                save();
                SketchProperties.this.dispose();
            }
        });

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SketchProperties.this.dispose();
            }
        });

        buttonBar.add(saveButton);
        buttonBar.add(cancelButton);

        win.add(buttonBar, BorderLayout.SOUTH);

        tabs = new JTabbedPane();

        overviewPane = new JPanel();
        overviewPane.setLayout(new BoxLayout(overviewPane, BoxLayout.PAGE_AXIS));
        overviewPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        addTextField(overviewPane, "sketch.author", "Sketch author:");
        addTextArea(overviewPane, "sketch.summary", "Summary:");
        addTextArea(overviewPane, "sketch.license", "Copyright / License:");
        winPos = new JCheckBox("Save Window Position");
        overviewPane.add(winPos);
        tabs.add("Overview", overviewPane);


        objectsPane = new JPanel();
        objectsPane.setLayout(new BoxLayout(objectsPane, BoxLayout.PAGE_AXIS));
        objectsPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        addTextField(objectsPane, "sketch.board", "Board:");
        addTextField(objectsPane, "sketch.core", "Core:");
        addTextField(objectsPane, "sketch.compiler", "Compiler:");
        addTextField(objectsPane, "sketch.port", "Serial port:");
        addTextField(objectsPane, "sketch.programmer", "Programmer:");
        JButton setDef = new JButton("Set to current IDE values");
        setDef.setMaximumSize(setDef.getPreferredSize());
        setDef.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setObjectValues();
            }
        });
        objectsPane.add(Box.createVerticalGlue());
        objectsPane.add(setDef);

        tabs.add("Objects", objectsPane);

        win.add(tabs, BorderLayout.CENTER);

        this.setTitle("Sketch Properties");

        this.pack();
        this.setVisible(true);
    }

    void addTextField(JPanel panel, String key, String label) {
        Context ctx = sketch.getContext();
        PropertyFile configFile = ctx.getSketchSettings();
        JLabel lab = new JLabel(label);
        lab.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(lab);
        JTextField field = new JTextField();
        String content = "";
        if (configFile.get(key) != null) {
            content = configFile.get(key);
        }
        field.setText(content);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, field.getPreferredSize().height));
        fields.put(key, field);
        panel.add(field);
    }

    void addTextArea(JPanel panel, String key, String label) {
        Context ctx = sketch.getContext();
        PropertyFile configFile = ctx.getSketchSettings();
        JLabel lab = new JLabel(label);
        lab.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(lab);
        JTextArea field = new JTextArea();
        String content = "";
        if (configFile.get(key) != null) {
            content = configFile.get(key);
        }
        field.setText(content);
        field.setLineWrap(true);
        field.setWrapStyleWord(true);
        fields.put(key, field);
        JScrollPane scroll = new JScrollPane(field);
        scroll.setAlignmentX(0.0f);
        panel.add(scroll);
    }

    public void setObjectValues() {
        String brd = "";
        String cre = "";
        String cmp = "";
        String dev = "";
        String prg = "";

        if (sketch.getBoard() != null) brd = sketch.getBoard().getName();
        if (sketch.getCore() != null) cre = sketch.getCore().getName();
        if (sketch.getCompiler() != null) cmp = sketch.getCompiler().getName();
        if (sketch.getDevice() != null) dev = sketch.getDevice().toString();
        if (sketch.getProgrammer() != null) prg = sketch.getProgrammer();
 
        ((JTextField)(fields.get("sketch.board"))).setText(brd);
        ((JTextField)(fields.get("sketch.core"))).setText(cre);
        ((JTextField)(fields.get("sketch.compiler"))).setText(cmp);
        ((JTextField)(fields.get("sketch.port"))).setText(dev);
        ((JTextField)(fields.get("sketch.programmer"))).setText(prg);
    }

    public void save() {
        Context ctx = sketch.getContext();
        PropertyFile cf = ctx.getSketchSettings();

        for(String key : fields.keySet()) {
            JComponent comp = fields.get(key);

            if(comp instanceof JTextField) {
                JTextField c = (JTextField)comp;

                if(c.getText().trim().equals("")) {
                    cf.unset(key);
                } else {
                    cf.set(key, c.getText());
                }
            } else if(comp instanceof JTextArea) {
                JTextArea c = (JTextArea)comp;

                if(c.getText().trim().equals("")) {
                    cf.unset(key);
                } else {
                    cf.set(key, c.getText());
                }
            }
        }

        if (winPos.isSelected()) {
            saveWinPos();
        }

        ctx.saveSketchSettings();
    }

    public void saveWinPos() {
        Editor e = sketch.getEditor();
        if (e == null) {
            return;
        }
        Point p = e.getLocation(null);
        Dimension d = e.getSize(null);
        Context ctx = sketch.getContext();
        PropertyFile cf = ctx.getSketchSettings();

        cf.setInteger("sketch.window.x", p.x);
        cf.setInteger("sketch.window.y", p.y);
        cf.setInteger("sketch.window.w", d.width);
        cf.setInteger("sketch.window.h", d.height);
    }
}
