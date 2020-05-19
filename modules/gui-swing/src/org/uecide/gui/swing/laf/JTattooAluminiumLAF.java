package org.uecide.gui.swing.laf;

import org.uecide.*;

import javax.swing.UIManager;
import java.util.*;

public class JTattooAluminiumLAF extends JTattooLAF {
    public String getName() { return "JTattoo: Aluminium"; }

    public void applyLAF() {
        doApplyLAF("Aluminium");
    }

    public String getStyleSheet(int type) {
        switch (type) {
            case LookAndFeel.STYLESHEET_DIALOG:
                return "body { font-family: Arial, Helvetica, Sans-Serif; font-size: 12px; }  p { margin: 0px 0px 5px 0px; } ";
        }
        return "";
    }

}
