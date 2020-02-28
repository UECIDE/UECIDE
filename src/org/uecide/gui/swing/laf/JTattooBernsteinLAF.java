package org.uecide.gui.swing.laf;

import org.uecide.*;

import javax.swing.UIManager;
import java.util.*;

public class JTattooBernsteinLAF extends JTattooLAF {
    public String getName() { return "JTattoo: Bernstein"; }

    public void applyLAF() {
        doApplyLAF("Bernstein");
    }
}
