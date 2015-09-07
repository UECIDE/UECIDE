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

public class CrashReporter {

    JDialog frame;
    JPanel mainContainer;
    JScrollPane infoScroll;
    Editor editor;
    BufferedImage image;

    Throwable exception;
    JTextPane info;

    public CrashReporter(Throwable ex) {
        exception = ex;

        if (exception.toString().equals("java.lang.NullPointerError")) {
            System.err.println("Auto-ignoring a NullPointerError");
            return;
        }

        Object[] options = {
            "Quit",
            "Ignore",
            "Report"
        };

        int n = JOptionPane.showOptionDialog(null,
            "I'm afraid UECIDE has crashed:\n" + exception.toString() +
            "\nDo you want to quit, ignore the error, or\n" +
            "examine (and send) a crash report?",
            exception.toString(),
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[2]);

        if (n == 0) {
            System.exit(10);
        }

        if (n == 1) {
            return;
        }

        if (n == 2) {
            frame = new JDialog(null, JDialog.ModalityType.APPLICATION_MODAL);
            reportError();
//            frame.setSize(400, 500);
            frame.setVisible(true);
        }
    }

    public void reportError() {
        StringBuilder sb = new StringBuilder();
        sb.append("OS: " + Base.getOSFullName() + "\n");
        sb.append("Version: " + Base.systemVersion + "\n");

        sb.append("\n\n=== EXCEPTION START ===\n\n");
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        String o = sw.toString();
        sb.append(o);
        sb.append("\n\n=== EXCEPTION END ===\n\n");
        sb.append("\n\n=== DEBUG LOG START ===\n\n");
        sb.append(Debug.getText());
        sb.append("\n\n=== DEBUG LOG END ===\n\n");

        JPanel reportPanel = new JPanel();

        reportPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        reportPanel.setLayout(new BoxLayout(reportPanel, BoxLayout.PAGE_AXIS));

        JLabel lab = new JLabel("Your name: ");
        reportPanel.add(lab);

        final JTextField yourName = new JTextField(40);
        reportPanel.add(yourName);

        lab = new JLabel("Your Email: ");
        reportPanel.add(lab);

        final JTextField yourEmail = new JTextField(40);
        reportPanel.add(yourEmail);

        lab = new JLabel("Text of the report:");
        reportPanel.add(lab);

        final JTextArea reportBody = new JTextArea(10, 50);
        reportBody.setLineWrap(true);
        reportBody.setWrapStyleWord(true);
        reportBody.setText(sb.toString());
        reportBody.setEditable(false);
        JScrollPane rbs = new JScrollPane(reportBody);
        reportPanel.add(rbs);
        
        lab = new JLabel("Your comments:");
        reportPanel.add(lab);
        
        final JTextArea reportComments = new JTextArea(10, 50);
        reportComments.setLineWrap(true);
        reportComments.setWrapStyleWord(true);
        JScrollPane rcs = new JScrollPane(reportComments);
        reportPanel.add(rcs);
        

        JPanel bb = new JPanel();
        bb.setLayout(new FlowLayout());
        JButton rep = new JButton("Send Report");
        rep.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    StringBuilder so = new StringBuilder();
                    so.append("name=" + URLEncoder.encode(yourName.getText(), "UTF-8"));
                    so.append("&email=" + URLEncoder.encode(yourEmail.getText(), "UTF-8"));
                    so.append("&comments=" + URLEncoder.encode(reportComments.getText(), "UTF-8"));
                    so.append("&body=" + URLEncoder.encode(reportBody.getText(), "UTF-8"));
                    String param = so.toString();
                
                    URL url = new URL("http://uecide.org/report.php");
                    HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    connection.setDoInput(true);
               //     connection.setRequestProperty("Content-Type", "application/x-www.form-urlencoded");
                    connection.setRequestProperty("charset", "utf-8");
                    connection.setRequestProperty("Content-Length", Integer.toString(param.getBytes().length));
                    connection.setUseCaches(false);
                    DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                    wr.writeBytes(param);
                    wr.flush();
                    wr.close();
                    int resp = connection.getResponseCode();
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    System.err.println(response);
                    connection.disconnect();
                    if (resp == 200) {
                        Base.openURL(response.toString());
                        int n = JOptionPane.showConfirmDialog(frame, "Report sent OK.\nClose UECIDE now?", "Report sent", JOptionPane.YES_NO_OPTION);
                        if (n == 0) {
                            System.exit(10);
                        } else {
                            frame.dispose();
                        }
                    } else {
                        JOptionPane.showMessageDialog(frame, "There was a problem submitting the report.\nThe remote server responded: " + resp, "Report failed", JOptionPane.ERROR_MESSAGE);
                        System.exit(10);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        bb.add(rep);
        JButton can = new JButton("Cancel");
        can.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
            }
        });
        bb.add(can);

        reportPanel.add(bb);
        
        frame.setContentPane(reportPanel);
        frame.pack();
//        frame.setSize(400, 500);
    
        
    }
}
