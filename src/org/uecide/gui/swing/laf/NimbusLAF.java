package org.uecide.gui.swing.laf;

import org.uecide.*;

import javax.swing.UIManager;

public class NimbusLAF extends LookAndFeel {
    public String getName() { return "Nimbus"; }

    public void applyLAF() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            UECIDE.error(e);
        }
    }

}
