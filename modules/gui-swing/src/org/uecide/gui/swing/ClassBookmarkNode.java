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
import java.util.Enumeration;

import org.uecide.ContextEvent;
import org.uecide.ContextEventListener;

public class ClassBookmarkNode extends FunctionBookmarkNode implements ContextEventListener {
    public ClassBookmarkNode(Context c, SketchTreeModel m, FunctionBookmark bm) {
        super(c, m, bm);
        updateChildren();
    }

    @Override
    public ImageIcon getIcon(JTree tree) throws IOException {
        return IconManager.getIcon(12, "tree.class");
    }

    @Override
    public boolean updateChildren() {
        return false;
    }

    public boolean updateChildren(ArrayList<FunctionBookmark> bookmarks) {

        boolean somethingRemoved = false;
        boolean hasBeenModified = false;

        // First remove any obsolete nodes
        do {
            somethingRemoved = false;
            for (Enumeration e = children(); e.hasMoreElements();) {
                MemberBookmarkNode child = (MemberBookmarkNode)e.nextElement();

                FunctionBookmark bm = child.getBookmark();
                boolean found = false;
                for (FunctionBookmark newbm : bookmarks) {
                    if (newbm.equals(bm)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    remove(child);
                    somethingRemoved = true;
                    hasBeenModified = true;
                    break;
                }
            }
        } while (somethingRemoved);

        // Then add any new ones.
        for (FunctionBookmark newbm : bookmarks) {
            if (!newbm.isMemberFunction()) continue; 
            if (newbm.getParentClass().equals(bookmark.getName())) {
                boolean found = false;
                for (Enumeration e = children(); e.hasMoreElements();) {
                    FunctionBookmarkNode child = (FunctionBookmarkNode)e.nextElement();
                    FunctionBookmark bm = child.getBookmark();
                    if (newbm.equals(bm)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    add(new MemberBookmarkNode(ctx, model, newbm));
                    hasBeenModified = true;
                }
            }
        }

        if (hasBeenModified) model.reload(this);
        return hasBeenModified;
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
    public void contextEventTriggered(ContextEvent e) {
        if (e.getEvent().equals("functionBookmarksUpdated")) {
            updateChildren();
        }
    }
}
