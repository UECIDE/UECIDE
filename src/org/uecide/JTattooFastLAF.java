package org.uecide;

import javax.swing.UIManager;
import java.util.*;
import java.lang.reflect.*;

class JTattooFastLAF extends JTattooBase {
    public static void applyLAF() {
        configureLAF("Fast");
    }

    public static String getName() { return "JTattoo: Fast"; }
    public static boolean isCompatible() { return true; }

}
