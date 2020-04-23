package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.Sketch;
import org.uecide.Library;
import org.uecide.ContextEvent;
import org.uecide.ContextEventListener;
import org.uecide.Debug;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JLabel;
import javax.swing.JTree;

import java.io.IOException;

import java.awt.Font;
import java.awt.Component;

import java.util.Enumeration;
import java.util.ArrayList;

public class SketchTreeLibrariesNode extends SketchTreeNodeBase implements ContextEventListener {
    public SketchTreeLibrariesNode(Context c, SketchTreeModel m) {
        super(c, m, "Libraries");
        updateChildren();
        ctx.listenForEvent("sketchLibraryListUpdated", this);
    }

    public boolean updateChildren() {
        Sketch sketch = ctx.getSketch();

        ArrayList<Library> libraries = sketch.getLibraries();

        boolean somethingRemoved = false;
        boolean hasBeenModified = false;
        do {
            somethingRemoved = false;
            for (Enumeration e = children(); e.hasMoreElements();) {
                SketchLibraryNode child = (SketchLibraryNode)e.nextElement();
                if (!(libraries.contains(child.getLibrary()))) {
                    remove(child);
                    somethingRemoved = true;
                    hasBeenModified = true;
                    break;
                }
            }
        } while (somethingRemoved);

        for (Library l : libraries) {
            if (!hasChildLibrary(l)) {
                SketchLibraryNode sfn = new SketchLibraryNode(ctx, model, l);
                add(sfn);
                hasBeenModified = true;
            }
        }
        
        return hasBeenModified;
    }

    public ImageIcon getIcon(JTree tree) throws IOException {
        if (tree.isExpanded(getTreePath())) {
            return IconManager.getIcon(16, "tree.folder-open");
        } else {
            return IconManager.getIcon(16, "tree.folder-closed");
        }
    }

    public JPopupMenu getPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        return menu;
    }
    public void performDoubleClick() {
    }

    public Component getRenderComponent(JLabel original, JTree tree) {
        try {
            original.setIcon(getIcon(tree));
        } catch (Exception ex) {
            Debug.exception(ex);
        }

        return original;
    }

    boolean hasChildLibrary(Library l) {
        for (Enumeration e = children(); e.hasMoreElements();) {
            SketchLibraryNode child = (SketchLibraryNode)e.nextElement();
            if (child.getLibrary() == l) return true;
        }
        return false;
    }

    public void contextEventTriggered(ContextEvent event) {
        if (event.getEvent().equals("sketchLibraryListUpdated")) {
            if (updateChildren()) {
                model.reload(this);
            }
        }
    }

}
