package org.uecide.gui.swing;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import java.awt.Toolkit;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.uecide.Context;

public class EditMenu extends JMenu {
    
    Context ctx;

    JMenuItem copyMenu;
    JMenuItem cutMenu;
    JMenuItem pasteMenu;
    JMenuItem selectAllMenu;
    JMenuItem copyForForumBB;
    JMenuItem copyForForumMD;
    JMenuItem findMenu;
    JMenuItem undoMenu;
    JMenuItem redoMenu;

    public EditMenu(Context c) {
        super("Edit");
        ctx = c;

        int defaultModifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        undoMenu = new JMenuItem("Undo");
        undoMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, defaultModifiers));
        undoMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                TabPanel p = ((SwingGui)ctx.getGui()).getActiveTab();
                if (p instanceof CopyAndPaste) {
                    CopyAndPaste ce = (CopyAndPaste)p;
                    ce.undo();
                }
            }
        });
        add(undoMenu);

        redoMenu = new JMenuItem("Redo");
        redoMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, defaultModifiers));
        redoMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                TabPanel p = ((SwingGui)ctx.getGui()).getActiveTab();
                if (p instanceof CopyAndPaste) {
                    CopyAndPaste ce = (CopyAndPaste)p;
                    ce.redo();
                }
            }
        });
        add(redoMenu);

        addSeparator();

        copyMenu = new JMenuItem("Copy");
        copyMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, defaultModifiers));
        copyMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                TabPanel p = ((SwingGui)ctx.getGui()).getActiveTab();
                if (p instanceof CopyAndPaste) {
                    CopyAndPaste ce = (CopyAndPaste)p;
                    ce.copy();
                }
            }
        });
        add(copyMenu);

        cutMenu = new JMenuItem("Cut");
        cutMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, defaultModifiers));
        cutMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                TabPanel p = ((SwingGui)ctx.getGui()).getActiveTab();
                if (p instanceof CodeEditor) {
                    CopyAndPaste ce = (CopyAndPaste)p;
                    ce.cut();
                }
            }
        });
        add(cutMenu);

        pasteMenu = new JMenuItem("Paste");
        pasteMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, defaultModifiers));
        pasteMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                TabPanel p = ((SwingGui)ctx.getGui()).getActiveTab();
                if (p instanceof CodeEditor) {
                    CopyAndPaste ce = (CopyAndPaste)p;
                    ce.paste();
                }
            }
        });
        add(pasteMenu);
   
        addSeparator();

        selectAllMenu = new JMenuItem("Select All");
        selectAllMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, defaultModifiers));
        selectAllMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                TabPanel p = ((SwingGui)ctx.getGui()).getActiveTab();
                if (p instanceof CodeEditor) {
                    CopyAndPaste ce = (CopyAndPaste)p;
                    ce.selectAll();
                }
            }
        });
        add(selectAllMenu);
   
        copyForForumBB = new JMenuItem("Copy for forum (BBCode)");
        copyForForumBB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                TabPanel p = ((SwingGui)ctx.getGui()).getActiveTab();
                if (p instanceof CodeEditor) {
                    CopyAndPaste ce = (CopyAndPaste)p;
                    ce.copyAll("[code]", "[/code]");
                }
            }
        });
        add(copyForForumBB);
   
        copyForForumMD = new JMenuItem("Copy for forum (Markdown)");
        copyForForumMD.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                TabPanel p = ((SwingGui)ctx.getGui()).getActiveTab();
                if (p instanceof CodeEditor) {
                    CopyAndPaste ce = (CopyAndPaste)p;
                    ce.copyAll("```c++", "```");
                }
            }
        });
        add(copyForForumMD);

        addSeparator();
   
        findMenu = new JMenuItem("Find & Replace (TODO)");
        findMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, defaultModifiers));
        add(findMenu);

    
    }
}
