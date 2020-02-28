package org.uecide.gui.swing.laf;

import org.uecide.*;

import javax.swing.UIManager;
import java.util.*;

public class JTattooFastLAF extends JTattooLAF {
    public String getName() { return "JTattoo: Fast"; }

    public void applyLAF() {
        doApplyLAF("Fast");
    }
}
