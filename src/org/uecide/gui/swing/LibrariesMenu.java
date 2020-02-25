package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.Library;
import org.uecide.Core;

import java.util.TreeSet;
import java.util.TreeMap;

import java.io.File;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuListener;
import javax.swing.event.MenuEvent;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LibrariesMenu extends JMenu implements MenuListener {

    Context ctx;

    public LibrariesMenu(Context c) {
        super("Libraries");
        ctx = c;
        addMenuListener(this);
    }

    public void menuCanceled(MenuEvent e) {
    }

    public void menuDeselected(MenuEvent e) {
    }

    public void menuSelected(MenuEvent e) {
        removeAll();

        add(new JMenuItem("Install library archive... (placeholder)"));
        addSeparator();

        Core core = ctx.getCore();

        if (core == null) {
            JMenuItem item = new JMenuItem("No core selected");
            item.setEnabled(false);
            add(item);
            return;
        }

        TreeMap<String, TreeSet<Library>> libs = Library.getFilteredLibraries(core.getName());

        for(String cat : libs.keySet()) {
            LibraryCategoryMenu item = new LibraryCategoryMenu(ctx, cat);
            add(item);
        }
    }
    
}
