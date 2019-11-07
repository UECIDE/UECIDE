package org.uecide.gui.swing;

import org.uecide.Base;
import org.uecide.Context;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuListener;
import javax.swing.event.MenuEvent;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;

import java.io.File;


public class ExampleFileMenu extends JMenu implements MenuListener {

    Context ctx;

    public ExampleFileMenu(Context c) {
        super("Examples");
        ctx = c;
        addMenuListener(this);
    }

    public void menuCanceled(MenuEvent e) {
    }

    public void menuDeselected(MenuEvent e) {
    }

    public void menuSelected(MenuEvent e) {
        removeAll();

        JMenuItem browser = new JMenuItem("Example Browser...");
        add(browser);
        addSeparator();

    }
    
}    
