package org.uecide.gui.swing.laf;

import org.uecide.*;

import javax.swing.UIManager;

public class MetalLAF extends LookAndFeel {
    public String getName() { return "Metal"; }

    public void applyLAF() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (Exception e) {
            UECIDE.error(e);
        }
    }

}
