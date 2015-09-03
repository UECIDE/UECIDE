package org.uecide;

import javax.swing.UIManager;
import java.util.*;
import java.lang.reflect.*;

class JTattooGraphiteLAF extends JTattooBase {
    public static void applyLAF() {
        configureLAF("Graphite");
    }

    public static String getName() { return "JTattoo: Graphite"; }
    public static boolean isCompatible() { return true; }

}
