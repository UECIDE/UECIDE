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

public class LibraryLocationList extends JPanel {

    HashMap<String, String> libraryList = new HashMap<String, String>();

    LibraryTableModel dataModel = new LibraryTableModel();

    ActionListener onChange = null;

    class LibraryTableModel extends AbstractTableModel {
        public int getColumnCount() { return 2; }
        public int getRowCount() { return libraryList.keySet().size(); }
        public Object getValueAt(int row, int col) {
            String[] keys = libraryList.keySet().toArray(new String[0]);
            Arrays.sort(keys);

            if (col == 0) {
                return keys[row];
            }
            if (col == 1) {
                return libraryList.get(keys[row]);
            }
            return "";
        }
        public String getColumnName(int col) {
            switch(col) {
                case 0: return "Section Name";
                case 1: return "Destination Folder";
                default: return "";
            }
        }
        public boolean isCellEditable(int row, int col) {
            return true;
        }

        public void setValueAt(Object val, int row, int col) {
            String[] keys = libraryList.keySet().toArray(new String[0]);
            if (row >= keys.length) {
                return;
            }

            Arrays.sort(keys);
            if (col == 0) {
                String oldKey = keys[row];
                String savedData = libraryList.get(oldKey);
                String newKey = (String)val;
                String byebye = libraryList.remove(oldKey);
                System.err.println("Key removed said: " + byebye);
                libraryList.put(newKey, savedData);
                System.err.println("Changing " + oldKey + " to " + newKey);
            }

            if (col == 1) {
                if (row >= keys.length) {
                    return;
                }
                String key = keys[row];
                libraryList.put(key, (String)val);
            }
            if (onChange != null) {
                ActionEvent evt = new ActionEvent(LibraryLocationList.this, 0, "");
                onChange.actionPerformed(evt);
            }
        }
    }


    public LibraryLocationList() {
        setLayout(new BorderLayout());

        final JTable table = new JTable(dataModel); 
        table.setFillsViewportHeight(true);

        JScrollPane scroll = new JScrollPane(table);

        add(scroll, BorderLayout.CENTER);

        JPanel buttonBar = new JPanel();
        buttonBar.setLayout(new FlowLayout());

        JButton addFolder = new JButton("Add Location");
        addFolder.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doAddFolder();
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
                }
            }
        });

        buttonBar.add(addFolder);
        buttonBar.add(deleteSelected);

        add(buttonBar, BorderLayout.SOUTH);
    }

    public void addLibraryLocation(String name, String path) {
        libraryList.put(name, path);
        dataModel.fireTableDataChanged();
    }

    public void removeLibraryLocation(String name) {
        libraryList.remove(name);
        dataModel.fireTableDataChanged();
    }

    public void doAddFolder() {
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File(System.getProperty("user.home")));

        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int rv = fc.showOpenDialog(this);

        if(rv == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            String name = f.getName();
            String path = f.getAbsolutePath();
            addLibraryLocation(name, path);
            if (onChange != null) {
                ActionEvent evt = new ActionEvent(this, 0, "");
                onChange.actionPerformed(evt);
            }
        }
    }

    public void doRemoveSelected(int row) {
        String[] keys = libraryList.keySet().toArray(new String[0]);
        if (row >= keys.length) {
            return;
        }
        Arrays.sort(keys);
        String key = keys[row];
        removeLibraryLocation(key);
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

    public HashMap<String, String> getLibraryList() {
        return libraryList;
    }

    public void addActionListener(ActionListener l) {
        onChange = l;
    }

    public void removeActionListener() {
        onChange = null;
    }
}
