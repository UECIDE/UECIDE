package org.uecide;

import java.io.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import javax.imageio.*;

public class RGB565FileConverter implements FileConverter {

    File inputFile = null;
    String prefix = null;

    File outputFile = null;
    ArrayList<String> headerLines = null;

    String progmem = "PROGMEM";
        
    public RGB565FileConverter(File in, String pref) {
        inputFile = in;
        prefix = pref;
    }

    public boolean convertFile(File buildFolder) {
        try {
            headerLines = new ArrayList<String>();

            File bins = new File(buildFolder, "binary");
            if (!bins.exists()) {
                bins.mkdirs();
            }

            outputFile = new File(bins, prefix + ".c");

            BufferedImage img = ImageIO.read(inputFile);

            boolean start = true;
            int len = 0;

            headerLines.add("static const int " + prefix + "_width = " + img.getWidth() + ";");
            headerLines.add("static const int " + prefix + "_height = " + img.getHeight() + ";");
            headerLines.add("extern const uint16_t " + prefix + "_data[];");

            PrintWriter pw = new PrintWriter(outputFile);
            pw.println("#include <Arduino.h>\n");
            pw.println("extern const uint16_t " + prefix + "_data[];");
            pw.println("const uint16_t " + prefix + "_data[] " + progmem + " = {");

            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int rgb = img.getRGB(x, y);

                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    
                    int col = ((r & 0xF8) << 8) | ((g & 0xFC) << 3) | (b >> 3);

                    String formatted = String.format("0x%04x", col & 0xFFFF);

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
            pw.println("};");
            pw.close();

            return true;

        } catch (Exception ex) {
            Debug.exception(ex);
            return false;
        }
    }

    public File getFile() {
        return outputFile;
    }

    public String[] getHeaderLines() {
        if (headerLines == null) return null;
        return headerLines.toArray(new String[0]);
    }

}
