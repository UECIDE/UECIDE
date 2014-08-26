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
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.Method;

import javax.swing.*;
import javax.swing.border.*;

import say.swing.*;

public class Theme extends PropertyFile {

    public static TreeMap<String, PropertyFile>themeList = new TreeMap<String, PropertyFile>();

    public Theme(String u) {
        super(u);
    }

    public Theme(File u) {
        super(u);
    }

    public Theme(File u, String d) {
        super(u, d);
    }

    public Theme(File u, File d) {
        super(u, d);
    }

    public void loadNewTheme(File f) {
        loadNewUserFile(f);
//        for (Editor e : Base.editors) {
//            e.applyPreferences();
//        }
    }

    public static void loadThemeList() {
        File sysThemes = Base.getSystemThemesFolder();
        File userThemes = Base.getUserThemesFolder();

        if(sysThemes.exists() && sysThemes.isDirectory()) {
            String fl[] = sysThemes.list();

            for(String f : fl) {
                PropertyFile nf = new PropertyFile(new File(f));
                themeList.put(nf.get("name"), nf);
            }
        }

        if(userThemes.exists() && userThemes.isDirectory()) {
            String fl[] = userThemes.list();

            for(String f : fl) {
                PropertyFile nf = new PropertyFile(new File(f));
                themeList.put(nf.get("name"), nf);
            }
        }
    }
}
