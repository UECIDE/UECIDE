package org.uecide;

import javax.swing.UIManager;
import java.util.*;
import java.lang.reflect.*;

class JTattooBernsteinLAF extends JTattooBase {
    public static void applyLAF() {
        configureLAF("Bernstein");
    }

    public static String getName() { return "JTattoo: Bernstein"; }
    public static boolean isCompatible() { return true; }

}
