package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.Sketch;

import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class SketchTree extends JTree implements MouseListener {
    Context ctx;
    SketchTreeModel model;

    public SketchTree(Context c) {
        super();
        ctx = c;
        model = new SketchTreeModel(ctx);
        setModel(model);
        setCellRenderer(new SketchTreeCellRenderer(ctx));
        addMouseListener(this);
    }

    public void mousePressed(MouseEvent evt) {

        int selRow = getRowForLocation(evt.getX(), evt.getY());
        TreePath selPath = getPathForLocation(evt.getX(), evt.getY());
        setSelectionPath(selPath);

        if(selPath == null) {
            return;
        }

        if (evt.getButton() == 1) {
            if (evt.getClickCount() == 2) {
                SketchTreeNodeBase node = (SketchTreeNodeBase)selPath.getLastPathComponent();
                node.performDoubleClick();
            }
        } else if (evt.getButton() == 3) {

            SketchTreeNodeBase node = (SketchTreeNodeBase)selPath.getLastPathComponent();
            JPopupMenu menu = node.getPopupMenu();
            JMenuItem refresh = new JMenuItem("Refresh");
            refresh.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if (node.updateChildren()) {
                        model.reload(node);
                    }
                }
            });
            menu.add(refresh);
            if (menu != null) {
                menu.show(this, evt.getX(), evt.getY());
            }
        }
    }

    public void mouseReleased(MouseEvent evt) {
    }

    public void mouseEntered(MouseEvent evt) {
    }

    public void mouseExited(MouseEvent evt) {
    }

    public void mouseClicked(MouseEvent evt) {
    }
}

