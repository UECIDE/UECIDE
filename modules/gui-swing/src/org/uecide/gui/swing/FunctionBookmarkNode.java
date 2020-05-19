package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.Debug;
import org.uecide.SketchFile;
import org.uecide.FileType;
import org.uecide.FunctionBookmark;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import java.io.IOException;
import java.util.ArrayList;

import org.uecide.ContextEventListener;
import org.uecide.ContextEvent;

import java.awt.Font;
import java.awt.Component;

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
        SketchFile sf = bookmark.getFile();
        ctx.action("OpenSketchFile", sf, (Integer)bookmark.getLine());
    }

    public void contextEventTriggered(ContextEvent e) {
        if (e.getEvent().equals("functionBookmarksUpdated")) {
            updateChildren();
        }
    }

    public FunctionBookmark getBookmark() {
        return bookmark;
    }

    public Component getRenderComponent(JLabel original, JTree tree) {
        Font f = original.getFont();
        original.setFont(f.deriveFont(Font.PLAIN, f.getSize() * 0.8f));
        try {
            original.setIcon(getIcon(tree));
        } catch (IOException ex) {
            Debug.exception(ex);
        }
        return original;
    }

}
