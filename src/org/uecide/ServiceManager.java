package org.uecide;

import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

public class ServiceManager {
    static ArrayList<Service> services = new ArrayList<Service>();

    public static void addService(Service s) {
        services.add(s);
        if (s.isAutoStart()) {
            s.start();
        }
    }

    public static void open(Window parent) {
        final JDialog dialog = new JDialog(parent, JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setResizable(false);

        dialog.setLayout(new BorderLayout());
        
        JToolBar toolbar = new JToolBar();
        dialog.add(toolbar, BorderLayout.NORTH);

        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout());
        dialog.add(buttons, BorderLayout.SOUTH);

        JScrollPane scroll = new JScrollPane();
        dialog.add(scroll, BorderLayout.CENTER);

        dialog.setMinimumSize(new Dimension(400, 500));
        dialog.setMaximumSize(new Dimension(400, 500));
        dialog.setPreferredSize(new Dimension(400, 500));
        dialog.setSize(new Dimension(400, 500));
        dialog.setLocationRelativeTo(parent);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {    
            public void actionPerformed(ActionEvent ev) {
                dialog.dispose();
            }
        });

        final JButton stopButton = new JButton(Base.loadIconFromResource("toolbar/media-playback-stop.png"));
        final JButton startButton = new JButton(Base.loadIconFromResource("toolbar/media-playback-start.png"));
        final JButton restartButton = new JButton(Base.loadIconFromResource("toolbar/media-seek-forward.png"));

        stopButton.setEnabled(false);
        startButton.setEnabled(false);
        restartButton.setEnabled(false);

        toolbar.add(stopButton);
        toolbar.add(startButton);
        toolbar.add(restartButton);

        buttons.add(closeButton);

        final AbstractTableModel tableModel = new AbstractTableModel() {

            public String getColumnName(int col) {
                switch (col) {
                    case 0: return "Service name";
                    case 1: return "State";
                    case 2: return "Autostart";
                }
                return null;
            }

            public int getRowCount() {
                return services.size();
            }

            public int getColumnCount() {
                return 3;
            }

            public Object getValueAt(int row, int col) {
                Service s = services.get(row);
                if (s == null) {
                    return null;
                }

                switch (col) {
                    case 0: return s.getName();
                    case 1: return s.isRunning() ? "Running" : "Stopped";
                    case 2: return s.isAutoStart() ? "Yes" : "No";
                }
                return null;
            }

            public void setValueAt(Object o, int row, int col) {
            }
        };

        final JTable serviceTable = new JTable(tableModel);

        final ListSelectionModel listSelection = serviceTable.getSelectionModel();

        listSelection.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        listSelection.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent ev) {
                if (ev.getValueIsAdjusting()) {
                    int entry = serviceTable.getSelectedRow();
                    if (entry == -1) {
                        stopButton.setEnabled(false);
                        startButton.setEnabled(false);
                        restartButton.setEnabled(false);
                    }

                    Service s = services.get(entry);
                    if (s == null) {
                        stopButton.setEnabled(false);
                        startButton.setEnabled(false);
                        restartButton.setEnabled(false);
                    }
                    if (s.isRunning()) {
                        stopButton.setEnabled(true);
                        startButton.setEnabled(false);
                        restartButton.setEnabled(true);
                    } else {
                        stopButton.setEnabled(false);
                        startButton.setEnabled(true);
                        restartButton.setEnabled(false);
                    }
                }
            }
        });

        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                int entry = serviceTable.getSelectedRow();
                if (entry == -1) {
                    return;
                }
                Service s = services.get(entry);
                if (s == null) {
                    return;
                }
                if (s.isRunning()) {
                    s.stop(true);
                    tableModel.fireTableRowsUpdated(entry, entry);
                }
                if (s.isRunning()) {
                    stopButton.setEnabled(true);
                    startButton.setEnabled(false);
                    restartButton.setEnabled(true);
                } else {
                    stopButton.setEnabled(false);
                    startButton.setEnabled(true);
                    restartButton.setEnabled(false);
                }
            }
        });

        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                int entry = serviceTable.getSelectedRow();
                if (entry == -1) {
                    return;
                }
                Service s = services.get(entry);
                if (s == null) {
                    return;
                }
                if (!s.isRunning()) {
                    s.start();
                    tableModel.fireTableRowsUpdated(entry, entry);
                }
                if (s.isRunning()) {
                    stopButton.setEnabled(true);
                    startButton.setEnabled(false);
                    restartButton.setEnabled(true);
                } else {
                    stopButton.setEnabled(false);
                    startButton.setEnabled(true);
                    restartButton.setEnabled(false);
                }
            }
        });

        restartButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                int entry = serviceTable.getSelectedRow();
                if (entry == -1) {
                    return;
                }
                Service s = services.get(entry);
                if (s == null) {
                    return;
                }
                if (s.isRunning()) {
                    s.restart();
                    tableModel.fireTableRowsUpdated(entry, entry);
                }
                if (s.isRunning()) {
                    stopButton.setEnabled(true);
                    startButton.setEnabled(false);
                    restartButton.setEnabled(true);
                } else {
                    stopButton.setEnabled(false);
                    startButton.setEnabled(true);
                    restartButton.setEnabled(false);
                }
            }
        });


        final JPopupMenu menu = new JPopupMenu();
        JMenuItem enable = new JMenuItem("Enable auto-start");
        JMenuItem disable = new JMenuItem("Disable auto-start");
        menu.add(enable);
        menu.add(disable);

        enable.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                int entry = serviceTable.getSelectedRow();
                if (entry == -1) {
                    return;
                }
                Service s = services.get(entry);
                if (s == null) {
                    return;
                }
                s.setAutoStart(true);
                tableModel.fireTableRowsUpdated(entry, entry);
            }
        });

        disable.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                int entry = serviceTable.getSelectedRow();
                if (entry == -1) {
                    return;
                }
                Service s = services.get(entry);
                if (s == null) {
                    return;
                }
                s.setAutoStart(false);
                tableModel.fireTableRowsUpdated(entry, entry);
            }
        });

//        serviceTable.setComponentPopupMenu(menu);

        serviceTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if ( SwingUtilities.isLeftMouseButton(e)) {
                    // Do something
                }
                else if ( SwingUtilities.isRightMouseButton(e)) {
                    Point p = e.getPoint();
         
                    int entry = serviceTable.rowAtPoint(p);
         
                    listSelection.setSelectionInterval(entry, entry);
                    Service s = services.get(entry);
                    if (s == null) {
                        stopButton.setEnabled(false);
                        startButton.setEnabled(false);
                        restartButton.setEnabled(false);
                    }
                    if (s.isRunning()) {
                        stopButton.setEnabled(true);
                        startButton.setEnabled(false);
                        restartButton.setEnabled(true);
                    } else {
                        stopButton.setEnabled(false);
                        startButton.setEnabled(true);
                        restartButton.setEnabled(false);
                    }
                    menu.show(serviceTable, e.getX(), e.getY());
                }
            }
        });

        scroll.setViewportView(serviceTable);

        dialog.pack();

        dialog.setVisible(true);
    }
}

