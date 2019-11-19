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

public class FunctionBookmarkNode extends SketchTreeNodeBase implements ContextEventListener {
    protected FunctionBookmark bookmark;

    public FunctionBookmarkNode(Context c, SketchTreeModel m, FunctionBookmark bm) {
        super(c, m, bm.toString());
        bookmark = bm;
        updateChildren();
    }

    public ImageIcon getIcon(JTree tree) throws IOException {
        return IconManager.getIcon(12, "tree.function");
    }

    public boolean updateChildren() {
        return false;
    }

    public JPopupMenu getPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        return menu;
    }

    public void performDoubleClick() {
    }

    public void contextEventTriggered(String event, Context c) {
        updateChildren();
    }

    public FunctionBookmark getBookmark() {
        return bookmark;
    }

}
