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
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.print.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import java.util.regex.*;
import java.text.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.border.*;
import javax.swing.JToolBar;
import java.nio.charset.*;

import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.*;

import java.lang.reflect.Method;

import org.uecide.debug.*;

public class Debug {

    public static StringBuilder debugText = new StringBuilder();
    public static JFrame win;
    public static RSyntaxTextArea textArea;
    public static boolean shown = false;
    public static RTextScrollPane scrollPane;
    public static JToolBar toolbar;
    public static boolean pause = false;
    public static JToggleButton pauseButton;
    public static boolean verbose = false;

    public static void setVerbose(boolean b) {
        verbose = b;
    }

    public static void show() {
        if(shown) {
            return;
        }

        win = new JFrame("Debug Console");

        Container contentPane = win.getContentPane();
        contentPane.setLayout(new BorderLayout());

        textArea = new RSyntaxTextArea();
        textArea.setText(debugText.toString());
//        textArea.setEnabled(false);
        textArea.setAntiAliasingEnabled(true);
        textArea.setMarkOccurrences(true);


        scrollPane = new RTextScrollPane(textArea);

        toolbar = new JToolBar();

        ImageIcon trashIcon = Base.loadIconFromResource("toolbar/trash.png");
        JButton trashButton = new JButton(trashIcon);
        trashButton.setToolTipText(Translate.t("Clear Debug Log"));
        trashButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Debug.debugText = new StringBuilder();
                textArea.setText("");
            }
        });
        toolbar.add(trashButton);

        ImageIcon pauseIcon = Base.loadIconFromResource("toolbar/pause.png");
        pauseButton = new JToggleButton(pauseIcon);
        pauseButton.setToolTipText(Translate.t("Pause Scrolling"));
        toolbar.add(pauseButton);

        contentPane.add(toolbar, BorderLayout.NORTH);
        contentPane.add(scrollPane, BorderLayout.CENTER);

        Base.setIcon(win);
        win.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                handleClose();
            }
        });
        win.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        win.pack();
        win.setMinimumSize(new Dimension(100, 100));

        if(Base.preferences != null) {
            win.setSize(new Dimension(
                            Base.preferences.getInteger("debug.window.width"),
                            Base.preferences.getInteger("debug.window.height")
                        ));
            win.setLocation(new Point(
                                Base.preferences.getInteger("debug.window.x"),
                                Base.preferences.getInteger("debug.window.y")
                            ));
        } else {
            win.setSize(new Dimension(400, 400));
            win.setLocation(new Point(0, 0));
        }

        win.setVisible(true);
        shown = true;
        win.addComponentListener(new ComponentListener() {
            public void componentMoved(ComponentEvent e) {
                Point windowPos = e.getComponent().getLocation(null);
                Base.preferences.setInteger("debug.window.x", windowPos.x);
                Base.preferences.setInteger("debug.window.y", windowPos.y);
                Base.preferences.saveDelay();
            }
            public void componentResized(ComponentEvent e) {
                Dimension windowSize = e.getComponent().getSize(null);
                Base.preferences.setInteger("debug.window.width", windowSize.width);
                Base.preferences.setInteger("debug.window.height", windowSize.height);
                Base.preferences.saveDelay();
            }
            public void componentHidden(ComponentEvent e) {
            }
            public void componentShown(ComponentEvent e) {
            }
        });

    }

    public static void hide() {
        if(!shown) {
            return;
        }

        handleClose();
    }

    public static void handleClose() {
        shown = false;

        if(Base.preferences != null) {
            Dimension d = win.getSize();
            Base.preferences.setInteger("debug.window.width", d.width);
            Base.preferences.setInteger("debug.window.height", d.height);
            Point p = win.getLocation();
            Base.preferences.setInteger("debug.window.x", p.x);
            Base.preferences.setInteger("debug.window.y", p.y);
            Base.preferences.save();
        }

        win.dispose();
    }

    public static void message(String s) {

        Thread t = Thread.currentThread();
        StackTraceElement[] st = t.getStackTrace();
        StackTraceElement caller = st[2];
        String tag = caller.getFileName() + " " + caller.getLineNumber() + " (" + caller.getMethodName() + "): ";

        debugText.append(tag);

        if (s == null) {
            debugText.append("[null]\n");
            return;
        }

        debugText.append(s);

        if(verbose) System.out.print(tag + s);

        if(!s.endsWith("\n")) {
            debugText.append("\n");

            if(verbose) System.out.print("\n");
        }

        if(shown) {
            textArea.append(tag);
            textArea.append(s);

            if(!s.endsWith("\n")) {
                textArea.append("\n");
            }

            if(!pauseButton.isSelected()) {
                try {
                    textArea.setCaretPosition(debugText.length());
                } catch(Exception e) {
                }
            }
        }
    }

    public static void setSize(Dimension d) {
        if(shown) {
            win.setSize(d);
        }
    }

    public static void setLocation(Point p) {
        if(shown) {
            win.setLocation(p);
        }
    }

}
