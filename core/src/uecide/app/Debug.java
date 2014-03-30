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

    public static void show() {
        if (shown) {
            return;
        }
        win = new JFrame("Debug Console");
        textArea = new RSyntaxTextArea();
        textArea.setText(debugText.toString());
        textArea.setEnabled(false);

        scrollPane = new RTextScrollPane(textArea);
        win.add(scrollPane);

        Base.setIcon(win);
        win.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                handleClose();
            }
        });
        win.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        win.pack();
        win.setMinimumSize(new Dimension(400, 400));
        win.setVisible(true);
        shown = true;
        textArea.setCaretPosition(debugText.length());
    }

    public static void hide() {
        if (!shown) {
            return;
        }
        handleClose();
    }

    public static void handleClose() {
        shown = false;
        win.dispose();
    }

    public static void message(String s) {
        debugText.append(s + "\n");
        if (shown) {
            textArea.append(s + "\n");
            textArea.setCaretPosition(debugText.length());
        }
    }
}
