package uecide.plugin;

import uecide.app.*;
import uecide.app.debug.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;


public class SerialTerminal extends BasePlugin implements MessageConsumer
{
    JFrame win;
    JTerminal term;
    Serial port;
    JComboBox baudRates;
    JCheckBox showCursor;
    JScrollBar scrollbackBar;

    int baudRate;

    boolean ready = false;

    static boolean isOpen = false;

    public void init(Editor editor)
    {
        this.editor = editor;
    }

    public void run()
    {
        if (SerialTerminal.isOpen) {
            return;
        }
        SerialTerminal.isOpen = true;
        win = new JFrame(Translate.t("Serial Terminal"));
        win.getContentPane().setLayout(new BorderLayout());
        win.setResizable(false);

        Box box = Box.createVerticalBox();

        Box line = Box.createHorizontalBox();

        term = new JTerminal();
        Font f = Preferences.getFont("serial.font");
        if (f == null) {
            f = new Font("Monospaced", Font.PLAIN, 12);
            Preferences.set("serial.font", "Monospaced,plain,12");
        }
        term.setFont(f);
        term.setKeypressConsumer(this);
        term.boxCursor(true);

        line.add(term);
        scrollbackBar = new JScrollBar(JScrollBar.VERTICAL);
        scrollbackBar.setMinimum(24);
        scrollbackBar.setMaximum(2010);
        scrollbackBar.setValue(2000);
        scrollbackBar.setVisibleAmount(24);
        scrollbackBar.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                System.err.println(scrollbackBar.getValue() + " = " + (2000 - scrollbackBar.getValue()));
                term.setScrollbackPosition(2000 - scrollbackBar.getValue());
            }
        });
        line.add(scrollbackBar);
        box.add(line);
        
        line = Box.createHorizontalBox();

        line.add(Box.createHorizontalGlue());

        JLabel label = new JLabel(Translate.t("Baud Rate") + ": ");
        line.add(label);
        baudRates = new JComboBox(new String[] { "300", "1200", "2400", "4800", "9600", "14400", "28800", "38400", "57600", "115200", "230400", "460800", "500000", "576000", "1000000", "1152000"});
        baudRates.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (ready) {
                    String value = (String) baudRates.getSelectedItem();
                    baudRate = Integer.parseInt(value);
                    Preferences.set("serial.debug_rate", value);
                    try {
                        if (port != null) {
                            port.dispose();
                            port = null;
                        }
                    } catch (Exception e) {
                        System.err.println("Unable to release port");
                    }
                    try {
                        port = new Serial(baudRate);
                        port.addListener(term);
                    } catch (Exception e) {
                        System.err.println("Unable to reopen port");
                    }
                }
            }
        });

        line.add(baudRates);

        showCursor = new JCheckBox(Translate.t("Show Cursor"));
        showCursor.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (ready) {
                    term.showCursor(showCursor.isSelected());
                    Preferences.setBoolean("serial.debug_cursor", showCursor.isSelected());
                }
            }
        });
        
        line.add(showCursor);
        box.add(line);

        win.getContentPane().add(box);
        win.pack();

        Dimension size = win.getSize();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        win.setLocation((screen.width - size.width) / 2,
                          (screen.height - size.height) / 2);

        win.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        win.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                close();
            }
        });
//        Base.registerWindowCloseKeys(win.getRootPane(), new ActionListener() {
//            public void actionPerformed(ActionEvent actionEvent) {
//                close();
//            }
//        });
        Base.setIcon(win);

        term.clearScreen();
        try {
            baudRate = Integer.parseInt(Preferences.get("serial.debug_rate"));
            baudRates.setSelectedItem(Preferences.get("serial.debug_rate"));
            port = new Serial(baudRate);
        } catch(Exception e) {
            System.err.println("Unable to open serial port");
            return;
        }
        showCursor.setSelected(Preferences.getBoolean("serial.debug_cursor"));
        term.showCursor(Preferences.getBoolean("serial.debug_cursor"));
        port.addListener(term);
        win.setVisible(true);
        ready = true;
    }

    public void close()
    {
        port.dispose();
        win.dispose();
        ready = false;
        SerialTerminal.isOpen = false;
    }

    public String getMenuTitle()
    {
        return(Translate.t("Serial Terminal"));
    }

    public void message(String m) {
        if (port == null) {
            win.setVisible(false);
            return;
        }
        port.write(m);
    }
    
    public void message(String m, int c) {
        message(m);
    }

    public ImageIcon toolbarIcon() {
        ImageIcon icon = new ImageIcon(getResourceURL("uecide/plugin/SerialTerminal/console.png"));
        return icon;
    }

}

