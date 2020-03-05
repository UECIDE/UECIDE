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

public class SketchLibraryNode extends SketchTreeNodeBase implements ContextEventListener, AnimationListener {
    Library library;

    CleverIcon goodIcon = null;
    CleverIcon badIcon = null;
    CleverIcon emptyIcon = null;
    CleverIcon compilingIcon = null;

    public SketchLibraryNode(Context c, SketchTreeModel m, Library l) {
        super(c, m, l.getName());
        library = l;
        updateChildren();
        ctx.listenForEvent("libraryCompileStarted", this);
        ctx.listenForEvent("libraryCompileFinished", this);
        ctx.listenForEvent("libraryCompileFailed", this);

        try {
            goodIcon = IconManager.getIcon(16, "tree.lib-good");
            badIcon = IconManager.getIcon(16, "tree.lib-bad");
            emptyIcon = IconManager.getIcon(16, "tree.lib-bad");
            compilingIcon = IconManager.getIcon(16, "main.spin");
            compilingIcon.addAnimationListener(this);
        } catch (Exception ex) {
        }
    }

    public Library getLibrary() {
        return library;
    }

    public ImageIcon getIcon(JTree tree) throws IOException {
        if (library.isCompiled()) {
            return goodIcon;
        }

        if (library.isCompiling()) {
            return compilingIcon;
        }

        if (library.compilingFailed()) {
            return badIcon;
        }

        return emptyIcon;
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
        if (e.getObject() instanceof Library) {
            Library lib = (Library)e.getObject();
            if (lib == library) {
                model.reload(this);
            }
        }
    }

    public Component getRenderComponent(JLabel original, JTree tree) {
        try {
            original.setIcon(getIcon(tree));
        } catch (IOException ex) {
        }
        return original;
    }

    public void animationUpdated(CleverIcon i) {
        if (library.isCompiling()) {
            model.reload(this);
        }
    }
}
