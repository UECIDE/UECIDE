package org.uecide;

import javax.swing.*;
import java.awt.image.*;
import java.net.*;
import java.io.*;
import javax.imageio.*;

public class InternalIcon extends ImageIcon {

    String category;
    String name;
    int size;
    
    public InternalIcon(String cat, String n, int s) throws IOException {
        super();
        category = cat;
        name = n;
        size = s;
        updateIconData();
    }

    public void updateIconData() throws IOException {
        URL path = getIconURL(Preferences.get("theme.icons"), category, name, size);

        if (path == null) {
            path = getIconURL("gnomic", category, name, size);
        }

        if (path == null) {
            path = getIconURL("gnomic", "actions", "unknown", size);
        }

        BufferedImage img = ImageIO.read(path);
        setImage(img);
    }


    URL getIconURL(String iconset, String category, String name, int size) {
        return InternalIcon.class.getResource("/org/uecide/icons/" + iconset + "/" + size + "x" + size + "/" + category + "/" + name + ".png");
    }

}
