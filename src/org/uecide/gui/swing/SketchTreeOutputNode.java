package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.FileType;
import org.uecide.SketchFile;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;

import java.io.IOException;

public class SketchTreeOutputNode extends SketchTreeNodeBase {
    public SketchTreeOutputNode(Context c) {
        super(c, "Output");
        updateChildren();
    }

    public boolean updateChildren() {
        removeAllChildren();
        for (SketchFile f : ctx.getSketch().getSketchFiles().values()) {
            if (f.getGroup() == FileType.GROUP_HEADER) {
                SketchSourceFileNode sfn = new SketchSourceFileNode(ctx, f);
                add(sfn);
            }
        }
        return true;
    }

    public ImageIcon getIcon(JTree tree) throws IOException {
        if (tree.isExpanded(getTreePath())) {
            return IconManager.getIcon(16, "tree.folder-open");
        } else {
            return IconManager.getIcon(16, "tree.folder-closed");
        }
    }

    public JPopupMenu getPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        return menu;
    }
    public void performDoubleClick() {
    }
}
