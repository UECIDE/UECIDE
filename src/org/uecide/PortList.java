/*
 * Copyright (c) 2015, Majenko Technologies
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 * 
 * * Neither the name of Majenko Technologies nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.uecide;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.table.*;
import java.io.*;

public class PortList extends JPanel {

    ArrayList<String> portList = new ArrayList<String>();

    PortTableModel dataModel = new PortTableModel();

    ActionListener onChange = null;

    class PortTableModel extends AbstractTableModel {
        public int getColumnCount() { return 1; }
        public int getRowCount() { return portList.size(); }
        public Object getValueAt(int row, int col) {
            String[] vals = portList.toArray(new String[0]);
            Arrays.sort(vals);

            if (col == 0) {
                return vals[row];
            }
            return "";
        }
        public String getColumnName(int col) {
            switch(col) {
                case 0: return "Port Path";
                default: return "";
            }
        }
        public boolean isCellEditable(int row, int col) {
            return true;
        }

        public void setValueAt(Object val, int row, int col) {
            String[] vals = portList.toArray(new String[0]);
            if (row >= vals.length) {
                return;
            }

            Arrays.sort(vals);
            if (col == 0) {
                String oldVal = vals[row];
                String newVal = (String)val;
                portList.remove(oldVal);
                portList.add(newVal);
            }
        }
    }


    public PortList() {
        setLayout(new BorderLayout());

        final JTable table = new JTable(dataModel); 
        table.setFillsViewportHeight(true);

        JScrollPane scroll = new JScrollPane(table);

        add(scroll, BorderLayout.CENTER);

        JPanel buttonBar = new JPanel();
        buttonBar.setLayout(new FlowLayout());

        JButton addPortBtn = new JButton("Add Port");
        addPortBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doAddPort();
            }
        });

        JButton deleteSelected = new JButton("Delete Selected");
        deleteSelected.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int r = table.getSelectedRow();
                doRemoveSelected(r);
                if (r >= dataModel.getRowCount()) {
                    r = dataModel.getRowCount() - 1;
                }
                try {
                    table.setRowSelectionInterval(r, r);
                } catch (Exception ex) {
                    Debug.exception(ex);
                }
            }
        });

        buttonBar.add(addPortBtn);
        buttonBar.add(deleteSelected);

        add(buttonBar, BorderLayout.SOUTH);
    }

    public void addPort(String name) {
        portList.add(name);
        dataModel.fireTableDataChanged();
    }

    public void removePort(String name) {
        portList.remove(name);
        dataModel.fireTableDataChanged();
    }

    public void doAddPort() {
        String portName = (String)JOptionPane.showInputDialog(this, "Port Name:", "Add Port", JOptionPane.QUESTION_MESSAGE, null, null, "/dev/");

        if (portName != null) {
            addPort(portName);
            if (onChange != null) {
                ActionEvent evt = new ActionEvent(this, 0, "");
                onChange.actionPerformed(evt);
            }
        }
    }

    public void doRemoveSelected(int row) {
        String[] keys = portList.toArray(new String[0]);
        if (row >= keys.length) {
            return;
        }
        Arrays.sort(keys);
        String key = keys[row];
        removePort(key);
        if (onChange != null) {
            ActionEvent evt = new ActionEvent(this, 0, "");
            onChange.actionPerformed(evt);
        }
    }

    public Dimension getPreferredSize() {
        return new Dimension(500, 200);
    }

    public Dimension getMinimumSize() {
        return new Dimension(500, 200); 
    }

    public ArrayList<String> getPortList() {
        return portList;
    }

    public void addActionListener(ActionListener l) {
        onChange = l;
    }

    public void removeActionListener() {
        onChange = null;
    }
}
