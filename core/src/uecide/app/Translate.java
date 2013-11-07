package uecide.app;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;
import java.util.jar.*;
import uecide.plugin.*;

import javax.swing.*;

import uecide.app.debug.Board;
import uecide.app.debug.Core;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import java.nio.charset.*;


/**
 * The base class for the main uecide.application.
 * Primary role of this class is for platform identification and
 * general interaction with the system (launching URLs, loading
 * files and images, etc) that comes from that.
 */
public class Translate {
    public static HashMap<String, String> translations = new HashMap<String, String>();

    public static void load(String language)
    {
        InputStream fis;
        BufferedReader br;
        String line;
        File lib = Base.getContentFile("lib");
        File lang = new File(lib, language + ".po");
    
        translations = new HashMap<String, String>();
        if (!lang.exists()) {
            return;
        }
        try {
            fis = new FileInputStream(lang);
            br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
            while ((line = br.readLine()) != null) {
                String[] bits = line.split("::");
                if (bits.length == 2) {
                    translations.put(bits[0], bits[1]);
                }
            }
            br.close();
        } catch (Exception e) {
            Base.error(e);
        }
    }

    public static String t(String i) {
        return t(i, "", "", "", "", "", "", "", "", "");
    }

    public static String t(String i, String p1) {
        return t(i, p1, "", "", "", "", "", "", "", "");
    }

    public static String t(String i, String p1, String p2) {
        return t(i, p1, p2, "", "", "", "", "", "", "");
    }

    public static String t(String i, String p1, String p2, String p3) {
        return t(i, p1, p2, p3, "", "", "", "", "", "");
    }

    public static String t(String i, String p1, String p2, String p3, String p4) {
        return t(i, p1, p2, p3, p4, "", "", "", "", "");
    }

    public static String t(String i, String p1, String p2, String p3, String p4, String p5) {
        return t(i, p1, p2, p3, p4, p5, "", "", "", "");
    }

    public static String t(String i, String p1, String p2, String p3, String p4, String p5, String p6) {
        return t(i, p1, p2, p3, p4, p5, p6, "", "", "");
    }

    public static String t(String i, String p1, String p2, String p3, String p4, String p5, String p6, String p7) {
        return t(i, p1, p2, p3, p4, p5, p6, p7, "", "");
    }

    public static String t(String i, String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {
        return t(i, p1, p2, p3, p4, p5, p6, p7, p8, "");
    }

    public static String t(String i, String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8, String p9) {
        String j = translations.get(i);
        if (j == null) {
  //          System.err.println("Unhandled translation: \"" + i + "\"");
            j = i;
        }
        j = j.replace("%1", p1);
        j = j.replace("%2", p2);
        j = j.replace("%3", p3);
        j = j.replace("%4", p4);
        j = j.replace("%5", p5);
        j = j.replace("%6", p6);
        j = j.replace("%7", p7);
        j = j.replace("%8", p8);
        j = j.replace("%9", p9);
        return j;
    }

    public static String w(String i, int w, String d) {
        return w(i, w, d, "", "", "", "", "", "", "", "", "");
    }

    public static String w(String i, int w, String d, String p1) {
        return w(i, w, d, p1, "", "", "", "", "", "", "", "");
    }

    public static String w(String i, int w, String d, String p1, String p2) {
        return w(i, w, d, p1, p2, "", "", "", "", "", "", "");
    }

    public static String w(String i, int w, String d, String p1, String p2, String p3) {
        return w(i, w, d, p1, p2, p3, "", "", "", "", "", "");
    }

    public static String w(String i, int w, String d, String p1, String p2, String p3, String p4) {
        return w(i, w, d, p1, p2, p3, p4, "", "", "", "", "");
    }

    public static String w(String i, int w, String d, String p1, String p2, String p3, String p4, String p5) {
        return w(i, w, d, p1, p2, p3, p4, p5, "", "", "", "");
    }

    public static String w(String i, int w, String d, String p1, String p2, String p3, String p4, String p5, String p6) {
        return w(i, w, d, p1, p2, p3, p4, p5, p6, "", "", "");
    }

    public static String w(String i, int w, String d, String p1, String p2, String p3, String p4, String p5, String p6, String p7) {
        return w(i, w, d, p1, p2, p3, p4, p5, p6, p7, "", "");
    }

    public static String w(String i, int w, String d, String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {
        return w(i, w, d, p1, p2, p3, p4, p5, p6, p7, p8, "");
    }

    public static String w(String i, int w, String d, String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8, String p9) {
        String j = t(i, p1, p2, p3, p4, p5, p6, p7, p8, p9);
        String o = "";
        String[] b = j.split(" ");
        int len = 0;
        for (String q : b) {
            if (len + q.length() > w) {
                o += d;
                o += q + " ";
                len = q.length() + 1;
            } else {
                o += q + " ";
                len += q.length() + 1;
            }
        }
        return o;
    }
}
