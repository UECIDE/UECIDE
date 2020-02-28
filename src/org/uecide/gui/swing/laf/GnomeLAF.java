package org.uecide.gui.swing.laf;

import org.uecide.*;

import javax.swing.UIManager;

public class GnomeLAF extends LookAndFeel {
    public String getName() { return "Gnome"; }

    public void applyLAF() {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
        } catch (Exception e) {
            Base.error(e);
        }
    }

    public boolean isCompatible() { return Base.isLinux(); }
}
