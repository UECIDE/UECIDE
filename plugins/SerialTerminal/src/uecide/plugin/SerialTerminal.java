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

import say.swing.*;


public class SerialTerminal extends BasePlugin implements MessageConsumer
{
    JFrame win;
    JTerminal term;
    Serial port;
    JComboBox<String> baudRates;
    JCheckBox showCursor;
    JScrollBar scrollbackBar;

    JTextField fontSizeField;
    JTextField widthField;
    JTextField heightField;

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

        int width = 80;
        int height = 24;

        try {
            height = Integer.parseInt(Preferences.get("serial.height"));
        } catch (Exception e) {
            height = 24;
        }

        try {
            width = Integer.parseInt(Preferences.get("serial.width"));
        } catch (Exception e) {
            width = 80;
        }

        term.setSize(new Dimension(width, height));

        line.add(term);
        scrollbackBar = new JScrollBar(JScrollBar.VERTICAL);
        scrollbackBar.setMinimum(height);
        scrollbackBar.setMaximum(2000 + height);
        scrollbackBar.setValue(2000);
        scrollbackBar.setVisibleAmount(height);
        scrollbackBar.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                term.setScrollbackPosition(2000 - scrollbackBar.getValue());
            }
        });
        line.add(scrollbackBar);
        box.add(line);
        
        line = Box.createHorizontalBox();

        line.add(Box.createHorizontalGlue());

        JLabel label = new JLabel(Translate.t("Baud Rate") + ": ");
        line.add(label);
        baudRates = new JComboBox<String>(new String[] { "300", "1200", "2400", "4800", "9600", "14400", "28800", "38400", "57600", "115200", "230400", "460800", "500000", "576000", "1000000", "1152000"});
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
                        editor.message("Unable to release port\n", 2);
                    }
                    try {
                        port = new Serial(baudRate);
                        port.addListener(term);
                    } catch (Exception e) {
                        editor.message("Unable to reopen port: " + e.getMessage() + "\n", 2);
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
            editor.message("Unable to open serial port: " + e.getMessage() + "\n", 2);
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

    public void populatePreferences(JPanel p) {
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        c.gridheight = 1;

        JLabel label = new JLabel("Serial font: ");
        c.gridx = 0;
        c.gridy = 0;
        p.add(label, c);

        fontSizeField = new JTextField(40);
        c.gridx = 0;
        c.gridy = 1;
        p.add(fontSizeField, c);
        fontSizeField.setEditable(false);

        JButton selectSerialFont = new JButton(Translate.t("Select Font..."));
        c.gridx = 1;
        c.gridy = 1;
        p.add(selectSerialFont, c);

        Font serialFont = Preferences.getFont("serial.font");
        fontSizeField.setText(Preferences.fontToString(serialFont));

        final Container parent = p;
        selectSerialFont.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFontChooser fc = new JFontChooser(true);
                int res = fc.showDialog(parent);
                if (res == JFontChooser.OK_OPTION) {
                    Font f = fc.getSelectedFont();
                    fontSizeField.setText(Preferences.fontToString(f));
                }
            }
        });

        fontSizeField.setText(Preferences.get("serial.font"));

        label = new JLabel("Display size: ");
        c.gridx = 0;
        c.gridy++;
        p.add(label, c);

        c.gridx = 0;
        c.gridy++;
        Box b = Box.createHorizontalBox();
        widthField = new JTextField(4);
        heightField = new JTextField(4);
        int w = 80;
        int h = 24;
        try {
            w = Integer.parseInt(Preferences.get("serial.width"));
        } catch (Exception e) {
            w = 80;
        }
        try {
            h = Integer.parseInt(Preferences.get("serial.height"));
        } catch (Exception e) {
            h = 24;
        }
        widthField.setText(Integer.toString(w));
        heightField.setText(Integer.toString(h));
        b.add(widthField);
        label = new JLabel(" x ");
        b.add(label);
        b.add(heightField);
        b.add(Box.createHorizontalGlue());
        p.add(b, c);

        Dimension s = widthField.getMaximumSize();
        s.width = 40;
        widthField.setMaximumSize(s);
        s = heightField.getMaximumSize();
        s.width = 40;
        heightField.setMaximumSize(s);
    }

    public void savePreferences() {
        Preferences.set("serial.font", fontSizeField.getText());
        int w = 80;
        int h = 24;
        try {
            w = Integer.parseInt(widthField.getText().trim());
        } catch (Exception e) {
            w = 80;
        }
        try {
            h = Integer.parseInt(heightField.getText().trim());
        } catch (Exception e) {
            h = 80;
        }

        Preferences.set("serial.width", Integer.toString(w));
        Preferences.set("serial.height", Integer.toString(h));
            
    }
}

