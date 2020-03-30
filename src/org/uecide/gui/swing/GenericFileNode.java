package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.Debug;
import org.uecide.Utils;
import org.uecide.FileType;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JLabel;
import javax.swing.JTree;

import java.nio.file.Files;
import java.io.File;
import java.io.IOException;

import java.util.Enumeration;

import java.awt.Font;
import java.awt.Component;


public class GenericFileNode extends SketchTreeNodeBase {
    File file;

    TabPanel viewer = null;

    public GenericFileNode(Context c, SketchTreeModel m, File f) {
        super(c, m, f.getName());
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
        if (file.isDirectory()) return;
        if (viewer == null) {
            createViewer();
            return;
        }
        AutoTab at = ((SwingGui)ctx.getGui()).getPanelByTab(viewer);
        if (at == null) {
            createViewer();
            return;
        }
        ((TextViewerPanel)viewer).setText(getFileContent());
        at.setSelectedComponent(viewer);
    }

    public void createViewer() {
        AutoTab centerPane = ((SwingGui)ctx.getGui()).getDefaultTab();
        viewer = new TextViewerPanel(ctx, centerPane, file.getName(), getFileContent());
        centerPane.add(viewer);
        centerPane.setSelectedComponent(viewer);
    }

    public Component getRenderComponent(JLabel original, JTree tree) {
        try {
            original.setIcon(getIcon(tree));
        } catch (Exception ex) {
            Debug.exception(ex);
        }
        return original;
    }

    String getFileContent() {
        try {
            return Utils.getFileAsString(file);
        } catch (Exception ex) {
            Debug.exception(ex);
            ctx.error(ex);
        }

        return "File is binary. Unable to display content.";
    }

}
