package org.uecide;

import javax.swing.ImageIcon;
import java.util.List;
import java.util.ArrayList;
import java.awt.Component;
import java.awt.Graphics;

public class OverlayIcon extends ImageIcon {
    private ImageIcon base;
    private ArrayList<ImageIcon> overlays;

    public OverlayIcon(ImageIcon base) {
        super(base.getImage());
        this.base = base;
        this.overlays = new ArrayList<ImageIcon>();
    }

    public void add(ImageIcon overlay) {
        overlays.add(overlay);
    }

    @Override

    public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
        base.paintIcon(c, g, x, y);

        for(ImageIcon icon : overlays) {
            icon.paintIcon(c, g, x, y);
        }
    }
}
