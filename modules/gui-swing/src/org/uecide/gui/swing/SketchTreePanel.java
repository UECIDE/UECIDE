package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.ContextEvent;
import org.uecide.ContextEventListener;
import javax.swing.JScrollPane;

public class SketchTreePanel extends TabPanel implements ContextEventListener {
    SketchTree tree;
    Context ctx;

    public SketchTreePanel(Context c, AutoTab def) {
        super("Sketch", def);
        ctx = c;
        ctx.listenForEvent("sketchFileAdded", this);
        tree = new SketchTree(ctx);
        JScrollPane scroll = new JScrollPane(tree);
        add(scroll);
    }

    public void contextEventTriggered(ContextEvent e) {
        if (e.getEvent().equals("sketchFileAdded")) {
            SketchTreeModel m = (SketchTreeModel)tree.getModel();
            m.updateChildren();
        }
    }
}
