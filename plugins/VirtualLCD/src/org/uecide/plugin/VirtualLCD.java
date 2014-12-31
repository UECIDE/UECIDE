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

    Dimension size = new Dimension(64, 64);

    int col_r = 0;
    int col_g = 0;
    int col_b = 0;


    public void run()
    {
        if (win != null) {
            close();
        }

        win = new JFrame(Translate.t("Virtual LCD"));
        win.getContentPane().setLayout(new BorderLayout());
        win.setResizable(true);
        lcd = new LCD(size.width,size.height);
        win.add(lcd, BorderLayout.CENTER);
        win.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        win.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                close();
            }
        });
        Base.setIcon(win);
        win.setTitle(Translate.t("Virtual LCD") + " :: " + serialPort);
        win.setLocationRelativeTo(editor);
        win.pack();
        win.setVisible(true);


        serialPort = editor.getSerialPort();
        
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

    byte[] data = new byte[size.width/8 * size.height];
    byte currentByte = 0;
    int bytepos = 0;

    public final int NEXT_NONE = 0;
    public final int NEXT_XL = 1;
    public final int NEXT_XH = 2;
    public final int NEXT_YL = 3;
    public final int NEXT_YH = 4;
    public final int NEXT_BRL = 5;
    public final int NEXT_BRH = 6;
    public final int NEXT_BGL = 7;
    public final int NEXT_BGH = 8;
    public final int NEXT_BBL = 9;
    public final int NEXT_BBH = 10;
    public final int NEXT_FRL = 11;
    public final int NEXT_FRH = 12;
    public final int NEXT_FGL = 13;
    public final int NEXT_FGH = 14;
    public final int NEXT_FBL = 15;
    public final int NEXT_FBH = 16;

    int nextByte = NEXT_NONE;

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
                for (byte b : bytes) {
                    if (((int)b & 0xFF) > 128) {
                        switch (b) {
                            case (byte)129: nextByte = NEXT_XL; break;
                            case (byte)130: nextByte = NEXT_XH; break;
                            case (byte)131: nextByte = NEXT_YL; break;
                            case (byte)132: nextByte = NEXT_YH; break;
                            case (byte)133: nextByte = NEXT_BRL; break;
                            case (byte)134: nextByte = NEXT_BRH; break;
                            case (byte)135: nextByte = NEXT_BGL; break;
                            case (byte)136: nextByte = NEXT_BGH; break;
                            case (byte)137: nextByte = NEXT_BBL; break;
                            case (byte)138: nextByte = NEXT_BBH; break;
                            case (byte)139: nextByte = NEXT_FRL; break;
                            case (byte)140: nextByte = NEXT_FRH; break;
                            case (byte)141: nextByte = NEXT_FGL; break;
                            case (byte)142: nextByte = NEXT_FGH; break;
                            case (byte)143: nextByte = NEXT_FBL; break;
                            case (byte)144: nextByte = NEXT_FBH; break;
                            default: nextByte = NEXT_NONE; break;
                        }
                        continue;
                    }

                    switch (nextByte) {
                        default:
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
                                    if (bytepos < (size.width/8) * size.height) {
                                        data[bytepos++] = currentByte;
                                        if (bytepos == (size.width/8) * size.height) {
                                            lcd.setData(data);
                                        }
                                    }
                                    secondNibble = false;
                                }
                            }
                            break;
                        case NEXT_XL:
                            size.width = ((b - 97));
                            nextByte = NEXT_NONE;
                            break;
                        case NEXT_XH:
                            size.width |= ((b - 97) << 4);
                            nextByte = NEXT_NONE;
                            break;
                        case NEXT_YL:
                            size.height = ((b - 97));
                            nextByte = NEXT_NONE;
                            break;
                        case NEXT_YH:
                            size.height |= ((b - 97) << 4);
                            nextByte = NEXT_NONE;
                            data = new byte[size.width/8 * size.height];
                            lcd.setDimensions(size);
                            win.pack();
                            System.err.print("New dimensions: " + size);
                            break;
                        case NEXT_BRL:
                            col_r = ((b - 97));
                            nextByte = NEXT_NONE;
                            break;
                        case NEXT_BRH:
                            col_r |= ((b - 97) << 4);
                            nextByte = NEXT_NONE;
                            break;
                        case NEXT_BGL:
                            col_g = ((b - 97));
                            nextByte = NEXT_NONE;
                            break;
                        case NEXT_BGH:
                            col_g |= ((b - 97) << 4);
                            nextByte = NEXT_NONE;
                            break;
                        case NEXT_BBL:
                            col_b = ((b - 97));
                            nextByte = NEXT_NONE;
                            break;
                        case NEXT_BBH:
                            col_b |= ((b - 97) << 4);
                            nextByte = NEXT_NONE;
                            lcd.setBackground(col_r, col_g, col_b);
                            break;
                        case NEXT_FRL:
                            col_r = ((b - 97));
                            nextByte = NEXT_NONE;
                            break;
                        case NEXT_FRH:
                            col_r |= ((b - 97) << 4);
                            nextByte = NEXT_NONE;
                            break;
                        case NEXT_FGL:
                            col_g = ((b - 97));
                            nextByte = NEXT_NONE;
                            break;
                        case NEXT_FGH:
                            col_g |= ((b - 97) << 4);
                            nextByte = NEXT_NONE;
                            break;
                        case NEXT_FBL:
                            col_b = ((b - 97));
                            nextByte = NEXT_NONE;
                            break;
                        case NEXT_FBH:
                            col_b |= ((b - 97) << 4);
                            nextByte = NEXT_NONE;
                            lcd.setForeground(col_r, col_g, col_b);
                            break;
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

