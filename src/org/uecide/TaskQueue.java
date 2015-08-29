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

import org.uecide.*;
import org.uecide.plugin.*;
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

public class TaskQueue extends JPanel implements PropertyChangeListener {

    JScrollPane scroll;
    JTable table;

    ArrayList<QueueWorker> workerList = new ArrayList<QueueWorker>();
    ArrayList<QueueWorker> finishedList = new ArrayList<QueueWorker>();

    AbstractTableModel dataModel = new AbstractTableModel() {
        public int getColumnCount() {
            return 3;
        }
        public int getRowCount() {
            return workerList.size();
        }

        public String getColumnName(int col) {
            if (col == 0) return "Plugin";
            if (col == 1) return "Action";
            if (col == 2) return "Progress";
            return "";
        }

        public Class getColumnClass(int col) {
            if (col == 0) return String.class;
            if (col == 1) return String.class;
            if (col == 2) return JProgressBar.class;
            return String.class;
        }

        public Object getValueAt(int row, int col) {
            Object anonWorker = workerList.get(row);
            if (anonWorker instanceof QueueWorker) {
                QueueWorker worker = (QueueWorker)anonWorker;
                if (col == 0) return worker.getTaskName();
                if (col == 1) {
                    if (worker.getState() == SwingWorker.StateValue.PENDING) {
                        return worker.getQueuedDescription();
                    } else {
                        return worker.getActiveDescription();
                    }
                }
                if (col == 2) {
                    JProgressBar bar = new JProgressBar();
                    bar.setValue(worker.getProgress());
                    bar.setString(Integer.toString(worker.getProgress()) + "%");
                    bar.setStringPainted(true);
                    return bar;
                }
                return null;
            }
            return null;
        }
    };

    TableCellRenderer cellRenderer = new TableCellRenderer() {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return (Component)value;
        }
    };

    public TaskQueue() {
        table = new JTable(dataModel);
        table.setDefaultRenderer(JProgressBar.class, cellRenderer);
        scroll = new JScrollPane();
        scroll.setViewportView(table);
        setLayout(new BorderLayout());
        add(scroll, BorderLayout.CENTER);
        Font f = table.getFont();
        table.setFont(new Font(f.getName(), Font.PLAIN, 12));
    }

    public void addTask(QueueWorker worker) {
        workerList.add(worker);
        worker.addPropertyChangeListener(this);
        dataModel.fireTableDataChanged();
        startIfSpace();
    }

    public void propertyChange(PropertyChangeEvent e) {
        QueueWorker worker = (QueueWorker)e.getSource();
        String property = e.getPropertyName();
        if (property.equals("state")) {
            SwingWorker.StateValue state = (SwingWorker.StateValue)e.getNewValue();
            if (state == SwingWorker.StateValue.DONE) {
                workerList.remove(worker);
                finishedList.add(worker);
                firePropertyChange("finished", finishedList.size()-1, finishedList.size());
                startIfSpace();
            }
        }
        dataModel.fireTableDataChanged();
    }

    public QueueWorker[] getFinishedTasks() {
        QueueWorker[] out = finishedList.toArray(new QueueWorker[finishedList.size()]);
        finishedList.clear();
        return out;
    }

    public void startIfSpace() {
        int running = 0;
        for (QueueWorker w : workerList) {
            if (w.getState() == SwingWorker.StateValue.STARTED) {
                running++;
            }
        }

         while(running < 3) {
            QueueWorker runit = null;
            for (QueueWorker w : workerList) {
                if (w.getState() == SwingWorker.StateValue.PENDING) {
                    runit = w;
                    break;
                }
             }
            if (runit == null) {
                break;
            }
            runit.execute();
            running++;
        }
        dataModel.fireTableDataChanged();
    }

    public int getQueueSize() {
        int n = workerList.size();
        n += finishedList.size();
        return n;
    }

    public QueueWorker getWorkerByName(String o) {
        for (QueueWorker w : workerList) {
            if (w.getTaskName().equals(o)) {
                    return w;
            }
        }
        return null;
    }
}
