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

import org.uecide.plugin.*;
import org.uecide.debug.*;
import org.uecide.editors.*;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.net.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.border.*;
import java.lang.reflect.*;
import javax.imageio.*;

import java.awt.datatransfer.*;

import org.uecide.Compiler;

import java.beans.*;

import java.util.jar.*;
import java.util.zip.*;

public class FunctionBookmark {
    File file;
    int line;
    String proto;

    String returnType;
    String name;
    ArrayList<String> parameters = new ArrayList<String>();

    public FunctionBookmark(File f, int l, String p) {
        file = f;
        line = l;
        proto = p.trim();
        parsePrototype();
    }

    public String simplify(String in) {
        String out = in.trim();
        String rep = out.replaceAll("\\s\\s", " ");
        while (!rep.equals(out)) {
            out = rep;
            rep = out.replaceAll("\\s\\s", " ");
        }
        return out;
    }

    public void parsePrototype() {
        Pattern p = Pattern.compile("^(.*)\\((.*)\\)$");
        Matcher m = p.matcher(proto);
        if (m.find()) {
            String def = simplify(m.group(1));
            String parms = simplify(m.group(2));
            String[] spl = def.split(" ");
            int num = spl.length;
            name = spl[num-1];
            returnType = "";
            for (int i = 0; i < num-1; i++) {
                if (i != 0) {
                    returnType += " ";
                }
                returnType += spl[i];
            }

            spl = parms.split(",");
            for (String parm : spl) {
                parameters.add(simplify(parm));
            }

        }
    }

    public String getName() {
        return name;
    }

    public String formatted() {
        StringBuilder sb = new StringBuilder();
        sb.append(returnType);
        sb.append(" ");
        sb.append(name);
        sb.append("(");
        boolean first = true;
        for (String parm : parameters) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(parm);
            first = false;
        }
        sb.append(")");
        return sb.toString();
    }

    public String toString() {
        return proto;
    }

    public File getFile() {
        return file;
    }

    public int getLine() {
        return line;
    }

    public String getFunction() {
        return proto;
    }
}

