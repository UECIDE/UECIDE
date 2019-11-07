package org.uecide.gui.swing;

import org.uecide.Context;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;

public class SketchTreeModel extends DefaultTreeModel {
    Context ctx;

    SketchTreeNode rootNode;

    public SketchTreeModel(Context c) {
        super(new DefaultMutableTreeNode("dummy"));
        rootNode = new SketchTreeNode(c);
        ctx = c;
        setRoot(rootNode);
    }

    public boolean updateChildren() {
        return rootNode.updateChildren();
    }
    
}
