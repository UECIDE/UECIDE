package org.uecide.gui.swing;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import javax.swing.event.MenuListener;
import javax.swing.event.MenuEvent;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;

import org.uecide.Context;
import org.uecide.FileType;

public class HardwareMenu extends JMenu implements MenuListener {
    
    Context ctx;

    BoardsMenu boardsMenu;
    CoresMenu coresMenu;
    CompilersMenu compilersMenu;
    ProgrammersMenu programmersMenu;
    PortsMenu portsMenu;

    OptionsMenu optionsMenu;

    public HardwareMenu(Context c) {
        super("Hardware");
        ctx = c;

        boardsMenu = new BoardsMenu(ctx);
        add(boardsMenu);

        coresMenu = new CoresMenu(ctx);
        add(coresMenu);

        compilersMenu = new CompilersMenu(ctx);
        add(compilersMenu);

        programmersMenu = new ProgrammersMenu(ctx);
        add(programmersMenu);

        portsMenu = new PortsMenu(ctx);
        add(portsMenu);

        addSeparator();

        optionsMenu = new OptionsMenu(ctx);
        add(optionsMenu);

        addMenuListener(this);
    }

    public void menuCanceled(MenuEvent e) {
    }

    public void menuDeselected(MenuEvent e) {
    }

    public void menuSelected(MenuEvent e) {
        boardsMenu.updateBoard();
        coresMenu.updateCore();
        compilersMenu.updateCompiler();
        programmersMenu.updateProgrammer();
        portsMenu.updatePort();
    }
}
