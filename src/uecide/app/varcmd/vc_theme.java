package uecide.app.varcmd;

import uecide.app.*;
import java.io.File;

public class vc_theme {
    public static String main(Sketch sketch, String args) {
        return Base.theme.get(args);
    }
}
