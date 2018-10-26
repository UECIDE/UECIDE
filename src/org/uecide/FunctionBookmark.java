/*
 * Copyright (c) 2015, Majenko Technologies
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

    String returnType;
    String name;
    String paramList;
    String parentClass;

    int type;

    public static final int FUNCTION = 0;
    public static final int VARIABLE = 1;
    public static final int MEMBER_FUNCTION = 2;
    public static final int MEMBER_VARIABLE = 3;
    public static final int DEFINE = 4;
    public static final int CLASS = 5;

    public FunctionBookmark(int t, File f, int l, String n, String rt, String pl, String pc) {
        type = t;
        file = f;
        line = l;
        name = n != null ? n.trim() : null;
        returnType = rt != null ? rt.trim() : null;
        paramList = pl != null ? pl.trim() : null;
        parentClass = pc != null ? pc.trim() : null;
    }

    public boolean isFunction() {
        return type == FUNCTION;
    }

    public boolean isVariable() {
        return type == VARIABLE;
    }

    public boolean isMemberFunction() {
        return type == MEMBER_FUNCTION;
    }

    public boolean isMemberVariable() {
        return type == MEMBER_VARIABLE;
    }

    public boolean isDefine() {
        return type == DEFINE;
    }

    public boolean isClass() {
        return type == CLASS;
    }
    
    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String formatted() {
        switch (type) {
            case FUNCTION:
                return returnType + " " + name + paramList;
            case VARIABLE:
                return returnType + " " + name;
            case MEMBER_FUNCTION:
                return returnType + " " + parentClass + "::" + name + paramList;
            case MEMBER_VARIABLE:
                return returnType + " " + parentClass + "::" + name;
            case DEFINE:
                return "#define " + name;
            case CLASS:
                return "class " + name;
        }
        return name;
    }

    public String briefFormatted() {
        switch (type) {
            case FUNCTION:
                return name + paramList;
            case VARIABLE:
                return name;
            case MEMBER_FUNCTION:
                return parentClass + "::" + name + paramList;
            case MEMBER_VARIABLE:
                return parentClass + "::" + name;
            case DEFINE:
                return name;
            case CLASS:
                return name;
        }
        return name;
    }

    public String toString() {
        return formatted().trim();
    }

    public File getFile() {
        return file;
    }

    public int getLine() {
        return line;
    }

    public String getFunction() {
        return formatted().trim();
    }

    public String getVariable() {
        return formatted().trim();
    }

    public String getMember() {
        return formatted().trim();
    }

    public String getDefine() {
        return formatted().trim();
    }

    public String dump() {
        return formatted().trim() + " @ " + file.getAbsolutePath() + " line " + line + " type " + type;
    }

    public String getParentClass() {
        return parentClass;
    }
}

