package org.uecide;

import org.uecide.debug.*;
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
            frame.setVisible(true);
        }
    }

    public void reportError() {
        StringBuilder sb = new StringBuilder();
        sb.append("OS: " + Base.getOSFullName() + "\n");
        sb.append("Version: " + Base.systemVersion + "\n");
        sb.append("Build: " + Base.BUILDNO + "\n");

        sb.append("Exception: \n");
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        String o = sw.toString();
        sb.append(o);

        JPanel reportPanel = new JPanel();

        reportPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        reportPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        reportPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;

        JLabel lab = new JLabel("Your name: ");
        lab.setAlignmentX(Component.LEFT_ALIGNMENT);
        lab.setHorizontalAlignment(SwingConstants.LEFT);
        reportPanel.add(lab, c);
        c.gridy++;
        final JTextField yourName = new JTextField(40);
        reportPanel.add(yourName, c);
        c.gridy++;

        lab = new JLabel("Your Email: ");
        lab.setAlignmentX(Component.LEFT_ALIGNMENT);
        lab.setHorizontalAlignment(SwingConstants.LEFT);
        reportPanel.add(lab,c);
        c.gridy++;
        final JTextField yourEmail = new JTextField(40);
        reportPanel.add(yourEmail,c);
        c.gridy++;

        lab = new JLabel("Text of the report:");
        lab.setAlignmentX(Component.LEFT_ALIGNMENT);
        lab.setHorizontalAlignment(SwingConstants.LEFT);
        reportPanel.add(lab,c);
        c.gridy++;
        final JTextArea reportBody = new JTextArea(10,40);
        reportBody.setText(sb.toString());
        reportBody.setEditable(false);
        reportPanel.add(reportBody,c);
        c.gridy++;
        
        lab = new JLabel("Your comments:");
        lab.setAlignmentX(Component.LEFT_ALIGNMENT);
        lab.setHorizontalAlignment(SwingConstants.LEFT);
        reportPanel.add(lab,c);
        c.gridy++;
        final JTextArea reportComments = new JTextArea(10, 40);
        reportPanel.add(reportComments,c);
        c.gridy++;
        

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

        reportPanel.add(bb, c);
        
        frame.setContentPane(reportPanel);
        frame.pack();
    
        
    }
}
