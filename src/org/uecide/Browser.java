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

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceInfo;

public class Browser extends JTextPane implements HyperlinkListener {

    File root;
    File currentPage;

    HTMLEditorKit editorKit;
    StyleSheet css;
    HTMLDocument browserDoc;

    Stack<File> history = new Stack<File>();

    final static String tags[] = {
        "body", "a", "ul", "ol", "li", "h1", "h2", "h3", "h4", "a:hover"
    };

    public Browser(File r) {
        super();
        root = r;
        setContentType("text/html");
        setEditable(false);

        addHyperlinkListener(this);

        editorKit = new HTMLEditorKit();
        css = editorKit.getStyleSheet();
        browserDoc = (HTMLDocument)editorKit.createDefaultDocument();

        refreshTheme();
//        editorKit.setStyleSheet(css);
        setEditorKit(editorKit);
        navigate("/index.html");
    }

    public void refreshTheme() {
        String theme = Base.preferences.get("theme.selected", "default");
        theme = "theme." + theme + ".";

        for (String t : tags) {
            String data = Base.theme.get(theme + "browser." + t, Base.theme.get("theme.default.browser." + t));
            //css.addRule(t + " {" + data + "}");
        }
    }

    public void navigate(String url) {
        File from = null;
        if (url.startsWith("/")) {
            from = new File(root, url);
        } else {
            if (currentPage == null) {
                from = new File(root, url);
            } else {
                from = new File(currentPage.getParentFile(), url);
            }
        }

        if (from.isDirectory()) {
            from = new File(from, "index.html");
        }

        navigate(from);
    }

    public String craftPage(String content) {
        String theme = Base.preferences.get("theme.selected", "default");
        theme = "theme." + theme + ".";
        StringBuilder sb = new StringBuilder();

        sb.append("<html>\n");
        sb.append("<head>\n");
        sb.append("<style>\n");

        PropertyFile themeData = Base.theme.getChildren(theme + "browser");
        PropertyFile defaultData = Base.theme.getChildren("theme.default.browser");
        ArrayList<String> done = new ArrayList<String>();

        for (Object keyo : themeData.keySet()) {
            String key = (String)keyo;
            String data = themeData.get(key);
            sb.append(key + " {" + data + "}\n");
            done.add(key);
        }

        for (Object keyo : defaultData.keySet()) {
            String key = (String)keyo;
            if (done.indexOf(key) == -1) {
                String data = defaultData.get(key);
                sb.append(key + " {" + data + "}\n");
            }
        }

        sb.append("</style>\n");
        sb.append("</head>\n");
        sb.append("<body>\n");
        sb.append("<div class='uecideBrowserMenu'>[ <a href='/'>Home</a> | <a href='back://'>Back</a> ]</div><br/>");
        sb.append(content);
        sb.append("</body>\n");
        sb.append("</html>\n");
        return sb.toString();
    }

    public void navigate(File from) {

        if (from.exists()) {

            history.push(currentPage);
            currentPage = from;

            
            InputStream    fis;
            BufferedReader br;
            String         line;
            StringBuilder sb = new StringBuilder();

            try {

                fis = new FileInputStream(from);
                br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));

                while((line = br.readLine()) != null) {
                    sb.append(line + "\n");
                }

                br.close();
                String page = sb.toString();
                Pattern p = Pattern.compile("<body>(.*)</body>", Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(page);
                if (m.find()) {
                    page = m.group(1);
                }
                clearPage();

                String text = craftPage(page);
                setText(text);
            } catch (Exception e) {
                Base.error(e);
            }
        } else {
            clearPage();
            setText(craftPage("<h1>Manual not available</h1><p>The manual is not available for this core.</p>"));
        }

        try {
            setCaretPosition(0);
        } catch (Exception e) {
        }
    }

    public void appendToPage(String s) {
        try {
            editorKit.insertHTML(browserDoc, browserDoc.getLength(), s, 0, 0, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        setCaretPosition(0);
    }


    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            if (e.getDescription().equals("back://")) {
                if (history.size() > 0) {
                    File h = history.pop();
                    navigate(h);
                }
                return;
            }
            if (e.getDescription().startsWith("http://")) {
                Base.openURL(e.getDescription());
                return;
            }
            if (e.getDescription().startsWith("https://")) {
                Base.openURL(e.getDescription());
                return;
            }
            if (e.getDescription().startsWith("ftp://")) {
                Base.openURL(e.getDescription());
                return;
            }
            navigate(e.getDescription());
        }
    }

    public void home() {
        navigate("/index.html");
    }

    public void setRoot(File f) {
        root = f;
        home();
    }

    public void clearPage() {
        try {
            browserDoc.remove(0, browserDoc.getLength());
        } catch(BadLocationException e) {
        }
    }
}
