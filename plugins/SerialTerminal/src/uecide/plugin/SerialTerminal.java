package uecide.plugin;

import uecide.app.*;
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
import java.awt.*;
import java.awt.event.*;

import jssc.*;

import say.swing.*;


public class SerialTerminal extends Plugin implements SerialPortEventListener,MessageConsumer
{
    public static HashMap<String, String> pluginInfo = null;
    public static URLClassLoader loader = null;
    public static void setLoader(URLClassLoader l) { loader = l; }
    public static void setInfo(HashMap<String, String>info) { pluginInfo = info; }
    public static String getInfo(String item) { return pluginInfo.get(item); }

    JFrame win = null;
    JTerminal term;
    SerialPort port;
    JComboBox baudRates;
    JCheckBox showCursor;
    JCheckBox localEcho;
    JCheckBox lineEntry;
    JScrollBar scrollbackBar;

    static JTextField fontSizeField;
    static JTextField widthField;
    static JTextField heightField;
    static JCheckBox  autoCrIn;
    static JCheckBox  autoCrOut;

    JTextField lineEntryBox;
    JComboBox lineEndings;
    JButton lineSubmit;

    String serialPort;

    Box entryLineArea;

    int baudRate;

    boolean ready = false;


    public SerialTerminal(Editor e) { editor = e; }
    public SerialTerminal(EditorBase e) { editorTab = e; }


    public void run()
    {
        if (win != null) {
            close();
        }
        serialPort = editor.getSerialPort();

        Debug.message(this + ": Opening serial terminal on port " + serialPort);
        win = new JFrame(Translate.t("Serial Terminal"));
        win.getContentPane().setLayout(new BorderLayout());
        win.setResizable(false);

        Box box = Box.createVerticalBox();

        Box line = Box.createHorizontalBox();

        term = new JTerminal();
        Font f = Base.preferences.getFont("serial.font");
        if (f == null) {
            f = new Font("Monospaced", Font.PLAIN, 12);
            Base.preferences.set("serial.font", "Monospaced,plain,12");
        }
        term.setFont(f);
        term.setKeypressConsumer(this);
        term.boxCursor(true);

        int width = 80;
        int height = 24;

        try {
            height = Integer.parseInt(Base.preferences.get("serial.height"));
        } catch (Exception e) {
            height = 24;
        }

        try {
            width = Integer.parseInt(Base.preferences.get("serial.width"));
        } catch (Exception e) {
            width = 80;
        }

        term.setSize(new Dimension(width, height));
        term.setAutoCr(Base.preferences.getBoolean("serial.autocr_in"));

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
        baudRates = new JComboBox(new String[] { "300", "1200", "2400", "4800", "9600", "14400", "19200", "28800", "38400", "57600", "115200", "230400", "460800", "500000", "576000", "1000000", "1152000"});
        final SerialPortEventListener mc = this;
        baudRates.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (ready) {
                    String value = (String) baudRates.getSelectedItem();
                    baudRate = Integer.parseInt(value);
                    Base.preferences.set("serial.debug_rate", value);
                    try {
                        if (port != null) {
                            if (port.isOpened()) {
                                Serial.closePort(port);
                            }
                            port = null;
                        }
                    } catch (Exception e) {
                        editor.error("Unable to release port");
                    }
                    try {
                        Debug.message(this + ": Change baud rate " + serialPort);
                        port = Serial.requestPort(serialPort, baudRate);
                        if (port == null) {
                            editor.error("Unable to reopen port");
                            return;
                        }
                        port.addEventListener(mc);
                        term.setDisconnected(false);
                    } catch (Exception e) {
                        editor.error("Unable to reopen port:");
                        editor.error(e);
                    }
                }
            }
        });

        line.add(baudRates);

        localEcho = new JCheckBox(Translate.t("Local Echo"));

        final JFrame subwin = win;
        
        lineEntry = new JCheckBox(Translate.t("Line Entry"));
        lineEntry.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                entryLineArea.setVisible(lineEntry.isSelected());
                subwin.pack();
                subwin.repaint();
                lineEntryBox.requestFocusInWindow();
            }
        });

        line.add(lineEntry);

        showCursor = new JCheckBox(Translate.t("Show Cursor"));
        showCursor.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (ready) {
                    term.showCursor(showCursor.isSelected());
                    Base.preferences.setBoolean("serial.debug_cursor", showCursor.isSelected());
                }
            }
        });
        
        line.add(localEcho);
        line.add(showCursor);
        box.add(line);

        entryLineArea = Box.createHorizontalBox();

        ActionListener al = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    port.writeString(lineEntryBox.getText());
                    if (((String)lineEndings.getSelectedItem()).equals("Carriage Return")) {
                        port.writeString("\r");
                    }
                    if (((String)lineEndings.getSelectedItem()).equals("Line Feed")) {
                        port.writeString("\n");
                    }
                    if (((String)lineEndings.getSelectedItem()).equals("CR + LF")) {
                        port.writeString("\r\n");
                    }
                    lineEntryBox.setText("");
                    lineEntryBox.requestFocusInWindow();
                } catch (Exception ex) {
                    Base.error(ex);
                }
            }
        };

        entryLineArea.setVisible(false);
        lineEntryBox = new JTextField();
        lineEntryBox.setBackground(new Color(255, 255, 255));
        lineEntryBox.addActionListener(al);

        lineEndings = new JComboBox(new String[] {"None", "Carriage Return", "Line Feed", "CR + LF"});
        lineSubmit = new JButton("Send");
        lineSubmit.addActionListener(al);
                
        entryLineArea.add(lineEntryBox);
        entryLineArea.add(lineSubmit);
        entryLineArea.add(lineEndings);
        box.add(entryLineArea);

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
        Base.setIcon(win);

        term.clearScreen();
        try {
            baudRate = Base.preferences.getInteger("serial.debug_rate");
            baudRates.setSelectedItem(Base.preferences.get("serial.debug_rate"));
            Debug.message(this + ": Open port " + serialPort);
            port = Serial.requestPort(serialPort, baudRate);
            if (port == null) {
                editor.error("Unable to open serial port");
                return;
            }
                
            term.setDisconnected(false);
        } catch(Exception e) {
            editor.error("Unable to open serial port:");
            editor.error(e);
            return;
        }
        showCursor.setSelected(Base.preferences.getBoolean("serial.debug_cursor"));
        term.showCursor(Base.preferences.getBoolean("serial.debug_cursor"));
        try {
            port.addEventListener(this);
        } catch (Exception e) {
            Base.error(e);
        }
        win.setTitle(Translate.t("Serial Terminal") + " :: " + serialPort);
        win.setVisible(true);
        ready = true;
    }

    public void close()
    {
        ready = false;
        for( ActionListener al : baudRates.getActionListeners() ) {
            baudRates.removeActionListener( al );
        }
        if (port != null) {
            if (port.isOpened()) {
                try {
                    Serial.closePort(port);
                } catch (Exception e) {
                    editor.error(e);
                }
            }
            port = null;
        }
        win.dispose();
        Debug.message(this + ": Closing serial terminal on port " + serialPort);
    }

    public void warning(String m) { editor.warning(m); }
    public void error(String m) { editor.error(m); }

    public void message(String m) {
        if (port == null) {
            win.setVisible(false);
            return;
        }
        if (Base.preferences.getBoolean("serial.autocr_out")) {
            m = m.replace("\n", "\r\n");
        }

        if (localEcho.isSelected()) {
            term.message(m);
        }
        try {
            port.writeString(m);
        } catch (Exception e) {
            Base.error(e);
        }
    }
    
    public void addToolbarButtons(JToolBar toolbar, int flags) {
        if (flags == Plugin.TOOLBAR_EDITOR) {
            JButton b = new JButton(Base.loadIconFromResource("uecide/plugin/SerialTerminal/console.png", loader));
            b.setToolTipText("Serial Terminal");
            b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    run();
                }
            });
            toolbar.add(b);
        }
    }

    public static void populatePreferences(JPanel p) {
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

        Font serialFont = Base.preferences.getFont("serial.font");
        fontSizeField.setText(Base.preferences.fontToString(serialFont));

        final Container parent = p;
        selectSerialFont.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFontChooser fc = new JFontChooser();
                fc.setSelectedFont(Base.preferences.stringToFont(fontSizeField.getText()));
                int res = fc.showDialog(parent);
                if (res == JFontChooser.OK_OPTION) {
                    Font f = fc.getSelectedFont();
                    fontSizeField.setText(Base.preferences.fontToString(f));
                }
            }
        });

        fontSizeField.setText(Base.preferences.get("serial.font"));

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
            w = Integer.parseInt(Base.preferences.get("serial.width"));
        } catch (Exception e) {
            w = 80;
        }
        try {
            h = Integer.parseInt(Base.preferences.get("serial.height"));
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

        c.gridy++;
        c.gridx = 0;
        JLabel tl = new JLabel(Translate.t("Add CR to LF: "));
        p.add(tl, c);
        c.gridx++;
        autoCrIn = new JCheckBox(Translate.t("Incoming"));
        autoCrIn.setSelected(Base.preferences.getBoolean("serial.autocr_in"));
        p.add(autoCrIn, c);
        c.gridx++;
        autoCrOut = new JCheckBox(Translate.t("Outgoing"));
        autoCrOut.setSelected(Base.preferences.getBoolean("serial.autocr_out"));
        p.add(autoCrOut, c);
    }

    public static void savePreferences() {
        Base.preferences.set("serial.font", fontSizeField.getText());
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
            h = 24;
        }

        Base.preferences.set("serial.width", Integer.toString(w));
        Base.preferences.set("serial.height", Integer.toString(h));

        Base.preferences.setBoolean("serial.autocr_in", autoCrIn.isSelected());
        Base.preferences.setBoolean("serial.autocr_out", autoCrOut.isSelected());
            
    }

    public void releasePort(String portName) {
        if (port == null) {
            return;
        }
        if (portName == null) {
            return;
        }
        if (portName.equals(serialPort)) {
            close();
        }
    }

    public void serialEvent(SerialPortEvent e) {
        if (e.isRXCHAR()) {
            try {
                term.message(port.readString());
            } catch (Exception ex) {
                editor.error(ex);
            }
        }
    }

    public void populateMenu(JMenu menu, int flags) {
        if (flags == (Plugin.MENU_TOOLS | Plugin.MENU_MID)) {
            JMenuItem item = new JMenuItem("Serial Terminal");
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    run();
                }
            });
            menu.add(item);
        }
    }

    public static String getPreferencesTitle() {
        return "Serial Terminal";
    }
}

