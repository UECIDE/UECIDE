package org.uecide.gui.swing;

import org.uecide.Context;

import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JLabel;
import javax.swing.JTree;

import java.awt.Font;

public class SketchTreeNode extends SketchTreeNodeBase {

    SketchSourceFilesNode sketchSourceFilesNode;
    SketchHeaderFilesNode sketchHeaderFilesNode;
    SketchTreeLibrariesNode sketchTreeLibrariesNode;
    SketchTreeOutputNode sketchTreeOutputNode;

    public SketchTreeNode(Context c, SketchTreeModel m) {
        super(c, m, c.getSketch().getName());
        sketchSourceFilesNode = new SketchSourceFilesNode(ctx, model);
        sketchHeaderFilesNode = new SketchHeaderFilesNode(ctx, model);
        sketchTreeLibrariesNode = new SketchTreeLibrariesNode(ctx, model);
        sketchTreeOutputNode = new SketchTreeOutputNode(ctx, model);

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
        if (modified) model.reload(this);
        return modified;
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
