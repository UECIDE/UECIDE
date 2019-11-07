package org.uecide.gui.swing;

import org.uecide.Context;

import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;

public class SketchTreeNode extends SketchTreeNodeBase {

    SketchSourceFilesNode sketchSourceFilesNode;
    SketchHeaderFilesNode sketchHeaderFilesNode;
    SketchTreeLibrariesNode sketchTreeLibrariesNode;
    SketchTreeOutputNode sketchTreeOutputNode;

    public SketchTreeNode(Context c) {
        super(c, c.getSketch().getName());
        sketchSourceFilesNode = new SketchSourceFilesNode(c);
        sketchHeaderFilesNode = new SketchHeaderFilesNode(c);
        sketchTreeLibrariesNode = new SketchTreeLibrariesNode(c);
        sketchTreeOutputNode = new SketchTreeOutputNode(c);

        add(sketchSourceFilesNode);
        add(sketchHeaderFilesNode);
        add(sketchTreeLibrariesNode);
        add(sketchTreeOutputNode);
    }

    public ImageIcon getIcon(JTree tree) throws IOException {
        return IconManager.getIcon(16, "apps.uecide");
    }

    public boolean updateChildren() {
        boolean modified = false;
        if(sketchSourceFilesNode.updateChildren()) modified = true;
        if(sketchHeaderFilesNode.updateChildren()) modified = true;
        if(sketchTreeLibrariesNode.updateChildren()) modified = true;
        if(sketchTreeOutputNode.updateChildren()) modified = true;
        return modified;
    }

    public JPopupMenu getPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        return menu;
    }

    public void performDoubleClick() {
    }
}
