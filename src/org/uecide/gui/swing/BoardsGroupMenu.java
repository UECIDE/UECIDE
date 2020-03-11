package org.uecide.gui.swing;

import org.uecide.UECIDE;
import org.uecide.Context;
import org.uecide.Board;

import java.util.TreeSet;
import java.util.TreeMap;

import java.io.File;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuListener;
import javax.swing.event.MenuEvent;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class BoardsGroupMenu extends JMenu implements MenuListener {

    Context ctx;
    String group;

    public BoardsGroupMenu(Context c, String g) {
        super(g);
        ctx = c;
        group = g;
        addMenuListener(this);
    }

    TreeSet<String> getGroupList() {
        TreeSet<String> groups = new TreeSet<String>();
        for (Board board : UECIDE.boards.values()) {
            String group = board.getGroup();
            if (!groups.contains(group)) {
                groups.add(group);
            }
        }
        return groups;
    }

    public void menuCanceled(MenuEvent e) {
    }

    public void menuDeselected(MenuEvent e) {
    }

    public void menuSelected(MenuEvent e) {
        removeAll();
        for (Board board : UECIDE.boards.values()) {
            if (board.getGroup().equals(group)) {
                BoardsMenuItem bi = new BoardsMenuItem(ctx, board);
                add(bi);
            }
        }
    }
}
