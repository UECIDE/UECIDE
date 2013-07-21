package uecide.plugin;

import uecide.app.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.util.*;
import java.net.*;
import java.awt.datatransfer.*;

/**
 * Color selector tool for the Tools menu.
 * <p/>
 * Using the keyboard shortcuts, you can copy/paste the values for the
 * colors and paste them into your program. We didn't do any sort of
 * auto-insert of colorMode() or fill() or stroke() code cuz we couldn't
 * decide on a good way to do this.. your contributions welcome).
 */
public class ColorSelector extends BasePlugin {

    static Color lastColor = new Color(0,0,0);

    public String getMenuTitle() {
        return "Color Selector";
    }
  
    public void run() {
        Color newColor = JColorChooser.showDialog(editor, "Color Selector", lastColor);
        if (newColor != null) {
            lastColor = newColor;
            String color = String.format("byte color[] = {%d, %d, %d};\n", lastColor.getRed(), lastColor.getGreen(), lastColor.getBlue());

            Clipboard clipboard = editor.getToolkit().getSystemClipboard();
            clipboard.setContents( new StringSelection(color), null);
        }
    }
}
