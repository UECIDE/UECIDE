/*
 * Copyright (c) 2014, Majenko Technologies
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 *
 * * Neither the name of Majenko Technologies nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.uecide;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;
import java.util.jar.*;
import org.uecide.plugin.*;

import javax.swing.*;

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
    public static TreeMap<String, String> translations = new TreeMap<String, String>();

    public static void load(String language) {
        InputStream fis;
        BufferedReader br;
        String line;
        File lib = Base.getContentFile("lib");
        File lang = new File(lib, language + ".po");

        translations = new TreeMap<String, String>();

        if(!lang.exists()) {
            return;
        }

        try {
            fis = new FileInputStream(lang);
            br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));

            while((line = br.readLine()) != null) {
                String[] bits = line.split("::");

                if(bits.length == 2) {
                    translations.put(bits[0], bits[1]);
                }
            }

            br.close();
        } catch(Exception e) {
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

        if(j == null) {
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

        for(String q : b) {
            if(len + q.length() > w) {
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
