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

import org.uecide.editors.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import say.swing.*;
import org.json.simple.*;
import java.beans.*;
import javax.imageio.*;

/*! The About class provides an About box with the logo, license, list of
 *  contributors, etc.
 */
public class About {

    JDialog frame;
    JPanel mainContainer;
    JScrollPane infoScroll;
    Editor editor;
    BufferedImage image;

    JTextPane info;

    public About(Editor e) {
        editor = e;
        frame = new JDialog(editor, JDialog.ModalityType.APPLICATION_MODAL);
        mainContainer = new JPanel();
        mainContainer.setLayout(new BorderLayout());
        frame.add(mainContainer);

        int imageWidth = 0;

        try {
            URL loc = About.class.getResource("/org/uecide/icons/about.png");
            image = ImageIO.read(loc);
            imageWidth = image.getWidth();
            JLabel picLabel = new JLabel(new ImageIcon(image));

            mainContainer.add(picLabel, BorderLayout.NORTH);
        } catch(Exception ex) {
            Base.error(ex);
        }


        infoScroll = new JScrollPane();
        infoScroll.setPreferredSize(new Dimension(imageWidth, 150));

        info = new JTextPane();
        info.setContentType("text/html");
        infoScroll.setViewportView(info);


        info.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        info.setEditable(false);
//        info.setBackground(new Color(0, 0, 0));
//        info.setForeground(new Color(0, 255, 0));
//        Font f = info.getFont();

        info.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if (e.getDescription().equals("uecide://close")) {
                        frame.dispose();
                        return;
                    }
                    Base.openURL(e.getURL().toString());
                }
            }
        });

        HTMLEditorKit kit = new HTMLEditorKit();
        info.setEditorKit(kit);
//        StyleSheet css = kit.getStyleSheet();

//        css.addRule("body {color: #88ff88; font-family: Arial,Helvetica,Sans-Serif;}");
//        css.addRule("a {color: #88ffff;}");
//        css.addRule("a:visited {color: #00aaaa;}");
        Document doc = kit.createDefaultDocument();
        info.setDocument(doc);

        info.setText(generateInfoData());

        info.setCaretPosition(0);
        mainContainer.add(infoScroll, BorderLayout.CENTER);

        frame.pack();

        Dimension mySize = frame.getSize();

        Dimension eSize = editor.getSize();
        Point ePos = editor.getLocation();
        frame.setLocation(new Point(
                              ePos.x + (eSize.width / 2) - mySize.width / 2,
                              ePos.y + (eSize.height / 2) - mySize.height / 2
                          ));

        frame.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent ev) {
            }
            public void keyPressed(KeyEvent ev) {
                if(ev.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    frame.dispose();
                }
            }
            public void keyReleased(KeyEvent ev) {
            }
        });

        frame.setVisible(true);
    }

    public void appendTextFile(StringBuilder s, String res) {
        URL u = About.class.getResource(res);
    
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(u.openStream()));
            String cont = "";

            while((cont = reader.readLine()) != null) {
                s.append(cont);
                s.append("\n");
            }
            reader.close();
        } catch(Exception ex) {
            Base.error(ex);
        }
    }

    public String generateInfoData() {
        StringBuilder s = new StringBuilder();
        URL contributors = About.class.getResource("/org/uecide/contributors.txt");

        s.append("<html><body>");

        s.append("<h1>UECIDE</h1>");
        s.append("<h4>Version " + Base.systemVersion + "</h4>");
        s.append("<br/>");
        s.append("<h4>Contributors:</h4><ul>");

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(contributors.openStream()));
            String cont = "";

            while((cont = reader.readLine()) != null) {
                Pattern p = Pattern.compile("\\d+\\s+(.*)");
                Matcher m = p.matcher(cont);

                if(m.find()) {
                    s.append("<li>");
                    s.append(m.group(1));
                    s.append("</li>");
                }
            }
            reader.close();
        } catch(Exception ex) {
            Base.error(ex);
        }

        s.append("</ul><br/>");

        appendTextFile(s, "/org/uecide/license.html");
        s.append("<br/>");
        s.append("<h4>Third party libraries and plugins:</h4>");
        s.append("<br/>");
        appendTextFile(s, "/org/uecide/thirdparty.html");
        s.append("<br/>");
        s.append("<p><a href='uecide://close'>Click to close this window.</a></p><br/>");

        s.append("</body></html>");

        return s.toString();
    }
}
