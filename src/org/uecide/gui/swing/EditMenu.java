package org.uecide.gui.swing;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;

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

        copyMenu = new JMenuItem("Copy");
        copyMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, defaultModifiers));
        add(copyMenu);

        cutMenu = new JMenuItem("Cut");
        cutMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, defaultModifiers));
        add(cutMenu);

        pasteMenu = new JMenuItem("Paste");
        pasteMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, defaultModifiers));
        add(pasteMenu);
   
        addSeparator();

        selectAllMenu = new JMenuItem("Select All");
        selectAllMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, defaultModifiers));
        add(selectAllMenu);
   
        copyForForumBB = new JMenuItem("Copy for forum (BBCode)");
        add(copyForForumBB);
   
        copyForForumMD = new JMenuItem("Copy for forum (Markdown)");
        add(copyForForumMD);

        addSeparator();
   
        findMenu = new JMenuItem("Find & Replace");
        findMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, defaultModifiers));
        add(findMenu);

        undoMenu = new JMenuItem("Undo");
        undoMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, defaultModifiers));
        add(undoMenu);

        redoMenu = new JMenuItem("Redo");
        redoMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, defaultModifiers));
        add(redoMenu);
        
    
    }
}
