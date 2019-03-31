package mdlaf.utils;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.util.Map;

public class MaterialDrawingUtils {

	static {
		System.setProperty ("awt.useSystemAAFontSettings", "on");
		System.setProperty ("swing.aatext", "true");
		System.setProperty ("sun.java2d.xrender", "true");
	}

	public static Graphics getAliasedGraphics (Graphics g) {

		Graphics2D g2d = (Graphics2D) g;
        try {
            RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            hints.put(RenderingHints.KEY_RENDERING,	RenderingHints.VALUE_RENDER_DEFAULT);
            hints.put(RenderingHints.KEY_COLOR_RENDERING,	RenderingHints.VALUE_COLOR_RENDER_DEFAULT);
            hints.put(RenderingHints.KEY_TEXT_ANTIALIASING,	RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2d.setRenderingHints (hints);
        } catch (Exception e) {
            e.printStackTrace();
        }

		return g2d;
	}
}
