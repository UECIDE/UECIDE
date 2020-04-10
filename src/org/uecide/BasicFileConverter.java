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

    String progmem = "PROGMEM";
    String endian = "little";
        
    public BasicFileConverter(File in, String pref, String dt, String en) {
        inputFile = in;
        prefix = pref;
        dataType = dt;
        endian = en;
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


            boolean start = true;
            int len = 0;

            boolean signed = false;
            int bytes = 1;
            
            switch (dataType) {
                case "uint8_t":
                    signed = false;
                    bytes = 1;
                    break;
                case "int8_t":
                    signed = true;
                    bytes = 1;
                    break;
                case "uint16_t":
                    signed = false;
                    bytes = 2;
                    break;
                case "int16_t":
                    signed = true;
                    bytes = 2;
                    break;
                case "uint32_t":
                    signed = false;
                    bytes = 4;
                    break;
                case "int32_t":
                    signed = true;
                    bytes = 4;
                    break;
            }

            headerLines.add("static const int " + prefix + "_length = " + inputFile.length() / bytes + ";");
            headerLines.add("extern const " + dataType + " " + prefix + "_data[];");

            PrintWriter pw = new PrintWriter(outputFile);
            pw.println("#include <Arduino.h>\n");
            pw.println("extern const " + dataType + " " + prefix + "_data[];");
            pw.println("const " + dataType + " " + prefix + "_data[] " + progmem + " = {");

            for (int x = 0; x < inputFile.length(); x += bytes) {

                String formatted = null;

                if (bytes == 1) {
                    int r = fis.read() & 0xFF;
                    if (signed) {
                        // sign extend
                        if ((r & 0x80) == 0x80) {
                            r |= 0xFFFFFF00;
                        }
                        formatted = String.format("%d", r);
                    } else {
                        formatted = String.format("0x%02x", r & 0xFF);
                    }
                } else if (bytes == 2) {
                    int b1 = fis.read();
                    int b2 = fis.read();

                    if (b1 == -1) { b1 = 0; } else { b1 = b1 & 0xFF; }
                    if (b2 == -1) { b2 = 0; } else { b2 = b2 & 0xFF; }

                    int r = 0;
                    if (endian.equals("big")) {
                        r = (b1 << 8) | b2;
                    } else {
                        r = (b2 << 8) | b1;
                    }

                    if (signed) {
                        // sign extend
                        if ((r & 0x8000) == 0x8000) {
                            r |= 0xFFFF0000;
                        }
                        formatted = String.format("%d", r);
                    } else {
                        formatted = String.format("0x%04x", r & 0xFFFF);
                    }
                } else if (bytes == 4) {
                    int b1 = fis.read();
                    int b2 = fis.read();
                    int b3 = fis.read();
                    int b4 = fis.read();

                    if (b1 == -1) { b1 = 0; } else { b1 = b1 & 0xFF; }
                    if (b2 == -1) { b2 = 0; } else { b2 = b2 & 0xFF; }
                    if (b3 == -1) { b3 = 0; } else { b3 = b3 & 0xFF; }
                    if (b4 == -1) { b4 = 0; } else { b4 = b4 & 0xFF; }

                    int r = 0;
                    if (endian.equals("big")) {
                        r = (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
                    } else {
                        r = (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
                    }

                    if (signed) {
                        formatted = String.format("%d", r);
                    } else {
                        formatted = String.format("0x%08x", r & 0xFFFFFFFF);
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
