package org.uecide.gui.swing;

import org.uecide.Context;

import java.util.ArrayList;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.JPopupMenu;

import java.io.IOException;

import java.awt.Font;
import java.awt.Component;

public abstract class SketchTreeNodeBase extends DefaultMutableTreeNode {
    protected Context ctx;
    protected SketchTreeModel model;

    public abstract Component getRenderComponent(JLabel original, JTree tree);

    public SketchTreeNodeBase(Context c, SketchTreeModel m, String name) {
        super(name);
        ctx = c;
        model = m;
    }

  public TreePath getTreePath() {
        ArrayList<TreeNode> nodes = new ArrayList<TreeNode>();
        TreeNode treeNode = this;
        nodes.add(treeNode);
        treeNode = treeNode.getParent();
        while (treeNode != null) {
            nodes.add(0, treeNode);
            treeNode = treeNode.getParent();
        }

        return nodes.isEmpty() ? null : new TreePath(nodes.toArray());
    }

    public abstract ImageIcon getIcon(JTree tree) throws IOException;
    public abstract boolean updateChildren();

    public abstract JPopupMenu getPopupMenu();
    public abstract void performDoubleClick();
}
