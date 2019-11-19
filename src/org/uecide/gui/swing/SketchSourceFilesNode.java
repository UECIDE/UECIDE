package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.FileType;
import org.uecide.SketchFile;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;

import java.io.IOException;

import java.util.TreeMap;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;

public class SketchSourceFilesNode extends SketchTreeNodeBase {
    public SketchSourceFilesNode(Context c, SketchTreeModel m) {
        super(c, m, "Source");
        updateChildren();
    }

    public boolean updateChildren() {
        boolean somethingRemoved = false;
        boolean hasBeenModified = false;

        TreeMap<String, SketchFile> sketchFiles = ctx.getSketch().getSketchFiles();

        do {
            somethingRemoved = false;
            for (Enumeration e = children(); e.hasMoreElements();) {
                SketchSourceFileNode child = (SketchSourceFileNode)e.nextElement();
                if (!(sketchFiles.containsValue(child.getSketchFile()))) {
                    remove(child);
                    somethingRemoved = true;
                    hasBeenModified = true;
                    break;
                }
            }
        } while (somethingRemoved);

        for (SketchFile f : ctx.getSketch().getSketchFiles().values()) {
            if (f.getGroup() == FileType.GROUP_SOURCE) {
                if (!hasChildFile(f)) {
                    SketchSourceFileNode sfn = new SketchSourceFileNode(ctx, model, f);
                    add(sfn);
                    hasBeenModified = true;
                }
            }
        }

        if (hasBeenModified) model.reload(this);

        for (Enumeration e = children(); e.hasMoreElements();) {
            SketchSourceFileNode child = (SketchSourceFileNode)e.nextElement();
            if (child.updateChildren()) {
                hasBeenModified = true;
                break;
            }
        }

        return hasBeenModified;
    }

    boolean hasChildFile(SketchFile f) {
        for (Enumeration e = children(); e.hasMoreElements();) {
            SketchSourceFileNode child = (SketchSourceFileNode)e.nextElement();
            if (child.getSketchFile() == f) return true;
        }
        return false;
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

}
