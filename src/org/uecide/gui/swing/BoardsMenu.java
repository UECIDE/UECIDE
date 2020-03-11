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

public class BoardsMenu extends JMenu implements MenuListener {

    Context ctx;

    public BoardsMenu(Context c) {
        super("Board (None Selected)");
        ctx = c;
        addMenuListener(this);

        updateBoard();
    }

    public void updateBoard() {
        Board board = ctx.getBoard();
        if (board == null) {
            setText("Board (None Selected)");
        } else {
            setText("Board (" + board.getDescription() + ")");
            try {
                setIcon(new CleverIcon(16, board.getIcon()));
            } catch (Exception ex) {
            }
        }
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
        TreeSet<String> groups = getGroupList();
        for (String group : groups) {
            BoardsGroupMenu menu = new BoardsGroupMenu(ctx, group);
            add(menu);
        }
    }
    
}
