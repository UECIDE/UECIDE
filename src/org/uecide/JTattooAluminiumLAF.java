package org.uecide;

import javax.swing.UIManager;
import java.util.*;
import java.lang.reflect.*;

class JTattooAluminiumLAF extends JTattooBase {

    public static void applyLAF() {
        configureLAF("Aluminium");
    }

    public static String getName() { return "JTattoo: Aluminium"; }
    public static boolean isCompatible() { return true; }

}
