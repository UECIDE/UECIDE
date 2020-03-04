package org.uecide.gui.swing;

import org.uecide.Context;

import java.awt.Component;
import java.awt.Font;

import java.io.IOException;

import javax.swing.JLabel;
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

        if (value instanceof SketchTreeNodeBase) {
            SketchTreeNodeBase n = (SketchTreeNodeBase)value;
            return n.getRenderComponent(ret, tree);
//            ret.setFont(n.getFont());
//            try {
//                ImageIcon i = n.getIcon(tree);
//                if (i != null) {
//                    ret.setIcon(i);
//                }
//            } catch (IOException ignored) {
//            }
        }

        return ret;
    }

}
