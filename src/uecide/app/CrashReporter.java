package uecide.app;

import uecide.app.debug.*;
import uecide.app.editors.*;
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
            mainContainer = new JPanel();
            mainContainer.setLayout(new BorderLayout());
            frame.add(mainContainer);

            infoScroll = new JScrollPane();
            infoScroll.setPreferredSize(new Dimension(400, 500));

            info = new JTextPane();
            info.setContentType("text/html");
            infoScroll.setViewportView(info);


            info.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            info.setEditable(false);
            info.setBackground(new Color(0, 0, 0));
            info.setForeground(new Color(0, 255, 0));
            Font f = info.getFont();

            info.addHyperlinkListener(new HyperlinkListener() {
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        if (e.getDescription().equals("uecide://close")) {
                            System.exit(10);
                            frame.dispose();
                            return;
                        }
                        if (e.getDescription().equals("uecide://ignore")) {
                            frame.dispose();
                            return;
                        }
                        if (e.getDescription().equals("uecide://report")) {
                            reportError();
                            return;
                        }
                        Base.openURL(e.getURL().toString());
                    }
                }
            });

            HTMLEditorKit kit = new HTMLEditorKit();
            info.setEditorKit(kit);
            StyleSheet css = kit.getStyleSheet();

            css.addRule("body {color: #88ff88; font-family: Arial,Helvetica,Sans-Serif;}");
            css.addRule("a {color: #88ffff;}");
            css.addRule("a:visited {color: #00aaaa;}");
            Document doc = kit.createDefaultDocument();
            info.setDocument(doc);

            info.setText(generateInfoData());

            info.setCaretPosition(0);
            mainContainer.add(infoScroll, BorderLayout.CENTER);

            frame.pack();

            Dimension mySize = frame.getSize();

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

            JPanel bp = new JPanel();
            bp.setLayout(new FlowLayout());

            JButton q = new JButton("Quit");
            q.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.exit(10);
                }
            });
            bp.add(q);

            JButton i = new JButton("Ignore");
            i.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    frame.dispose();
                    return;
                }
            });
            bp.add(i);

            JButton r = new JButton("Report");
            r.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    reportError();
                }
            });
            bp.add(r);

            mainContainer.add(bp, BorderLayout.SOUTH);

            frame.setVisible(true);
        }
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
        URL contributors = About.class.getResource("/uecide/app/contributors.txt");

        s.append("<html><body>");

        s.append("<h1>" + Base.theme.get("product.cap") + "</h1>");
        s.append("<h4>Version " + Base.systemVersion);
        s.append(" Build number " + Base.BUILDNO + "</h4>");
        s.append("<br/>");
        s.append("<p>A serious error has occurred.  The detail of the error is shown below.  You can try and continue past this error, but things might not work right, or you can quit and try running again (any unsaved work will be lost).  Alternatively, you can report the error to Majenko Technologies and we will try and resolve the problem.</p>");
        s.append("<p><b>Summary:</b> " + exception.toString() + "</p>");
        s.append("<p><b>Cause:</b> " + exception.getCause() + "</p>");
        s.append("<p><b>Message:</b> " + exception.getMessage() + "</p>");

        StackTraceElement[] els = exception.getStackTrace();
        int tp = 0;
        for (StackTraceElement e : els) {
            s.append("<h3>Trace point " + tp + " in class " + e.getClassName() + ":</h3>");
            s.append("<ul>");
            s.append("<li>File: " + e.getFileName() + "</li>");
            s.append("<li>Line: " + e.getLineNumber() + "</li>");
            s.append("<li>Method: " + e.getMethodName() + "</li>");
            s.append("<li>Native: " + e.isNativeMethod() + "</li>");
            tp++;
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        String o = sw.toString();
        s.append("<h3>Full text of the exception:</h3>");
        s.append("<pre>");
        s.append(o);
        s.append("</pre>");
        s.append("<br/>");
        s.append("</body></html>");

        return s.toString();
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
                    connection.disconnect();
                    if (resp == 200) {
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
