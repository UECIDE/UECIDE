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
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import say.swing.*;
import org.json.simple.*;
import java.beans.*;

import jssc.*;

public class LogicAnalyzer extends Plugin implements SerialPortEventListener, MouseWheelListener {

    JFrame window;

    JLogicView logicView;
    JScrollBar horizontalScroll;
    JScrollBar verticalScroll;

    public LogicAnalyzer(Editor e) { editor = e; }
    public LogicAnalyzer(EditorBase e) { editorTab = e; }

    SerialPort serialPort = null;

    JComboBox portList;
    JComboBox baudList;
    JComboBox sampleRateList;

    JCheckBox[] cb;

    public void populateMenu(JMenu menu, int flags) {
        if (flags == (Plugin.MENU_TOOLS | Plugin.MENU_MID)) {
            JMenuItem item = new JMenuItem("Logic Analyzer");
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    openWindow();
                }
            });
            menu.add(item);
        }
    }

    public void populateContextMenu(JPopupMenu menu, int flags, DefaultMutableTreeNode node) {
    }

    public void addToolbarButtons(JToolBar toolbar, int flags) {
        if (flags == Plugin.TOOLBAR_EDITOR) {

            Version iconTest = new Version("0.8.7z31");

            if (Base.systemVersion.compareTo(iconTest) > 0) {
                editor.addToolbarButton(toolbar, "apps", "logic", "Logic Analyzer", new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        openWindow();
                    }
                });
            } 
        }
    }

    public static void populatePreferences(JPanel p) {
    }

    public static String getPreferencesTitle() {
        return null;
    }

    public static void savePreferences() {
    }

    public ImageIcon getFileIconOverlay(File f) {
        return null;
    }

    public static final Integer[] baudRates = {
        2400, 4800, 9600, 14400, 19200, 28800, 57600, 115200, 230400, 460800, 500000, 1000000, 2000000, 4000000
    };

    public static final Integer[] sampleSpeeds = {
        1, 2, 5, 10, 20, 50, 100, 200
    };

    public void openWindow() {
        window = new JFrame("Logic Analyzer");
        window.setLayout(new BorderLayout());

        JToolBar toolbar = new JToolBar();
        window.add(toolbar, BorderLayout.NORTH);

        final JButton runButton = new JButton("Run");
        final JButton stopButton = new JButton("Stop");
        final JLabel portLabel = new JLabel("Serial Port:");
        portList = new JComboBox(Serial.getPortList().toArray(new String[0]));
        final JLabel baudLabel = new JLabel("Baud Rate:");
        baudList = new JComboBox(baudRates);
        final JLabel sampleLabel = new JLabel("uS Per Sample:");
        sampleRateList = new JComboBox(sampleSpeeds);

        portList.setSelectedItem(Base.preferences.get("logicanalyzer.port", ""));
        baudList.setSelectedItem(Base.preferences.getInteger("logicanalyzer.baud", 9600));

        stopButton.setVisible(false);

        runButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                runButton.setVisible(false);
                stopButton.setVisible(true);
                baudList.setEnabled(false);
                portList.setEnabled(false);
                sampleRateList.setEnabled(false);
                startRunning();
            }
        });

        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stopRunning();
                runButton.setVisible(true);
                stopButton.setVisible(false);
                baudList.setEnabled(true);
                portList.setEnabled(true);
                sampleRateList.setEnabled(true);
            }
        });

        toolbar.add(runButton);
        toolbar.add(stopButton);
        toolbar.addSeparator();
        toolbar.add(portLabel);
        toolbar.add(portList);
        toolbar.addSeparator();
        toolbar.add(baudLabel);
        toolbar.add(baudList);
        toolbar.addSeparator();
        toolbar.add(sampleLabel);
        toolbar.add(sampleRateList);

        logicView = new JLogicView();
        window.add(logicView, BorderLayout.CENTER);

        horizontalScroll = new JScrollBar(JScrollBar.HORIZONTAL);
        verticalScroll = new JScrollBar(JScrollBar.VERTICAL);

        logicView.addMouseWheelListener(this);

        int zr = logicView.getZoomRange();
        verticalScroll.setMinimum(0);
        verticalScroll.setMaximum(zr);
        verticalScroll.setVisibleAmount(1);
        verticalScroll.setValue(zr-1);
        logicView.setZoom(zr-1);

        logicView.tieHorizontalScroll(horizontalScroll);
        
        verticalScroll.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent ev) {
                logicView.setZoom(ev.getValue());
            }
        });

        horizontalScroll.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent ev) {
                logicView.setOffset((long)ev.getValue() * 1000L);
            }
        });

        window.add(horizontalScroll, BorderLayout.SOUTH);
        window.add(verticalScroll, BorderLayout.EAST);

        window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        window.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                close();
            }
        });
        Base.setIcon(window);

        JPanel channelSelectors = new JPanel();
        channelSelectors.setLayout(new BoxLayout(channelSelectors, BoxLayout.PAGE_AXIS));
        window.add(channelSelectors, BorderLayout.WEST);

        cb = new JCheckBox[8];

        for (int i = 0; i < 8; i++) {
            cb[i] = new JCheckBox();
            channelSelectors.add(cb[i]);
            channelSelectors.add(Box.createVerticalGlue());
            cb[i].setSelected(Base.preferences.getBoolean("logicanalyzer." + i + ".enabled"));
        }

        window.pack();
        window.setVisible(true);
        window.setSize(new Dimension(600, 400));
        window.setLocationRelativeTo(editor);

        updateHorizontalRange();
    }

    public void close() {
        if (serialPort != null) {
            if (serialPort.isOpened()) {
                try {
                    serialPort.removeEventListener();
                } catch (SerialPortException ex) {
                }
                Serial.closePort(serialPort);
            }
        }
        window.dispose();
    }

    public void updateHorizontalRange() {
        horizontalScroll.setMaximum((int)(logicView.getSampleLength() / 1000L));
    }

    public void startRunning() {
        logicView.clearData();
        sampleNum = 0;
        Base.preferences.setInteger("logicanalyzer.baud", (Integer)baudList.getSelectedItem());
        Base.preferences.set("logicanalyzer.port", (String)portList.getSelectedItem());
        Base.preferences.setInteger("logicanalyzer.rate", (Integer)sampleRateList.getSelectedItem());
        for  (int i = 0; i < 8; i++) {
            Base.preferences.setBoolean("logicanalyzer." + i + ".enabled", cb[i].isSelected());
        }

        Base.preferences.saveDelay();

        serialPort = Serial.requestPort((String)portList.getSelectedItem(), (Integer)baudList.getSelectedItem());
        if (serialPort == null) {
            return;
        }
        try {
            serialPort.addEventListener(this);
        } catch (SerialPortException ex) {
        }
        // Wait for the bootloader to finish
        try {
            Thread.sleep(2500);
        } catch (InterruptedException ex) {
        }

        logicView.setNSPerSample((long)((Integer)sampleRateList.getSelectedItem() * 1000L));

        try {
            serialPort.writeInt((Integer)sampleRateList.getSelectedItem());
        } catch (SerialPortException ex) {
        }
    }

    public void stopRunning() {
        if (serialPort != null) {
            if (serialPort.isOpened()) {
                try {
                    serialPort.removeEventListener();
                } catch (SerialPortException ex) {
                }
                Serial.closePort(serialPort);
            }
        }
    }

    int lastValue = 0;
    long sampleNum = 0;

    public void serialEvent(SerialPortEvent e) {
        if (e.isRXCHAR()) {
            int mask = 0;
            for (int i = 0; i < 8; i++) {
                if (cb[i].isSelected()) {
                    mask |= (1<<i);
                }
            }
            try {
                if (serialPort == null) {
                    return;
                }
                byte[] bytes = serialPort.readBytes();
                for (byte b : bytes) {
                    int x = ((int)b) & mask;
                    if (x != lastValue) {
                        logicView.addDataPoint(sampleNum, x);
                        lastValue = x;
                    }
                    sampleNum++;
                }
                updateHorizontalRange();
                horizontalScroll.setValue(logicView.getScrollPosition());
            } catch (Exception ex) {
                editor.error(ex);
            }
        }
    }

    public void mouseWheelMoved(MouseWheelEvent ev) {
        int v = verticalScroll.getValue();
        int a = ev.getWheelRotation();
        if (a < 0) {
            v --;
        } else {
            v ++;
        }
        verticalScroll.setValue(v);
    }
}
