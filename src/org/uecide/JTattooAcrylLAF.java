package org.uecide;

import javax.swing.UIManager;
import java.util.*;
import java.lang.reflect.*;

class JTattooAcrylLAF extends JTattooBase {
    public static void applyLAF() {
        configureLAF("Acryl");
    }

    public static String getName() { return "JTattoo: Acryl"; }
    public static boolean isCompatible() { return true; }

}
