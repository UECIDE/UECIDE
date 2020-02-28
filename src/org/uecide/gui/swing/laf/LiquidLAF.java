package org.uecide.gui.swing.laf;

import org.uecide.*;

import javax.swing.UIManager;

public class LiquidLAF extends LookAndFeel {
    public String getName() { return "Liquid"; }

    public void applyLAF() {
        try {
            UIManager.setLookAndFeel("com.birosoft.liquid.LiquidLookAndFeel");
        } catch (Exception e) {
            Base.error(e);
        }
    }

}
