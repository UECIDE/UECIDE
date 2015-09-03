package org.uecide;

import javax.swing.UIManager;
import java.util.*;
import java.lang.reflect.*;

class JTattooSmartLAF extends JTattooBase {
    public static void applyLAF() {
        configureLAF("Smart");
    }

    public static String getName() { return "JTattoo: Smart"; }
    public static boolean isCompatible() { return true; }

}
