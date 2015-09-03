package org.uecide;

import javax.swing.UIManager;
import java.util.*;
import java.lang.reflect.*;

class JTattooAeroLAF extends JTattooBase {
    public static void applyLAF() {
        configureLAF("Aero");
    }

    public static String getName() { return "JTattoo: Aero"; }
    public static boolean isCompatible() { return true; }

}

