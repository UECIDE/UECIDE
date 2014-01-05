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
import javax.imageio.*;
import java.awt.*;
import java.awt.event.*;
import say.swing.*;
import jssc.*;


public class Grapher extends BasePlugin implements SerialPortEventListener
{
    JFrame win;
    JGrapher graph;
    SerialPort port;
    JComboBox<String> baudRates;
    JScrollBar scrollbackBar;

    JButton playPauseButton;
    ImageIcon playIcon;
    ImageIcon pauseIcon;
    boolean playPauseState = false;
    String serialPort;
    JToolBar toolbar;

    JTextField fontSizeField;
    Box box;
    Box line;

    int baudRate;

    boolean ready = false;


    public void init(Editor editor)
    {
        this.editor = editor;
        serialPort = editor.getSerialPort();
    }

    public void run()
    {
        win = new JFrame(Translate.t("Grapher"));
        win.getContentPane().setLayout(new BorderLayout());
        win.setResizable(false);

        box = Box.createVerticalBox();

        toolbar = new JToolBar();
        toolbar.setFloatable(false);

        File themeFolder = Base.getContentFile("lib/theme");
        File iconFile = new File(themeFolder, "save.png");
        ImageIcon saveIcon = new ImageIcon(iconFile.getAbsolutePath());
        JButton saveButton = new JButton(saveIcon);
        saveButton.setToolTipText(Translate.t("Save Image"));

        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveImage();
            }
        });
        toolbar.add(saveButton);

        playIcon = new ImageIcon(getResourceURL("uecide/plugin/Grapher/play.png"));
        pauseIcon = new ImageIcon(getResourceURL("uecide/plugin/Grapher/pause.png"));
        playPauseButton = new JButton(pauseIcon);
        playPauseButton.setToolTipText(Translate.t("Pause Graph"));
        playPauseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                playPause();
            }
        });
        playPauseState = false;
        toolbar.add(playPauseButton);
        box.add(toolbar);

//        line = Box.createHorizontalBox();

        graph = new JGrapher();
        Font f = Base.preferences.getFont("grapher.font");
        if (f == null) {
            f = new Font("Monospaced", Font.PLAIN, 12);
            Base.preferences.set("grapher.font", "Monospaced,plain,12");
        }
        graph.setFont(f);

//        line.add(graph);
        box.add(graph);
        
        line = Box.createHorizontalBox();

        line.add(Box.createHorizontalGlue());

        JLabel label = new JLabel(Translate.t("Baud Rate") + ": ");
        line.add(label);
        String[] baudRateList = new String[] { "300", "1200", "2400", "4800", "9600", "14400", "28800", "38400", "57600", "115200", "230400", "460800", "500000", "576000", "1000000", "1152000"};
        baudRates = new JComboBox<String>(baudRateList);
        final SerialPortEventListener mc = this;
        baudRates.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (ready) {
                    String value = (String) baudRates.getSelectedItem();
                    baudRate = Integer.parseInt(value);
                    Base.preferences.set("serial.debug_rate", value);
                    try {
                        if (port != null) {
                            Serial.releasePort(port);
                            port = null;
                        }
                    } catch (Exception e) {
                        editor.message("Unable to release port: " + e.getMessage() + "\n", 2);
                    }
                    try {
                        port = Serial.requestPort(serialPort, mc, baudRate);
                        if (port == null) {
                            editor.message("Unable to reopen port\n", 2);
                        } else {
                            port.addEventListener(mc);
                        }
                    } catch (Exception e) {
                        editor.message("Unable to reopen port: " + e.getMessage() + "\n", 2);
                    }
                }
            }
        });

        line.add(baudRates);

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
        Base.setIcon(win);

        try {
            baudRate = Base.preferences.getInteger("serial.debug_rate");
            baudRates.setSelectedItem(Base.preferences.get("serial.debug_rate"));
            port = Serial.requestPort(serialPort, this, baudRate); 
        } catch(Exception e) {
            editor.message("Unable to open serial port: " + e.getMessage() + "\n", 2);
            win.dispose();
            return;
        }
        try {
            port.addEventListener(this);
        } catch (Exception e) {
            Base.error(e);
        }
        win.setVisible(true);
        ready = true;
    }

    public void close()
    {
        Serial.releasePort(port);
        win.dispose();
        ready = false;
        int p = Base.pluginInstances.indexOf(this);
        if (p>=0) {
            Base.pluginInstances.remove(p);
        }
    }

    public String getMenuTitle()
    {
        return(Translate.t("Grapher"));
    }

    char command = 0;
    String data = "";

    public void executeCommand(char cmd, String dta) {
        String params[] = dta.split(":");
        try {
            switch(cmd) {
                case 'A':
                    if(params.length == 4) {
                        graph.addSeries(params[0], new Color(
                            Integer.parseInt(params[1]),
                            Integer.parseInt(params[2]),
                            Integer.parseInt(params[3])
                        ));
                    }
                    break;
                case 'V':
                    if (playPauseState == false) {
                        float vals[] = new float[params.length];
                        for (int i = 0; i < params.length; i++) {
                            vals[i] = Float.parseFloat(params[i]);
                        }
                        graph.addDataPoint(vals);
                    }
                    break;
                case 'S':
                    if (params.length == 2) {
                        graph.setScreenSize(new Dimension(
                            Integer.parseInt(params[0]),
                            Integer.parseInt(params[1])
                        ));
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                resizeWindow();
                            }
                        });
                    }
                    break;
                case 'M':
                    if (params.length == 4) {
                        graph.setTopMargin(Integer.parseInt(params[0]));
                        graph.setRightMargin(Integer.parseInt(params[1]));
                        graph.setBottomMargin(Integer.parseInt(params[2]));
                        graph.setLeftMargin(Integer.parseInt(params[3]));
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                resizeWindow();
                            }
                        });
                    } 
                    break;

                case 'B':
                    if (params.length == 3) {
                        graph.setBackgroundColor(new Color(
                            Integer.parseInt(params[0]),
                            Integer.parseInt(params[1]),
                            Integer.parseInt(params[2])
                        ));
                    }
                    break;
                case 'F':
                    if (params.length == 3) {
                        graph.setAxisColor(new Color(
                            Integer.parseInt(params[0]),
                            Integer.parseInt(params[1]),
                            Integer.parseInt(params[2])
                        ));
                    }
                    break;
                case 'Y':
                    if (params.length == 3) {
                        graph.setYMinimum(Float.parseFloat(params[0]));
                        graph.setYMaximum(Float.parseFloat(params[1]));
                        graph.setYStep(Float.parseFloat(params[2]));
                    }
                    break;
                case 'R':
                    graph.reset();
                    break;
            }
        } catch (Exception e) {
        }
    }
    
    public ImageIcon toolbarIcon() {
        ImageIcon icon = new ImageIcon(getResourceURL("uecide/plugin/Grapher/grapher.png"));
        return icon;
    }

    public void resizeWindow() {
//        win.setPreferredSize(win.getPreferredSize());
//        win.validate();
        box.invalidate();
        box.validate();
        win.pack();
//        win.repaint();
    }

    public void saveImage() {
        FileDialog fd = new FileDialog(win,
                                   Translate.t("Save Image"),
                                   FileDialog.SAVE);
        fd.setVisible(true);
        String newParentDir = fd.getDirectory();
        String newName = fd.getFile();
        if (newName == null || newParentDir == null) {
            return;
        }

        File df = new File(newParentDir, newName);
        if (!(df.getName().toLowerCase().endsWith(".png"))) {
            return;
        }
        try {
            ImageIO.write(graph.renderGraph(), "PNG", df);
        } catch (Exception e) {
            Base.error(e);
        }
    }

    public void playPause()
    {
        if (playPauseState == false) {
            playPauseState = true;
            playPauseButton.setIcon(playIcon);
        } else {
            playPauseState = false;
            playPauseButton.setIcon(pauseIcon);
        }
    }

    public void populatePreferences(JPanel p) {
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        c.gridheight = 1;

        JLabel label = new JLabel("Grapher font: ");
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

        final Font grapherFont = Base.preferences.getFont("grapher.font");
        fontSizeField.setText(Base.preferences.fontToString(grapherFont));

        final Container parent = p;
        selectSerialFont.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFontChooser fc = new JFontChooser(false);
                fc.setSelectedFont(grapherFont);
                int res = fc.showDialog(parent);
                if (res == JFontChooser.OK_OPTION) {
                    Font f = fc.getSelectedFont();
                    fontSizeField.setText(Base.preferences.fontToString(f));
                }
            }
        });

        fontSizeField.setText(Base.preferences.get("grapher.font"));
    }

    public void savePreferences() {
        Base.preferences.set("grapher.font", fontSizeField.getText());
    }

    public boolean releasePort(String portName) {
        if (portName == null) {
            return true;
        }
        if (port == null) {
            return true;
        }
        if (portName.equals(serialPort)) {
            try {
                port.removeEventListener();
            } catch (Exception e) {
                Base.error(e);
            }
            boolean rok = Serial.releasePort(port);
            port = null;
            return rok;
        }
        return false;
    }

    public void obtainPort(String portName) {
        if (portName.equals(serialPort)) {
            try {
                port = Serial.requestPort(serialPort, this, baudRate);
                port.addEventListener(this);
            } catch (Exception e) {
                editor.message("Unable to reopen port: " + e.getMessage() + "\n", 2);
            }
        }
    }

    public void serialEvent(SerialPortEvent e) {
        if (e.isRXCHAR()) {
            try {
                byte[] bytes = port.readBytes();
                for (byte c : bytes) {
                    if (command == 0) {
                        switch(c) {
                            case 'A':
                            case 'V':
                            case 'R':
                            case 'S':
                            case 'M':
                            case 'B':
                            case 'F':
                            case 'Y':
                                command = (char)c;
                                break;
                        }
                    } else {
                        if(c == '\n' || c == '\r') {
                            char cmd = command;
                            command = 0;
                            String dta = data;
                            data = "";
                            executeCommand(cmd, dta);
                        } else {
                            data += Character.toString((char)c);
                        }
                    }
                }
            } catch (Exception ex) {
                Base.error(ex);
            }
        }
    }
}

