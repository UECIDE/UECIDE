package org.uecide.plugin;

import org.uecide.*;
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
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import jssc.*;

import say.swing.*;

import com.wittams.gritty.swing.*;

public class SerialTerminal extends Plugin //implements MessageConsumer
{
    public static HashMap<String, String> pluginInfo = null;
    public static void setInfo(HashMap<String, String>info) { pluginInfo = info; }
    public static String getInfo(String item) { return pluginInfo.get(item); }

    ArrayList<String> history = new ArrayList<String>();
    int historySlot = -1;

    JFrame win = null;
//    JTerminal term;

    GrittyTerminal term;
    SerialTty tty;

    CommunicationPort port;
    JComboBox baudRates;
    JCheckBox showCursor;
    JCheckBox localEcho;
    JCheckBox lineEntry;

    JToolBar toolbar;

    Context ctx;

    static JTextField fontSizeField;
    static JTextField widthField;
    static JTextField heightField;
    static JCheckBox  autoCrIn;
    static JCheckBox  autoCrOut;

    FileOutputStream captureFile = null;

    JTextField lineEntryBox;
    JComboBox lineEndings;
    JButton lineSubmit;

    Box entryLineArea;

    int baudRate;

    boolean ready = false;


    public SerialTerminal(Editor e) { editor = e; }
    public SerialTerminal(EditorBase e) { editorTab = e; }


    JButton[] shortcuts;


    boolean havePortExists = false;

    Thread portCheckThread = null;
    boolean running = true;
    boolean portIsPresent = false;

    @SuppressWarnings("unchecked")
    public void run()
    {

        running = true;

        if (win != null) {
            close();
        }


        Version iconTest = new Version("0.8.8alpha20");

//        if (Base.systemVersion.compareTo(iconTest) < 0) {
//            editor.error("Error: This version of the SerialTerminal plugin requires UECIDE version 0.8.8alpha20 or greater.");
//            return;
//        }

        ctx = editor.getSketch().getContext();

        port = ctx.getDevice();
        if (port == null) {
            ctx.error("Error: You do not have a valid device selected.");
            return;
        }


        try {
            port.exists();
            havePortExists = true;
        } catch (NoSuchMethodError e) {
            havePortExists = false;
        }

        Debug.message(this + ": Opening serial terminal on port " + port);

        if (havePortExists) {
            portIsPresent = true;
            portCheckThread = new Thread() {
                public void run() {
                    while (running) {
                        try {
                            Thread.sleep(100);
                        } catch (Exception e) {
                        }
                        if (!port.exists()) {
                            if (portIsPresent) {
                                portIsPresent = false;
                                disable();
                            }
                        } else {
                            if (!portIsPresent) {
                                portIsPresent = true;
                                enable();
                            }
                        }
                    }
                }
            };
            portCheckThread.start();
        }

        win = new JFrame(Translate.t("Serial Terminal"));
        win.getContentPane().setLayout(new BorderLayout());
//        win.setResizable(false);



        term = new GrittyTerminal();
        tty = new SerialTty(port);
        term.setFont(Preferences.getFont("plugins.serialterminal.font"));
        term.getTermPanel().setSize(new Dimension(100, 100));
        term.getTermPanel().setAntiAliasing(true);
        term.setTty(tty);
        term.getTermPanel().getBackBuffer().clear();
        term.getTermPanel().getScrollBuffer().clear();

        toolbar = new JToolBar();

        toolbar.setFloatable(false);

        try {
            toolbar.add(new ToolbarButton("serial.save", Base.i18n.string("serial.save"), 24, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        saveSession();
                    } catch (Exception ex) {
                        Base.error(ex);
                    }
                }
            }));
            
            toolbar.add(new ToolbarToggleButton("serial.capture", Base.i18n.string("serial.capture"), 24, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ToolbarToggleButton b = (ToolbarToggleButton)e.getSource();
                    if (b.isSelected()) {
                        startCapture();
                    } else {
                        endCapture();
                    }
                }
            }));

            toolbar.add(new ToolbarSpacer());

            toolbar.add(new ToolbarButton("serial.clear", Base.i18n.string("serial.clear"), 24, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    term.getTermPanel().getBackBuffer().clear();
                    term.getTermPanel().getScrollBuffer().clear();
                    term.getTermPanel().updateRangeProperties();
                }
            }));

            ToolbarToggleButton as = new ToolbarToggleButton("serial.pause", Base.i18n.string("serial.pause"), 24, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ToolbarToggleButton b = (ToolbarToggleButton)e.getSource();
                    tty.discardInput(b.isSelected());
                    b.setAlternateIcon(b.isSelected());
                }
            }, "serial.play");

            as.setSelected(false);

            toolbar.add(as);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        win.add(toolbar, BorderLayout.NORTH);

        term.start();

        win.getContentPane().add(term.getTermPanel(), BorderLayout.CENTER);
        win.getContentPane().add(term.getScrollBar(), BorderLayout.EAST);

        term.getTermPanel().addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                term.getScrollBar().setValue(
                    term.getScrollBar().getValue() + (
                        e.getScrollAmount() * e.getWheelRotation()
                    )
                );
            }
        });


//        term.setAutoCr(Preferences.getBoolean("pluhins.serialconsole.autocr_in"));
        tty.setAddCR(Preferences.getBoolean("plugins.serialterminal.autocr_in"));

        shortcuts = new JButton[10];
        Box shortcutBox = Box.createVerticalBox();

        for (int i = 0; i < 9; i++) {
            String name = Preferences.get("plugins.serialterminal.shortcut." + i + ".name");
            if (name == null) {
                name = "None";
            }
            shortcuts[i] = new JButton(name);
            shortcuts[i].setActionCommand(Integer.toString(i));
            shortcuts[i].addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int i = 0;
                    try {
                        i = Integer.parseInt(e.getActionCommand());
                    } catch (Exception ex) {
                    }
            
                    if ((e.getModifiers() & InputEvent.CTRL_MASK) != 0) {
                        JTextField scname = new JTextField(Preferences.get("plugins.serialterminal.shortcut." + i + ".name"));
                        JTextField scstr = new JTextField(Preferences.get("plugins.serialterminal.shortcut." + i + ".string"));
                        JCheckBox docr = new JCheckBox("CR");
                        JCheckBox dolf = new JCheckBox("LF");

                        docr.setSelected(Preferences.getBoolean("plugins.serialterminal.shortcut." + i + ".cr"));
                        dolf.setSelected(Preferences.getBoolean("plugins.serialterminal.shortcut." + i + ".lf"));
                        final JComponent[] inputs = new JComponent[] {
                            new JLabel("Shortcut Name:"),
                            scname,
                            new JLabel("Shortcut Text:"),
                            scstr,
                            docr,
                            dolf
                        };
                        int res = JOptionPane.showConfirmDialog(win, inputs, "Edit Shortcut", JOptionPane.OK_CANCEL_OPTION);
                        if (res == JOptionPane.OK_OPTION) {
                            Preferences.setBoolean("plugins.serialterminal.shortcut." + i + ".cr", docr.isSelected());
                            Preferences.setBoolean("plugins.serialterminal.shortcut." + i + ".lf", dolf.isSelected());
                            Preferences.set("plugins.serialterminal.shortcut." + i + ".name", scname.getText());
                            Preferences.set("plugins.serialterminal.shortcut." + i + ".string", scstr.getText());
                            shortcuts[i].setText(scname.getText());
                        }
                            
                    } else {
                        port.print(Preferences.get("plugins.serialterminal.shortcut." + i + ".string"));
                        boolean cr = Preferences.getBoolean("plugins.serialterminal.shortcut." + i + ".cr");
                        boolean lf = Preferences.getBoolean("plugins.serialterminal.shortcut." + i + ".cr");
                        if (cr) port.print("\r");
                        if (lf) port.print("\n");
                    }
                }
            });
            shortcutBox.add(shortcuts[i]);
        }

        JLabel edmess = new JLabel("<html><body><center>CTRL+Click<br/>to edit</center></body></html>");
        shortcutBox.add(edmess);

        win.getContentPane().add(shortcutBox, BorderLayout.WEST);

        Box bottomBox = Box.createVerticalBox();
        win.getContentPane().add(bottomBox, BorderLayout.SOUTH);

        Box line = Box.createHorizontalBox();
        bottomBox.add(line);

        line.add(Box.createHorizontalGlue());

        JLabel label = new JLabel(Translate.t("Line speed") + ": ");
        line.add(label);

        CommsSpeed[] availableSpeeds = port.getSpeeds();
        baudRates = new JComboBox(availableSpeeds);
        baudRates.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (ready) {
                    CommsSpeed value = (CommsSpeed) baudRates.getSelectedItem();
                    baudRate = value.getSpeed();
                    Preferences.setInteger("plugins.serialterminal.speed", baudRate);
                    Debug.message(this + ": Change baud rate " + port);
                    if (!port.setSpeed(baudRate)) {
                        ctx.error("Error: Error changing baud rate: " + port.getLastError());
                    }
                }
            }
        });

        line.add(baudRates);

        localEcho = new JCheckBox(Translate.t("Local Echo"));

        final JFrame subwin = win;

        line.add(localEcho);
        
        showCursor = new JCheckBox(Translate.t("Show Cursor"));
        term.getTermPanel().setCursorEnabled(Preferences.getBoolean("plugins.serialterminal.cursor"));
        showCursor.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (ready) {
                    term.getTermPanel().setCursorEnabled(showCursor.isSelected());
                    Preferences.setBoolean("plugins.serialterminal.cursor", showCursor.isSelected());
                }
            }
        });
        
        line.add(showCursor); 
        showCursor.setSelected(Preferences.getBoolean("plugins.serialterminal.cursor"));

        JButton pulse = new JButton("Pulse Line");
        pulse.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                port.pulseLine();
            }
        });
        line.add(pulse);

        entryLineArea = Box.createHorizontalBox();

        ActionListener al = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    port.print(lineEntryBox.getText());
                    if (historySlot == -1) {
                        history.add(lineEntryBox.getText());
                    }
                    if (((String)lineEndings.getSelectedItem()).equals("Carriage Return")) {
                        port.print("\r");
                    }
                    if (((String)lineEndings.getSelectedItem()).equals("Line Feed")) {
                        port.print("\n");
                    }
                    if (((String)lineEndings.getSelectedItem()).equals("CR + LF")) {
                        port.print("\r\n");
                    }
                    lineEntryBox.setText("");
                    lineEntryBox.requestFocusInWindow();
                } catch (Exception ex) {
                    ctx.error(ex);
                }
            }
        };

        lineEntryBox = new JTextField();
        lineEntryBox.setBackground(Color.WHITE);
        lineEntryBox.setForeground(Color.BLACK);
        lineEntryBox.addActionListener(al);

        lineEntryBox.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {
            }
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case 38: // Up
                        if (historySlot == -1) {
                            historySlot = history.size();
                        }
                        historySlot--;
                        if (historySlot < 0) historySlot = 0;
                        lineEntryBox.setText(history.get(historySlot));
                        break;
                    case 40: // Down
                        if (historySlot == -1) break;
                        historySlot++;
                        if (historySlot >= history.size()) 
                            historySlot = history.size()-1;
                        lineEntryBox.setText(history.get(historySlot));
                        break;
                    default:
                        historySlot = -1;
                        break;
                }
            }
            public void keyReleased(KeyEvent e) {
            }
        });

        lineEndings = new JComboBox(new String[] {"None", "Carriage Return", "Line Feed", "CR + LF"});

        lineEndings.setSelectedItem(Preferences.get("plugins.serialterminal.lineendings"));
        lineEndings.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Preferences.set("plugins.serialterminal.lineendings", (String)lineEndings.getSelectedItem());
            }
        });

        lineSubmit = new JButton("Send");
        lineSubmit.addActionListener(al);
                
        entryLineArea.add(lineEntryBox);
        entryLineArea.add(lineSubmit);
        entryLineArea.add(lineEndings);
        bottomBox.add(entryLineArea);



        JMenuBar menu = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        menu.add(fileMenu);

        JMenuItem save = new JMenuItem("Save Session");
        fileMenu.add(save);
        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    saveSession();
                } catch (Exception exc) {
                    Base.error(exc);
                }
            }
        });

        JMenuItem closeItem = new JMenuItem("Close");
        fileMenu.add(closeItem);
        closeItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });


        //win.getContentPane().add(menu, BorderLayout.NORTH);

        int width = Preferences.getInteger("plugins.serialterminal.window.width");
        int height = Preferences.getInteger("plugins.serialterminal.window.height");
        if (width == 0) width = 600;
        if (height == 0) height = 400;

        win.pack();
        win.setSize(width, height);

        int top = Preferences.getInteger("plugins.serialterminal.window.y");
        int left = Preferences.getInteger("plugins.serialterminal.window.x");

        if ((top == 0) && (left == 0)) {
            win.setLocationRelativeTo(editor); 
        } else {
            win.setLocation(left, top);
        }

        win.addComponentListener(new ComponentListener() {
            public void componentMoved(ComponentEvent e) {
                Point windowPos = e.getComponent().getLocation(null);
                Preferences.setInteger("plugins.serialterminal.window.x", windowPos.x);
                Preferences.setInteger("plugins.serialterminal.window.y", windowPos.y);
            }
            public void componentResized(ComponentEvent e) {
                Dimension windowSize = e.getComponent().getSize(null);
                Preferences.setInteger("plugins.serialterminal.window.width", windowSize.width);
                Preferences.setInteger("plugins.serialterminal.window.height", windowSize.height);
            }
            public void componentHidden(ComponentEvent e) {
            }
            public void componentShown(ComponentEvent e) {
            }
        });



        win.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        win.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                close();
            }
        });
        Base.setIcon(win);

 //       term.clearScreen();
        try {
            baudRate = Preferences.getInteger("plugins.serialterminal.speed");
            for (CommsSpeed s : availableSpeeds) {
                if (s.getSpeed() == baudRate) {
                    baudRates.setSelectedItem(s);
                }
            }
            Debug.message(this + ": Open port " + port);
            if (port == null) {
                running = false;
                ctx.error("Error: Unable to open serial port");
                win.dispose();
                win = null;
                return;
            }
                
//            term.setDisconnected(false);
            if (!port.openPort()) {
                running = false;
                ctx.error("Error: " + port.getLastError());
                win.dispose();
                win = null;
                return;
            }
            port.setSpeed(baudRate);
        } catch(Exception e) {
            running = false;
            ctx.error("Error: Unable to open serial port:");
            ctx.error(e);
            win.dispose();
            win = null;
            return;
        }
//        term.showCursor(Preferences.getBoolean("plugins.serialterminal.cursor"));
//        port.addCommsListener(this);

        win.setTitle(Translate.t("Serial Terminal") + " :: " + editor.getSketch().getName() + " :: " + port);
        win.setVisible(true);
        ready = true;
    }

    public void close()
    {
        running = false;
        ready = false;
        for( ActionListener al : baudRates.getActionListeners() ) {
            baudRates.removeActionListener( al );
        }
        if (port != null) {
            port.closePort();
        }
        win.dispose();
        win = null;
        Debug.message(this + ": Closing serial terminal on port " + port);
    }

    public void warning(String m) { ctx.warning(m); }
    public void error(String m) { ctx.error(m); }

//    public void message(String m) {
//        if (port == null) {
//            win.setVisible(false);
//            return;
//        }
//        if (Preferences.getBoolean("plugins.serialterminall.autocr_out")) {
//            m = m.replace("\n", "\r\n");
//        }
//
//        if (localEcho.isSelected()) {
//            tty.feed(m);
//        }
//        try {
//            port.print(m);
//        } catch (Exception e) {
//            Base.error(e);
//        }
//    }
    
    public void addToolbarButtons(JToolBar toolbar, int flags) {
        if (flags == Plugin.TOOLBAR_EDITOR) {
            try {
                toolbar.add(new ToolbarButton("apps.serial", "Serial Terminal", new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        run();
                    }
                }));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void populatePreferences(JPanel p) {
    }

    public static void savePreferences() {
    }

    public static PropertyFile getPreferencesTree() {
        PropertyFile f = new PropertyFile();

        f.set("plugins.name", "Plugins");
        f.set("plugins.type", "section");
        f.set("plugins.serialterminal.name", "Serial Terminal");
        f.set("plugins.serialterminal.type", "section");

        f.set("plugins.serialterminal.font.name", "Terminal Font");
        f.set("plugins.serialterminal.font.type", "fontselect");
        f.set("plugins.serialterminal.font.default", "Monospaced,12,plain");

        f.set("plugins.serialterminal.width.name", "Terminal Width");
        f.set("plugins.serialterminal.width.type", "string");
        f.set("plugins.serialterminal.width.default", "80");

        f.set("plugins.serialterminal.height.name", "Terminal Height");
        f.set("plugins.serialterminal.height.type", "string");
        f.set("plugins.serialterminal.height.default", "25");

        f.set("plugins.serialterminal.autocr_in.name", "Add CR to incoming lines");
        f.set("plugins.serialterminal.autocr_in.type", "checkbox");
        f.setBoolean("plugins.serialterminal.autocr_in.default", false);

        f.set("plugins.serialterminal.autocr_out.name", "Add CR to outgoing lines");
        f.set("plugins.serialterminal.autocr_out.type", "checkbox");
        f.setBoolean("plugins.serialterminal.autocr_out.default", false);

        f.set("plugins.serialterminal.codepage.default", "cp850");

        return f;
    }

    public void releasePort(String portName) {
        if (port == null) {
            return;
        }
        if (portName.equals(port.toString())) {
            close();
        }
    }

    public void populateMenu(JMenu menu, int flags) {
        if (flags == (Plugin.MENU_TOOLS | Plugin.MENU_MID)) {

            JMenuItem item = new ActiveMenuItem("Serial Terminal", KeyEvent.VK_T, KeyEvent.SHIFT_MASK, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    run();
                }
            });
            menu.add(item);
        }
    }

    public static String getPreferencesTitle() {
        return null;
    }

    public void populateContextMenu(JPopupMenu menu, int flags, DefaultMutableTreeNode node) {
    }

    public ImageIcon getFileIconOverlay(File f) { return null; }

    public void addPanelsToTabs(JTabbedPane pane,int flags) { }

    public void populateMenu(JPopupMenu menu, int flags) { }

    public void saveSession() throws IOException {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fc.setFileFilter(new FileNameExtensionFilter("Text Files", "txt"));
        int n = fc.showSaveDialog(win);
        if (n == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (f.exists()) {
                int conf = JOptionPane.showConfirmDialog(win, "File exists. Overwrite?", "Overwrite?", JOptionPane.YES_NO_OPTION);
                if (conf == JOptionPane.OK_OPTION) {
                    f.delete();
                } else {
                    return;
                }
            }
            String[] scroll = term.getBufferText(GrittyTerminal.BufferType.Scroll).split("\n");
            String[] back = term.getBufferText(GrittyTerminal.BufferType.Back).split("\n");

            PrintWriter pw = new PrintWriter(f);
            boolean skip = true;
            for (String s : scroll) {
                if (s.trim().equals("") && skip) {
                    continue;
                }
                skip = false;
                pw.println(s.replaceAll("\\s+$",""));
            }
            for (String s : back) {
                if (s.trim().equals("") && skip) {
                    continue;
                }
                skip = false;
                pw.println(s.replaceAll("\\s+$",""));
            }
            pw.close();


        }

    }

    void disable() {
        tty.feed("\r\n\n[31m** Port Closed **[0m\r\n\n");
        port.closePort();
        term.stop();
    }

    void enable() {
        int tries = 100;
        while (tries > 0) {
            if (port.openPort()) {
                port.setSpeed(baudRate);
//                tty = new SerialTty(port);
//                term.setTty(tty);

                term.start();

                tty.feed("\r\n\n[32m** Port Opened **[0m\r\n\n");
                return;
            }
            try {
                Thread.sleep(10);
            } catch (Exception ex) {
            }
            tries--;
        }
    }

    void startCapture() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fc.setFileFilter(new FileNameExtensionFilter("Text Files", "txt"));
        int n = fc.showSaveDialog(win);
        if (n == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (f.exists()) {
                int conf = JOptionPane.showConfirmDialog(win, "File exists. Overwrite?", "Overwrite?", JOptionPane.YES_NO_OPTION);
                if (conf == JOptionPane.OK_OPTION) {
                    f.delete();
                } else {
                    return;
                }
            }

            try {
                captureFile = new FileOutputStream(f);
                tty.captureToFile(captureFile);
            } catch (Exception e) {
            }
        }
    }

    void endCapture() {
        try {
            tty.endCapture();
            captureFile.close();
        } catch (Exception e) {
        }
    }

}

