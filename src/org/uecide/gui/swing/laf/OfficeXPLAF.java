package org.uecide.gui.swing.laf;

import org.uecide.*;

import javax.swing.UIManager;

public class OfficeXPLAF extends LookAndFeel {
    public String getName() { return "Office XP"; }

    public void applyLAF() {
        if (!Base.isWindows()) {
            Base.error("The selected Look and Feel is only compatible with Windows. Select another.");
            return;
        }

        try {
            UIManager.setLookAndFeel("org.fife.plaf.OfficeXP.OfficeXPLookAndFeel");
        } catch (Exception e) {
            Base.error(e);
        }
    }

    public boolean isCompatible() { return Base.isWindows(); }

}
