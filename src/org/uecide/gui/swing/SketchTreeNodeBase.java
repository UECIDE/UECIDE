package org.uecide.gui.swing;

import org.uecide.Context;

import java.util.ArrayList;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.JPopupMenu;

import java.io.IOException;


public abstract class SketchTreeNodeBase extends DefaultMutableTreeNode {
    protected Context ctx;

    public SketchTreeNodeBase(Context c, String name) {
        super(name);
        ctx = c;
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
