package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.ContextEvent;
import org.uecide.ContextEventListener;
import org.uecide.Debug;
import org.uecide.PropertyFile;
import org.uecide.Utils;

import java.io.File;

import javax.swing.JTree;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;

import java.awt.Component;
import java.awt.Font;
import java.awt.font.TextAttribute;

import java.util.Map;

public class BinaryFileNode extends GenericFileNode implements ContextEventListener {
    public BinaryFileNode(Context c, SketchTreeModel m, File f) {
        super(c, m, f);
        ctx.listenForEvent("binaryFileConversionChanged", this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Component getRenderComponent(JLabel original, JTree tree) {
        try {
            original.setIcon(getIcon(tree));
        } catch (Exception ex) {
            Debug.exception(ex);
        }

        PropertyFile pf = ctx.getSketch().getSettings();
        Font font = original.getFont().deriveFont(Font.PLAIN);
        if (pf.getBoolean("binary." + Utils.sanitize(file.getName()) + ".conversion")) {
            original.setFont(font);
        } else {
            // This needs Map<TextAttribute, ?> but throws a wobbly with it.
            Map attributes = font.getAttributes();
            attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
            Font newFont = new Font(attributes); 
            original.setFont(newFont);
        }
        return original;
    }

    @Override
    public JPopupMenu getPopupMenu() {
        JPopupMenu menu = super.getPopupMenu();
        if (!file.isDirectory()) {
            menu.addSeparator();
            menu.add(new BinaryFileConversionMenuItem(ctx, file));
            menu.addSeparator();
        }
        return menu;
    }

    @Override
    public void contextEventTriggered(ContextEvent evt) {
        if (updateChildren()) {
            model.reload(this);
        }
    }

}
