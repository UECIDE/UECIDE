package org.uecide.gui.swing;

import org.uecide.Context;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JLabel;
import javax.swing.JTree;

import java.io.IOException;

import java.awt.Font;

public class SketchTreeLibrariesNode extends SketchTreeNodeBase {
    public SketchTreeLibrariesNode(Context c, SketchTreeModel m) {
        super(c, m, "Libraries");
        updateChildren();
    }

    public boolean updateChildren() {
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

    public Font getFont() {
        return new JLabel().getFont();
    }

}
