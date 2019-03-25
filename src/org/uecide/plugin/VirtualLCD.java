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


public class VirtualLCD extends Plugin implements CommsListener
{
    public static HashMap<String, String> pluginInfo = null;
    public static void setInfo(HashMap<String, String>info) { pluginInfo = info; }
    public static String getInfo(String item) { return pluginInfo.get(item); }

    JFrame win = null;
    LCD lcd;

    Context ctx;
    CommunicationPort port;

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


        win = new JFrame("Virtual LCD");
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
        ctx = editor.getSketch().getContext();
        port = ctx.getDevice();
        win.setTitle("Virtual LCD" + " :: " + port.toString());
        win.setLocationRelativeTo(editor);
        win.pack();
        win.setVisible(true);


        
        try {
            port.openPort();
            port.setSpeed(115200);
            if (port == null) {
                ctx.error("Unable to open serial port");
                return;
            }
        } catch(Exception e) {
            ctx.error("Unable to open serial port:");
            ctx.error(e);
            return;
        }
        try {
            port.addCommsListener(this);
        } catch (Exception e) {
            ctx.error(e);
        }
    }

    public void close()
    {
        if (port != null) {
            port.closePort();
        }
        win.dispose();
    }

    public void addToolbarButtons(JToolBar toolbar, int flags) {
        if (flags == Plugin.TOOLBAR_EDITOR) {
            try {
                toolbar.add(new ToolbarButton("apps.vlcd", "Virtual LCD", new ActionListener() {
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

    public void releasePort(String portName) {
        if (portName == null) {
            return;
        }
        if (portName.equals(port.toString())) {
            close();
        }
    }

    boolean secondNibble = false;

    byte[] data = new byte[size.width/8 * size.height];
    byte currentByte = 0;
    int bytepos = 0;

    Stack<Integer> commandStack = new Stack<Integer>();

    public final int NEXT_NONE = 0;
    public final int NEXT_PUSH_L = 1;
    public final int NEXT_PUSH_H = 2;
    public final byte CMD_PUSH = (byte)129;
    public final byte CMD_SET_DIM = (byte)130;
    public final byte CMD_SET_BG = (byte)131;
    public final byte CMD_SET_FG = (byte)132;
    public final byte CMD_SET_PIX = (byte)133;
    public final byte CMD_CLR_PIX = (byte)134;
    public final byte CMD_SET_LINE = (byte)135;
    public final byte CMD_CLR_LINE = (byte)136;
    public final byte CMD_SET_BAUD = (byte)137;

    int nextByte = NEXT_NONE;

    int px = 0;
    int py = 0;

    int pushVal = 0;

    public void commsDataReceived(byte[] bytes) {
        int red;
        int green;
        int blue;
        int px;
        int py;
        int pz;
        int x0;
        int x1;
        int y0;
        int y1;
        int b3;
        int b2;
        int b1;
        int b0;
        int baud;
        try {
            if (port == null) {
                return;
            }
            if (bytes == null) {
                return;
            }
            for (byte b : bytes) {
                if (((int)b & 0xFF) > 128) {
                    switch (b) {
                        case CMD_PUSH: nextByte = NEXT_PUSH_L; break;
                        case CMD_SET_DIM: // Set Dimensions
                            if (commandStack.size() >= 2) {
                                size.height = commandStack.pop();
                                size.width = commandStack.pop();
                                size.height &= 0xFFF8;
                                size.width &= 0xFFF8;
                                data = new byte[size.width/8 * size.height];
                                lcd.setDimensions(size);
                                win.pack();
                            }
                            commandStack.clear();
                            break;
                        case CMD_SET_BG: // Set background
                            if (commandStack.size() >= 3) {
                                blue = commandStack.pop();
                                green = commandStack.pop();
                                red = commandStack.pop();
                                lcd.setBackground(red, green, blue);
                            }
                            commandStack.clear();
                            break;
                        case CMD_SET_FG: // Set foreground
                            if (commandStack.size() >= 3) {
                                blue = commandStack.pop();
                                green = commandStack.pop();
                                red = commandStack.pop();
                                lcd.setForeground(red, green, blue);
                            }
                            commandStack.clear();
                            break;
                        case CMD_SET_PIX: // Set pixel
                            if (commandStack.size() >= 2) {
                                py = commandStack.pop();
                                px = commandStack.pop();
                                lcd.setPixel(px, py, true);
                            }
                            commandStack.clear();
                            break;
                        case CMD_CLR_PIX: // Clear pixel
                            if (commandStack.size() >= 2) {
                                py = commandStack.pop();
                                px = commandStack.pop();
                                lcd.setPixel(px, py, false);
                            }
                            commandStack.clear();
                            break;
                        case CMD_SET_LINE: // Draw a line
                            if (commandStack.size() >= 4) {
                                y1 = commandStack.pop();
                                x1 = commandStack.pop();
                                y0 = commandStack.pop();
                                x0 = commandStack.pop();
                                drawLine(x0, y0, x1, y1, true);
                            }
                            commandStack.clear();
                            break;
                        case CMD_CLR_LINE: // Erase a line
                            if (commandStack.size() >= 4) {
                                y1 = commandStack.pop();
                                x1 = commandStack.pop();
                                y0 = commandStack.pop();
                                x0 = commandStack.pop();
                                drawLine(x0, y0, x1, y1, false);
                            }
                            commandStack.clear();
                            break;

                        case CMD_SET_BAUD:
                            if (commandStack.size() >= 4) {
                                b3 = commandStack.pop();
                                b2 = commandStack.pop();
                                b1 = commandStack.pop();
                                b0 = commandStack.pop();
                                baud = (b3 << 24) | (b2 << 16) | (b1 << 8) | b0;
                                System.err.println("Setting baud: " + baud);
                                port.setSpeed(baud);
                            }
                            commandStack.clear();
                            break;

                        default: 
                            commandStack.clear();
                            break;
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
                    case NEXT_PUSH_L:
                        pushVal = (((int)(b - 97)) & 0xF);
                        nextByte = NEXT_PUSH_H;
                        break;
                    case NEXT_PUSH_H:
                        pushVal |= ((((int)(b - 97)) & 0xF) << 4);
                        commandStack.push(pushVal);
                        nextByte = NEXT_NONE;
                        break;
                } 
            }
            //term.message(port.readString());
        } catch (Exception ex) {
                ctx.error(ex);
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

    public void drawLine(int x0, int y0, int x1, int y1, boolean onoff) {
        boolean steep = Math.abs(y1 - y0) > Math.abs(x1 - x0);
        if (steep) {
            int t = x0;
            x0 = y0;
            y0 = t;
            t = x1;
            x1 = y1;
            y1 = t;
        }

        if (x0 > x1) {
            int t = x0;
            x0 = x1;
            x1 = t;
            t = y0;
            y0 = y1;
            y1 = t;
        }

        int dx;
        int dy;
        dx = x1 - x0;
        dy = Math.abs(y1 - y0);

        int err = dx / 2;
        int ystep;

        if (y0 < y1) {
            ystep = 1;
        } else {
            ystep = -1;
        }

        for (; x0 <= x1; x0++) {
            if (steep) {
                lcd.setPixel(y0, x0, onoff);
            } else {
                lcd.setPixel(x0, y0, onoff);
            }
            err -= dy;
            if (err < 0) {
                y0 += ystep;
                err += dx;
            }
        }
    }

    public void commsEventReceived(CommsEvent e) {
    }

    public void addPanelsToTabs(JTabbedPane pane,int flags) { }

    public void populateMenu(JPopupMenu menu, int flags) { }

}

