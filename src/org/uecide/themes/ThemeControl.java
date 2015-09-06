package org.uecide.themes;

import org.uecide.*;

import java.io.*;
import java.awt.*;

public class ThemeControl {

    public static void init() {
        // Override this function to do stuff at load time
    }

    public static void loadFont(String path) {
        try {
            InputStream is = Base.class.getResourceAsStream(path);
            Font f = Font.createFont(Font.TRUETYPE_FONT, is);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(f);
            Debug.message("Loaded font " + f);
        } catch (Exception e) {
            Base.error(e);
        }
    }

}
