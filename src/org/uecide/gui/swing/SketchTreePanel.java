package org.uecide.gui.swing;

import org.uecide.Context;
import javax.swing.JScrollPane;

public class SketchTreePanel extends TabPanel {
    SketchTree tree;
    Context ctx;

    public SketchTreePanel(Context c) {
        super("Sketch");
        ctx = c;
        tree = new SketchTree(ctx);
        JScrollPane scroll = new JScrollPane(tree);
        add(scroll);
    }
}
