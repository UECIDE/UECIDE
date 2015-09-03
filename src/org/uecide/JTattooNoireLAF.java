package org.uecide;

import javax.swing.UIManager;
import java.util.*;
import java.lang.reflect.*;

class JTattooNoireLAF extends JTattooBase {
    public static void applyLAF() {
        configureLAF("Noire");
    }

    public static String getName() { return "JTattoo: Noire"; }
    public static boolean isCompatible() { return true; }

}
