package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.FileType;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;

import java.io.File;
import java.io.IOException;

import java.util.Enumeration;


public class GenericFileNode extends SketchTreeNodeBase {
    File file;

    public GenericFileNode(Context c, File f) {
        super(c, f.getName());
        file = f;
    }

    public File getFile() {
        return file;
    }

    public ImageIcon getIcon(JTree tree) throws IOException {
        if (file.isDirectory()) {
            if (tree.isExpanded(getTreePath())) {
                return IconManager.getIcon(16, "tree.folder-open");
            } else {
                return IconManager.getIcon(16, "tree.folder-closed");
            }
        }
        return IconManager.getIcon(16, "mime." + FileType.getIcon(file));
    }

    boolean inFileArray(File needle, File[] haystack) {
        for (File f : haystack) {
            if (f == needle) return true;
        }
        return false;
    }

    public boolean updateChildren() {
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
                    GenericFileNode sfn = new GenericFileNode(ctx, f);
                    add(sfn);
                    hasBeenModified = true;
                }
            }
        }
        return hasBeenModified;
    }

    boolean hasChildFile(File f) {
        for (Enumeration e = children(); e.hasMoreElements();) {
            GenericFileNode child = (GenericFileNode)e.nextElement();
            if (child.getFile() == f) return true;
        }
        return false;
    }

    public JPopupMenu getPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(new DeleteFileMenuItem(ctx, file));
        menu.add(new RenameFileMenuItem(ctx, file));
        return menu;
    }

    public void performDoubleClick() {
    }
}
