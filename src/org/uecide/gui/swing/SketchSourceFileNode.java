package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.SketchFile;
import org.uecide.FileType;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;

import java.io.IOException;

public class SketchSourceFileNode extends SketchTreeNodeBase {
    SketchFile sketchFile;

    public SketchSourceFileNode(Context c, SketchFile sf) {
        super(c, sf.getFile().getName());
        sketchFile = sf;
    }

    public SketchFile getSketchFile() {
        return sketchFile;
    }

    public ImageIcon getIcon(JTree tree) throws IOException {
        return IconManager.getIcon(16, "mime." + FileType.getIcon(getSketchFile().getFile()));
    }

    public boolean updateChildren() {
        return false;
    }

    public JPopupMenu getPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
//        if (!sketchFile.isMainFile()) {
            menu.add(new DeleteFileMenuItem(ctx, sketchFile.getFile()));
            menu.add(new RenameFileMenuItem(ctx, sketchFile.getFile()));
//        }
        return menu;
    }

    public void performDoubleClick() {
        ctx.warning("I want to open " + sketchFile.getFile().getAbsolutePath());
        ctx.action("openSketchFile", sketchFile);
    }

}
