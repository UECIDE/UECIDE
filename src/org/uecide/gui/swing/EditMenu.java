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

        copyMenu = new JMenuItem("Copy (TODO)");
        copyMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, defaultModifiers));
        add(copyMenu);

        cutMenu = new JMenuItem("Cut (TODO)");
        cutMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, defaultModifiers));
        add(cutMenu);

        pasteMenu = new JMenuItem("Paste (TODO)");
        pasteMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, defaultModifiers));
        add(pasteMenu);
   
        addSeparator();

        selectAllMenu = new JMenuItem("Select All (TODO)");
        selectAllMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, defaultModifiers));
        add(selectAllMenu);
   
        copyForForumBB = new JMenuItem("Copy for forum (BBCode) (TODO)");
        add(copyForForumBB);
   
        copyForForumMD = new JMenuItem("Copy for forum (Markdown) (TODO)");
        add(copyForForumMD);

        addSeparator();
   
        findMenu = new JMenuItem("Find & Replace (TODO)");
        findMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, defaultModifiers));
        add(findMenu);

        undoMenu = new JMenuItem("Undo (TODO)");
        undoMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, defaultModifiers));
        add(undoMenu);

        redoMenu = new JMenuItem("Redo (TODO)");
        redoMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, defaultModifiers));
        add(redoMenu);
        
    
    }
}
