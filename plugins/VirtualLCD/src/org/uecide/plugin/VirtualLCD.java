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

import jssc.*;

import say.swing.*;


public class VirtualLCD extends Plugin implements SerialPortEventListener,MessageConsumer
{
    public static HashMap<String, String> pluginInfo = null;
    public static void setInfo(HashMap<String, String>info) { pluginInfo = info; }
    public static String getInfo(String item) { return pluginInfo.get(item); }

    JFrame win = null;
    LCD lcd;
    SerialPort port;

    String serialPort;

    int baudRate;

    public VirtualLCD(Editor e) { editor = e; }
    public VirtualLCD(EditorBase e) { editorTab = e; }


    public void run()
    {
        if (win != null) {
            close();
        }
        serialPort = editor.getSerialPort();

        win = new JFrame(Translate.t("Virtual LCD"));
        win.getContentPane().setLayout(new BorderLayout());
        win.setResizable(false);

        lcd = new LCD(248,64);
        win.add(lcd, BorderLayout.CENTER);
        
        win.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        win.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                close();
            }
        });
        Base.setIcon(win);

        try {
            Debug.message(this + ": Open port " + serialPort);
            port = Serial.requestPort(serialPort, 115200);
            if (port == null) {
                editor.error("Unable to open serial port");
                return;
            }
                
        } catch(Exception e) {
            editor.error("Unable to open serial port:");
            editor.error(e);
            return;
        }
        try {
            port.addEventListener(this);
        } catch (Exception e) {
            Base.error(e);
        }
        win.setTitle(Translate.t("Virtual LCD") + " :: " + serialPort);
        win.setLocationRelativeTo(editor);
//        win.setSize(lcd.getPreferredSize());
        win.pack();
        win.setVisible(true);
    }

    public void close()
    {
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

        try {
            port.writeString(m);
        } catch (Exception e) {
            Base.error(e);
        }
    }
    
    public void addToolbarButtons(JToolBar toolbar, int flags) {
    }

    public static void populatePreferences(JPanel p) {
    }

    public static void savePreferences() {
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

    boolean secondNibble = false;

    byte[] data = new byte[248/8 * 64];
    byte currentByte = 0;
    int bytepos = 0;

    public void serialEvent(SerialPortEvent e) {
        if (e.isRXCHAR()) {
            try {
                if (port == null) {
                    return;
                }
                byte[] bytes = port.readBytes();
                if (bytes == null) {
                    return;
                }
                String s = "";
                for (byte b : bytes) {
                    if (b <= 80) { // First nibble of first byte) {
                        bytepos = 0;
                        currentByte = (byte)((b - 65) << 4);
                        secondNibble = true;
                    } else {
                        if (!secondNibble) {
                            currentByte = (byte)((b - 97) << 4);
                            secondNibble = true;
                        } else {
                            currentByte |= (b - 97);
                            if (bytepos < (248/8) * 64) {
                                data[bytepos++] = currentByte;
                                if (bytepos == (248/8) * 64) {
                                    lcd.setData(data);
                                }
                            }
                            secondNibble = false;
                        }
                    }
                }
                //term.message(port.readString());
            } catch (Exception ex) {
                editor.error(ex);
            }
        }
    }

    public void populateMenu(JMenu menu, int flags) {
        if (flags == (Plugin.MENU_TOOLS | Plugin.MENU_MID)) {
            JMenuItem item = new JMenuItem("Virtual LCD");
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    run();
                }
            });
            menu.add(item);
        }
    }

    public static String getPreferencesTitle() {
        return "Virtual LCD";
    }

    public void populateContextMenu(JPopupMenu menu, int flags, DefaultMutableTreeNode node) {
    }

    public ImageIcon getFileIconOverlay(File f) { return null; }

}

