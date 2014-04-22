package uecide.app;

import uecide.plugin.*;

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

import uecide.app.debug.*;

public class Debug {

    public static StringBuilder debugText = new StringBuilder();
    public static JFrame win;
    public static RSyntaxTextArea textArea;
    public static boolean shown = false;
    public static RTextScrollPane scrollPane;
    public static JToolBar toolbar;
    public static boolean pause = false;
    public static JToggleButton pauseButton;

    public static void show() {
        if (shown) {
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

        File themeFolder = Base.getContentFile("lib/theme");
        if (!themeFolder.exists()) {
            System.err.println("PANIC: Theme folder doesn't exist! " + themeFolder.getAbsolutePath());
            return;
        }


        File trashIconFile = new File(themeFolder, "trash.png");
        ImageIcon trashIcon = new ImageIcon(trashIconFile.getAbsolutePath());
        JButton trashButton = new JButton(trashIcon);
        trashButton.setToolTipText(Translate.t("Clear Debug Log"));
        trashButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                Debug.debugText = new StringBuilder();
                textArea.setText("");
            }
        });
        toolbar.add(trashButton);

        File pauseIconFile = new File(themeFolder, "pause.png");
        ImageIcon pauseIcon = new ImageIcon(pauseIconFile.getAbsolutePath());
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
        if (Base.preferences != null) {
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
    }

    public static void hide() {
        if (!shown) {
            return;
        }
        handleClose();
    }

    public static void handleClose() {
        shown = false;
        if (Base.preferences != null) {
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
        debugText.append(s);
        if (!s.endsWith("\n")) {
            debugText.append("\n");
        }
        if (shown) {
            textArea.append(s);
            if (!s.endsWith("\n")) {
                textArea.append("\n");
            }
            if (!pauseButton.isSelected()) {
                try {
                    textArea.setCaretPosition(debugText.length());
                } catch (Exception e) {
                }
            }
        }
    }
}
