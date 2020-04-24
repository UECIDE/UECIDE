package org.uecide.gui.swing;

import org.uecide.Context;

import java.awt.Component;
import java.awt.Color;
import java.awt.Font;

import java.io.IOException;

import javax.swing.UIManager;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.ImageIcon;
import javax.swing.tree.DefaultTreeCellRenderer;


public class SketchTreeCellRenderer extends DefaultTreeCellRenderer {

    Context ctx;
    
    public SketchTreeCellRenderer(Context c) {
        super();
        ctx = c;
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        JLabel ret = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

        ret.setFont(new JLabel().getFont());

        if (sel) {
            ret.setForeground(UIManager.getColor("Tree.selectionForeground"));
        } else {
            ret.setForeground(UIManager.getColor("Tree.foreground"));
        }

        if (value instanceof SketchTreeNodeBase) {
            SketchTreeNodeBase n = (SketchTreeNodeBase)value;
            Component ret1 = n.getRenderComponent(ret, tree);
            return ret1;

        }

        return ret;
    }

}
