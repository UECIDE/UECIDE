package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.Library;
import org.uecide.SketchFile;
import org.uecide.FileType;
import org.uecide.FunctionBookmark;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

import org.uecide.ContextEvent;
import org.uecide.ContextEventListener;

import java.awt.Font;
import java.awt.Component;
import java.awt.BorderLayout;

public class SketchLibraryNode extends SketchTreeNodeBase implements ContextEventListener {
    Library library;

    public SketchLibraryNode(Context c, SketchTreeModel m, Library l) {
        super(c, m, l.getName());
        library = l;
        updateChildren();
    }

    public Library getLibrary() {
        return library;
    }

    public ImageIcon getIcon(JTree tree) throws IOException {
        return IconManager.getIcon(16, "library");
    }

    public boolean updateChildren() {
        return false;
    }

    public JPopupMenu getPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        return menu;
    }

    public void performDoubleClick() {
    }

    public void contextEventTriggered(ContextEvent e) {
    }

    public Component getRenderComponent(JLabel original, JTree tree) {
        return original;
    }
}
