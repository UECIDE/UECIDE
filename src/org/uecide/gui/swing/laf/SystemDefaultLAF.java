package org.uecide.gui.swing.laf;

import org.uecide.*;

import javax.swing.UIManager;

public class SystemDefaultLAF extends LookAndFeel {
    public String getName() { return "System Default"; }

    public void applyLAF() {
        try {
            if (UECIDE.isMacOS()) {
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                System.setProperty("com.apple.mrj.application.apple.menu.about.name", "UECIDE");
            }
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            UECIDE.error(e);
        }
    }

}
