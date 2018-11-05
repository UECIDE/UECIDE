package org.uecide;

import java.io.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import javax.imageio.*;

public class ImageFileConverter implements FileConverter {

    File inputFile = null;
    String prefix = null;
    Color transparency = null;
    int conversionType = 0;
    String dataType;

    File outputFile = null;
    ArrayList<String> headerLines = null;

    // Chosen nibble meanings:
    //  0x <bit depth H> <bit depth L> <format> <rgb/bgr>
    // Bit depths are 0 = special code, n = bits in the format
    // 0x0000 and 0x0001 are special and always must mean the same. I.e., no inclusion, and
    // raw inclusion.
        
    public static final int NONE        = 0x0000;
    public static final int RAW         = 0x0001;
    public static final int XBM         = 0x0100;
    public static final int RGB555      = 0x0F00;
    public static final int BGR555      = 0x0F01;
    public static final int RGB565      = 0x1000;
    public static final int BGR565      = 0x1001;
    public static final int RGB888      = 0x1800;
    public static final int BGR888      = 0x1801;
    public static final int ARGB8888    = 0x2000;
    public static final int ABGR8888    = 0x2001;
    public static final int RGBA8888    = 0x2010;
    public static final int BGRA8888    = 0x2011;

    public static final int UINT8_T     = 0;
    public static final int UINT16_T    = 1;
    public static final int UINT32_T    = 2;

    public static KeyValuePair[] conversionOptions = {
        new KeyValuePair(NONE, "Do not include"),
        new KeyValuePair(RAW, "Do not convert (raw inclusion)"),
        new KeyValuePair(RGB565, "RGB 565 (16 bit)"),
        new KeyValuePair(RGB555, "RGB 555 (16 bit)"),
        new KeyValuePair(RGB888, "RGB 888 (24 bit)"),
        new KeyValuePair(ARGB8888, "ARGB 8888 (32 bit)"),
        new KeyValuePair(RGBA8888, "RGBA 8888 (32 bit)"),
        new KeyValuePair(BGR565, "BGR 565 (16 bit)"),
        new KeyValuePair(BGR555, "BGR 555 (16 bit)"),
        new KeyValuePair(BGR888, "BGR 888 (24 bit)"),
        new KeyValuePair(ABGR8888, "ABGR 8888 (32 bit)"),
        new KeyValuePair(BGRA8888, "BGRA 8888 (32 bit)"),
        new KeyValuePair(XBM, "XBM (1 bit, u8glib)")
    };

    public ImageFileConverter(File in, int type, String dt, String pref, Color trans) {
        inputFile = in;
        conversionType = type;
        dataType = dt;
        if (dataType == null) {
            dataType = "uint8_t";
        }
        prefix = pref;
        transparency = trans;
    }

    public boolean convertFile(File buildFolder) {
        try {
            headerLines = new ArrayList<String>();

            File bins = new File(buildFolder, "binary");
            if (!bins.exists()) {
                bins.mkdirs();
            }

            outputFile = new File(bins, prefix + ".c");

            int format = UINT8_T;
            if (dataType.equals("uint8_t")) {
                format = UINT8_T;
            } else if (dataType.equals("uint16_t")) {
                format = UINT16_T;
            } else if (dataType.equals("uint32_t")) {
                format = UINT32_T;
            }

            BufferedImage image = ImageIO.read(inputFile);
            int width = image.getWidth();
            int height = image.getHeight();

            switch (conversionType) {
                case RGB555:
                case BGR555:
                case RGB565:
                case BGR565:
                case RGB888:
                case BGR888: {
                    BufferedImage flat = new BufferedImage(width, height, image.getType());
                    Graphics2D g = flat.createGraphics();
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                    g.setColor(transparency);
                    g.fillRect(0, 0, width, height);
                    g.drawImage(image, 0, 0, null);
                    g.dispose();
                    image = flat;
                }
            }

            headerLines.add("static const int " + prefix + "_width = " + width + ";");
            headerLines.add("static const int " + prefix + "_height = " + height + ";");
            headerLines.add("extern " + dataType + " " + prefix + "_data[];");

            PrintWriter pw = new PrintWriter(outputFile);
            pw.println("#include <Arduino.h>\n");
            pw.println("const " + dataType + " " + prefix + "_data[] PROGMEM = {");

            if (conversionType == XBM) {
                boolean start = true;
                int len = 0;
                int out = 0;

                pw.print("    ");

                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x += 8) {
                        out = 0;
                        for (int z = 0; z < 8; z++) {
                            int px = x + z;
                            Color c;
                            if (px < width) {
                                c = new Color(image.getRGB(px, y));
                            } else {
                                c = Color.BLACK;
                            }

                            int tot = (c.getRed() + c.getGreen() + c.getBlue()) / 3;

                            out <<= 1;
                            if (tot > 127) {
                                out |= 1;
                            }
                        }

                        if (start) {
                            pw.print("    ");
                            start = false;
                        } else {
                            if (len >= 80) {
                                pw.print(",\n    ");
                                len = 0;
                            } else {
                                pw.print(", ");
                            }
                        }

                        pw.print(String.format("0x%02x", out));
                        len += 6;
                    }
                }

            } else {
                boolean start = true;
                int len = 0;
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        Color c = new Color(image.getRGB(x, y));
                        String formatted = formatColor(c, conversionType, format);
                        if (start) {
                            pw.print("    ");
                            start = false;
                        } else {
                            if (len >= 80) {
                                pw.print(",\n    ");
                                len = 0;
                            } else {
                                pw.print(",");
                            }
                        }
                        pw.print(formatted);
                        len += formatted.length();
                    }
                }
            }

            pw.println();

            pw.println("};");

            pw.close();

            return true;

        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    String formatColor(Color c, int type, int format) {
        int r = c.getRed();
        int g = c.getGreen();
        int b = c.getBlue();
        int a = c.getAlpha();

        int converted = 0;
        int bits = 0;
        switch (type) {
            case RGB565:
                r >>= 3;
                g >>= 2;
                b >>= 3;
                converted = (r << 11) | (g << 5) | b;
                bits = 16;
                break;

            case BGR565:
                r >>= 3;
                g >>= 2;
                b >>= 3;
                converted = (b << 11) | (g << 5) | r;
                bits = 16;
                break;

            case RGB555:
                r >>= 3;
                g >>= 3;
                b >>= 3;
                converted = (r << 10) | (g << 5) | b;
                bits = 16;
                break;

            case BGR555:
                r >>= 3;
                g >>= 3;
                b >>= 3;
                converted = (b << 10) | (g << 5) | r;
                bits = 16;
                break;

            case RGB888:
                converted = (r << 16) | (g << 8) | b;
                bits = 24;
                break;

            case BGR888:
                converted = (b << 16) | (g << 8) | r;
                bits = 24;
                break;

            case ARGB8888:
                converted = (a << 24) | (r << 16) | (g << 8) | b;
                bits = 32;
                break;

            case ABGR8888:
                converted = (a << 24) | (b << 16) | (g << 8) | r;
                bits = 32;
                break;

            case RGBA8888:
                converted = (r << 24) | (g << 16) | (b << 8) | a;
                bits = 32;
                break;

            case BGRA8888:
                converted = (b << 24) | (g << 16) | (r << 8) | a;
                bits = 32;
                break;

        }

        String formatted = "";

        switch (format) {
            case UINT8_T:
                switch (bits) {
                    case 8:
                        formatted = String.format("0x%02X", converted & 0xFF);
                        break;
                    case 16:
                        formatted = String.format("0x%02X, 0x%02X", converted & 0xFF, (converted >> 8) & 0xFF);
                        break;
                    case 24:
                        formatted = String.format("0x%02X, 0x%02X, 0x%02X", converted & 0xFF, (converted >> 8) & 0xFF, (converted >> 16) & 0xFF);
                        break;
                    case 32:
                        formatted = String.format("0x%02X, 0x%02X, 0x%02X, 0x%02X", converted & 0xFF, (converted >> 8) & 0xFF, (converted >> 16) & 0xFF, (converted >> 24) & 0xFF);
                        break;
                }
                break;
            case UINT16_T:
                switch (bits) {
                    case 8:
                        formatted = String.format("0x%04X", converted & 0xFF);
                        break;
                    case 16:
                        formatted = String.format("0x%04X", converted & 0xFFFF);
                        break;
                    case 24:
                        formatted = String.format("0x%04X, 0x%04X", converted & 0xFFFF, (converted >> 16) & 0xFF);
                        break;
                    case 32:
                        formatted = String.format("0x%04X, 0x%04X", converted & 0xFFFF, (converted >> 16) & 0xFFFF);
                        break;
                }
                break;
            case UINT32_T:
                switch (bits) {
                    case 8:
                        formatted = String.format("0x%08X", converted & 0xFF);
                        break;
                    case 16:
                        formatted = String.format("0x%08X", converted & 0xFFFF);
                        break;
                    case 24:
                        formatted = String.format("0x%08X", converted & 0xFFFFFF);
                        break;
                    case 32:
                        formatted = String.format("0x%08X", converted);
                        break;
                }
                break;
        }
        return formatted;
    }

    public File getFile() {
        return outputFile;
    }

    public String[] getHeaderLines() {
        if (headerLines == null) return null;
        return headerLines.toArray(new String[0]);
    }

}
