package org.uecide.gui.swing.laf;

import org.uecide.*;

import javax.swing.UIManager;
import java.util.*;

import com.jtattoo.plaf.AbstractTheme;
import com.jtattoo.plaf.smart.SmartLookAndFeel;

public class JTattooSmartLAF extends JTattooLAF {
    public String getName() { return "JTattoo: Smart"; }

    public void applyLAF() {
        doApplyLAF("Smart");
    }


}
