package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.SketchFile;
import org.uecide.FileType;
import org.uecide.FunctionBookmark;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

import org.uecide.ContextEvent;
import org.uecide.ContextEventListener;

import java.awt.Font;

public class SketchSourceFileNode extends SketchTreeNodeBase implements ContextEventListener {
    SketchFile sketchFile;

    public SketchSourceFileNode(Context c, SketchTreeModel m, SketchFile sf) {
        super(c, m, sf.getFile().getName());
        sketchFile = sf;
        updateChildren();
        ctx.listenForEvent("sketchDataModified", this);
    }

    public SketchFile getSketchFile() {
        return sketchFile;
    }

    public ImageIcon getIcon(JTree tree) throws IOException {
        return IconManager.getIcon(16, "mime." + FileType.getIcon(getSketchFile().getFile()));
    }

    public boolean updateChildren() {

        ArrayList<FunctionBookmark> bookmarks = null;
        try {
            bookmarks = sketchFile.scanForFunctions();
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }

        boolean somethingRemoved = false;
        boolean hasBeenModified = false;

        // First remove any obsolete nodes
        do {
            somethingRemoved = false;
            for (Enumeration e = children(); e.hasMoreElements();) {
                FunctionBookmarkNode child = (FunctionBookmarkNode)e.nextElement();

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

        // Now add any new nodes
        for (FunctionBookmark bookmark : bookmarks) {
            boolean found = false;
            for (Enumeration e = children(); e.hasMoreElements();) {
                FunctionBookmarkNode child = (FunctionBookmarkNode)e.nextElement();
                FunctionBookmark bm = child.getBookmark();
                if (bookmark.equals(bm)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                if (bookmark.isClass()) {
                    add(new ClassBookmarkNode(ctx, model, bookmark));
                    hasBeenModified = true;
                } else if (bookmark.isFunction()) {
                    add(new FunctionBookmarkNode(ctx, model, bookmark));
                    hasBeenModified = true;
                }
            }
        }
    
        if (hasBeenModified) model.reload(this);

        // Finally, hand off the member functions to the class nodes
        for (Enumeration e = children(); e.hasMoreElements();) {
            FunctionBookmarkNode child = (FunctionBookmarkNode)e.nextElement();
            if (child instanceof ClassBookmarkNode) {
                ClassBookmarkNode cbn = (ClassBookmarkNode)child;
                if (cbn.updateChildren(bookmarks)) {
                    hasBeenModified = true;
                }
            }
        }

        return hasBeenModified;
    }

    public JPopupMenu getPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        if (!sketchFile.isMainFile()) {
            menu.add(new DeleteFileMenuItem(ctx, sketchFile.getFile()));
            menu.add(new RenameFileMenuItem(ctx, sketchFile.getFile()));
        }
        return menu;
    }

    public void performDoubleClick() {
        ctx.action("openSketchFile", sketchFile);
    }

    public void contextEventTriggered(ContextEvent e) {
        if (e.getEvent().equals("sketchDataModified")) {
            SketchFile sf = (SketchFile)e.getObject();
            if (sketchFile == sf) {
                updateChildren();
            }
            model.reload(this);
        }
    }

    public Font getFont() {
        Font f = new JLabel().getFont();
        if (sketchFile.isModified()) {
            f = f.deriveFont(Font.BOLD);
        } else {
            f = f.deriveFont(Font.PLAIN);
        }
        return f;
    }

}
