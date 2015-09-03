package org.uecide;

import javax.swing.UIManager;
import java.util.*;
import java.lang.reflect.*;

class JTattooHiFiLAF extends JTattooBase {
    public static void applyLAF() {
        configureLAF("HiFi");
    }

    public static String getName() { return "JTattoo: HiFi"; }
    public static boolean isCompatible() { return true; }

}
