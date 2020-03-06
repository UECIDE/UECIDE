package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.ContextEvent;
import org.uecide.ContextEventListener;
import org.uecide.FileType;
import org.uecide.SketchFile;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JLabel;
import javax.swing.JTree;

import java.io.File;
import java.io.IOException;

import java.awt.Font;
import java.awt.Component;

import java.util.Enumeration;

public class SketchTreeOutputNode extends SketchTreeNodeBase implements ContextEventListener {
    public SketchTreeOutputNode(Context c, SketchTreeModel m) {
        super(c, m, "Output");
        updateChildren();
        ctx.listenForEvent("buildFileUpdated", this);
        ctx.listenForEvent("buildFileRemoved", this);
        ctx.listenForEvent("buildFileAdded", this);
    }

    public boolean updateChildren() {
        File file = ctx.getBuildDir();

        boolean somethingRemoved = false;
        boolean hasBeenModified = false;

        if (file.isDirectory()) {
            File[] files = file.listFiles();

            do {
                somethingRemoved = false;
                for (Enumeration e = children(); e.hasMoreElements();) {
                    GenericFileNode child = (GenericFileNode)e.nextElement();
                    if (!inFileArray(child.getFile(), files)) {
                        remove(child);
                        somethingRemoved = true;
                        hasBeenModified = true;
                        break;
                    }
                }
            } while (somethingRemoved);

            for (File f : files) {
                if (!hasChildFile(f)) {
                    GenericFileNode sfn = new GenericFileNode(ctx, model, f);
                    add(sfn);
                    hasBeenModified = true;
                }
            }
            for (Enumeration e = children(); e.hasMoreElements();) {
                GenericFileNode child = (GenericFileNode)e.nextElement();
                if (child.updateChildren()) {
                    hasBeenModified = true;
                }
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
        }

        return original;
    }

    public void contextEventTriggered(ContextEvent evt) {
        if (updateChildren()) {
            model.reload(this);
        }
    }


    boolean inFileArray(File needle, File[] haystack) {
        for (File f : haystack) {
            if (f == needle) return true;
        }
        return false;
    }

    boolean hasChildFile(File f) {
        for (Enumeration e = children(); e.hasMoreElements();) {
            GenericFileNode child = (GenericFileNode)e.nextElement();
            if (child.getFile() == f) return true;
        }
        return false;
    }


}
