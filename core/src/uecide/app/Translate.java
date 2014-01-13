package uecide.app;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;
import java.util.jar.*;
import uecide.plugin.*;
import java.util.regex.*;


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
    public static HashMap<String, String> languages = new HashMap<String, String>();

    public static void loadIndex() {
        InputStream fis = Translate.class.getResourceAsStream("/uecide/app/i18n/index.txt");
        if (fis == null) {
            return;
        }
        BufferedReader br;
        String line;

        try {
            Pattern p = Pattern.compile("^([^=]+)=(.*)$");
            br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }
                if (line.startsWith("#")) {
                    continue;
                }
                Matcher m = p.matcher(line);
                if (m.find()) {
                    languages.put(m.group(1).trim(), m.group(2).trim());
                }
            }
            br.close();
        } catch (Exception e) {
            Base.error(e);
        }
    }

    public static void load(String resource) {
        try {
            load(Translate.class.getResourceAsStream(resource));
        } catch (Exception e) {
        }
    }

    public static void load(InputStream fis)
    {
        if (fis == null) {
            return;
        }
        BufferedReader br;
        String line;

        try {
            Pattern p = Pattern.compile("^([^=]+)=(.*)$");
            br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }
                if (line.startsWith("#")) {
                    continue;
                }
                Matcher m = p.matcher(line);
                if (m.find()) {
                    translations.put(m.group(1).trim(), m.group(2).trim());
                }
            }
            br.close();
        } catch (Exception e) {
            Base.error(e);
        }
    }

    public static String c(String i) { return t(i) + ": "; }
    public static String c(String i, String p1) { return t(i, p1) + ": "; }
    public static String c(String i, String p1, String p2) { return t(i, p1, p2) + ": "; }
    public static String c(String i, String p1, String p2, String p3) { return t(i, p1, p2, p3) + ": "; }
    public static String c(String i, String p1, String p2, String p3, String p4) { return t(i, p1, p2, p3, p4) + ": "; }
    public static String c(String i, String p1, String p2, String p3, String p4, String p5) { return t(i, p1, p2, p3, p4, p5) + ": "; }
    public static String c(String i, String p1, String p2, String p3, String p4, String p5, String p6) { return t(i, p1, p2, p3, p4, p5, p6) + ": "; }
    public static String c(String i, String p1, String p2, String p3, String p4, String p5, String p6, String p7) { return t(i, p1, p2, p3, p4, p5, p6, p7) + ": "; }
    public static String c(String i, String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) { return t(i, p1, p2, p3, p4, p5, p6, p7, p8) + ": "; }
    public static String c(String i, String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8, String p9) { return t(i, p1, p2, p3, p4, p5, p6, p7, p8, p9) + ": "; }

    public static String e(String i) { return t(i) + "..."; }
    public static String e(String i, String p1) { return t(i, p1) + "..."; }
    public static String e(String i, String p1, String p2) { return t(i, p1, p2) + "..."; }
    public static String e(String i, String p1, String p2, String p3) { return t(i, p1, p2, p3) + "..."; }
    public static String e(String i, String p1, String p2, String p3, String p4) { return t(i, p1, p2, p3, p4) + "..."; }
    public static String e(String i, String p1, String p2, String p3, String p4, String p5) { return t(i, p1, p2, p3, p4, p5) + "..."; }
    public static String e(String i, String p1, String p2, String p3, String p4, String p5, String p6) { return t(i, p1, p2, p3, p4, p5, p6) + "..."; }
    public static String e(String i, String p1, String p2, String p3, String p4, String p5, String p6, String p7) { return t(i, p1, p2, p3, p4, p5, p6, p7) + "..."; }
    public static String e(String i, String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) { return t(i, p1, p2, p3, p4, p5, p6, p7, p8) + "..."; }
    public static String e(String i, String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8, String p9) { return t(i, p1, p2, p3, p4, p5, p6, p7, p8, p9) + "..."; }

    public static String n(String i) { return t(i) + "\n"; }
    public static String n(String i, String p1) { return t(i, p1) + "\n"; }
    public static String n(String i, String p1, String p2) { return t(i, p1, p2) + "\n"; }
    public static String n(String i, String p1, String p2, String p3) { return t(i, p1, p2, p3) + "\n"; }
    public static String n(String i, String p1, String p2, String p3, String p4) { return t(i, p1, p2, p3, p4) + "\n"; }
    public static String n(String i, String p1, String p2, String p3, String p4, String p5) { return t(i, p1, p2, p3, p4, p5) + "\n"; }
    public static String n(String i, String p1, String p2, String p3, String p4, String p5, String p6) { return t(i, p1, p2, p3, p4, p5, p6) + "\n"; }
    public static String n(String i, String p1, String p2, String p3, String p4, String p5, String p6, String p7) { return t(i, p1, p2, p3, p4, p5, p6, p7) + "\n"; }
    public static String n(String i, String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) { return t(i, p1, p2, p3, p4, p5, p6, p7, p8) + "\n"; }
    public static String n(String i, String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8, String p9) { return t(i, p1, p2, p3, p4, p5, p6, p7, p8, p9) + "\n"; }

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
