package uecide.app.varcmd;

import uecide.app.*;
import java.io.File;

public class vc_prefs {
    public static String main(Sketch sketch, String args) {
        return Base.preferences.get(args);
    }
}
