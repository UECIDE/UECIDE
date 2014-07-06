package uecide.plugin;

import uecide.app.*;
import uecide.plugin.*;
import uecide.app.debug.*;
import uecide.app.editors.*;
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
}
