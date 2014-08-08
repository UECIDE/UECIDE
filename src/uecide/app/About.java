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

    JTextArea info;

    public About(Editor e) {
        editor = e;
        frame = new JDialog(editor, JDialog.ModalityType.APPLICATION_MODAL);
        mainContainer = new JPanel();
        mainContainer.setLayout(new BorderLayout());
        frame.add(mainContainer);

        int imageWidth = 0;

        try {
            URL loc = About.class.getResource("/uecide/app/icons/about.png");
            image = ImageIO.read(loc);
            imageWidth = image.getWidth();
            JLabel picLabel = new JLabel(new ImageIcon(image));

            mainContainer.add(picLabel, BorderLayout.NORTH);
        } catch(Exception ex) {
            Base.error(ex);
        }


        infoScroll = new JScrollPane();
        infoScroll.setPreferredSize(new Dimension(imageWidth, 150));

        info = new JTextArea();
        infoScroll.setViewportView(info);

        info.setText(generateInfoData());

        info.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        info.setEnabled(false);
        info.setBackground(new Color(0, 0, 0));
        info.setForeground(new Color(0, 255, 0));
        Font f = info.getFont();
        info.setFont(new Font(f.getFamily(), Font.PLAIN, 12));

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

    public String generateInfoData() {
        StringBuilder s = new StringBuilder();
        URL contributors = About.class.getResource("/uecide/app/contributors.txt");
        URL license = About.class.getResource("/uecide/app/license.txt");


        s.append(Base.theme.get("product.cap") + " version " + Base.systemVersion + "\n");
        s.append("Build number " + Base.BUILDNO + "\n");
        s.append("\n");
        s.append("Contributors:\n");

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(contributors.openStream()));
            String cont = "";

            while((cont = reader.readLine()) != null) {
                Pattern p = Pattern.compile("\\d+\\s+(.*)");
                Matcher m = p.matcher(cont);

                if(m.find()) {
                    s.append("    ");
                    s.append(m.group(1));
                    s.append("\n");
                }
            }
        } catch(Exception ex) {
            Base.error(ex);
        }

        s.append("\n");

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(license.openStream()));
            String line = "";

            while((line = reader.readLine()) != null) {
                s.append(line);
                s.append("\n");
            }
        } catch(Exception ex) {
            Base.error(ex);
        }

        s.append("\n");

        s.append("\n");
        s.append("Press <ESC> to close this window.");

        return s.toString();
    }
}
