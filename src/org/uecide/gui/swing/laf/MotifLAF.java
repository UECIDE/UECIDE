package org.uecide.gui.swing.laf;

import org.uecide.*;

import javax.swing.UIManager;

public class MotifLAF extends LookAndFeel {
    public String getName() { return "Motif"; }

    public void applyLAF() {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
        } catch (Exception e) {
            UECIDE.error(e);
        }
    }
}
