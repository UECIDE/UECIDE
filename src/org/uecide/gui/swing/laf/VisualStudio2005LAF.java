package org.uecide.gui.swing.laf;

import org.uecide.*;

import javax.swing.UIManager;

public class VisualStudio2005LAF extends LookAndFeel {
    public String getName() { return "Office XP"; }

    public void applyLAF() {
        if (!Base.isWindows()) {
            Base.error("The selected Look and Feel is only compatible with Windows. Select another.");
            return;
        }
        try {
            UIManager.setLookAndFeel("org.fife.plaf.VisualStudio2005.VisualStudio2005LookAndFeel");
        } catch (Exception e) {
            Base.error(e);
        }
    }

    public boolean isCompatible() { return Base.isWindows(); }

}
