package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.SketchFile;
import org.uecide.FileType;
import org.uecide.FunctionBookmark;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import java.io.IOException;
import java.util.ArrayList;

import org.uecide.ContextEventListener;

public class MemberBookmarkNode extends FunctionBookmarkNode implements ContextEventListener {

    public MemberBookmarkNode(Context c, SketchTreeModel m, FunctionBookmark bm) {
        super(c, m, bm);
        updateChildren();
    }

    @Override
    public JPopupMenu getPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        return menu;
    }

    @Override
    public void performDoubleClick() {
    }

    @Override
    public void contextEventTriggered(String event, Context c) {
        updateChildren();
    }

    @Override
    public FunctionBookmark getBookmark() {
        return bookmark;
    }
}
