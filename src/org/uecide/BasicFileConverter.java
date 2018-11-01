package org.uecide;

import java.io.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import javax.imageio.*;

public class BasicFileConverter implements FileConverter {

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

    public static KeyValuePair[] conversionOptions = {
        new KeyValuePair(NONE, "Do not include"),
        new KeyValuePair(RAW, "Do not convert (raw inclusion)"),
    };

    public BasicFileConverter(File in, String pref) {
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

            FileInputStream fis = new FileInputStream(inputFile);

            headerLines.add("static const int " + prefix + "_length = " + inputFile.length() + ";");
            headerLines.add("extern uint8_t " + prefix + "_data[];");

            PrintWriter pw = new PrintWriter(outputFile);
            pw.println("#include <Arduino.h>\n");
            pw.println("const uint8_t " + prefix + "_data[] PROGMEM = {");

            boolean start = true;
            int len = 0;

            for (int x = 0; x < inputFile.length(); x++) {
                int r = fis.read();
                String formatted = String.format("0x%02X", r & 0xFF);
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
            fis.close();

            pw.println("};");

            pw.close();

            return true;

        } catch (Exception ex) {
            ex.printStackTrace();
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
