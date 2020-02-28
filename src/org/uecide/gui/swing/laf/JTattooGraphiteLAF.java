package org.uecide.gui.swing.laf;

import org.uecide.*;

import javax.swing.UIManager;
import java.util.*;

public class JTattooGraphiteLAF extends JTattooLAF {
    public String getName() { return "JTattoo: Graphite"; }

    public void applyLAF() {
        doApplyLAF("Graphite");
    }
}
