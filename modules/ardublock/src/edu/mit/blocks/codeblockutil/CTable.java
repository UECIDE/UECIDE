package edu.mit.blocks.codeblockutil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import edu.mit.blocks.codeblockutil.CScrollPane.ScrollPolicy;

/**
 * CTable is a generic table with a look and feel that matches the 
 * rest of codeblocks.
 */
public class CTable extends JPanel {

    private static final long serialVersionUID = 328149080251L;
    private static final int COLUMN_WIDTH = 50;
    private static final int ROW_HEIGHT = 15;
    private static final Color foreground = Color.white;
    private static final Font font = new Font("Ariel", Font.BOLD, 12);
    private final List<double[]> data;
    private final JComponent view;
    private final JComponent scroll;
    private String[] columns;
    private JLabel[] columnLabels;

    public CTable() {
        this(9);
    }

    /**
     * Create a new Table instance with an empty domain
     * @param i - thumb width
     */
    public CTable(int i) {
        super(new BorderLayout());
        this.columns = new String[]{};
        this.columnLabels = new JLabel[]{};
        this.data = new ArrayList<double[]>();
        view = new JPanel();
        view.setBackground(foreground);
        scroll = new CTracklessScrollPane(
                view,
                ScrollPolicy.VERTICAL_BAR_AS_NEEDED,
                ScrollPolicy.HORIZONTAL_BAR_AS_NEEDED,
                i, CGraphite.blue, new Color(100, 100, 100));
        this.add(scroll, BorderLayout.CENTER);
    }

    @Override
    public void addMouseListener(MouseListener l) {
        this.view.addMouseListener(l);
    }

    /**
     * rests this table with the new column identities
     *
     */
    public void setColumns(String[] columns) {
        data.clear();
        view.removeAll();
        view.setLayout(new GridLayout(0, columns.length));
        columnLabels = new JLabel[columns.length];
        int i = 0;
        for (String name : columns) {
            JLabel label = new JLabel(name, SwingConstants.CENTER);
            label.setFont(font);
            label.setForeground(foreground);
            label.setOpaque(true);
            label.setBackground(CGraphite.blue);
            label.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.BLUE));
            view.add(label);
            columnLabels[i] = label;
            i++;
        }
        this.columns = columns;
        view.setPreferredSize(new Dimension(columns.length * COLUMN_WIDTH, ROW_HEIGHT * 2));
        scroll.revalidate();
    }

    /**
     * Updates the column names of this with the specified String array of column names
     * @param columns the desired column names to update this with
     */
    public void updateColumns(String[] columns) {
        this.columns = columns;
        for (int i = 0; i < columns.length; i++) {
            columnLabels[i].setText(columns[i]);
        }
        scroll.revalidate();
        scroll.repaint();
    }

    /**
     * Clears the table of all data, but keeps the current column names
     */
    public void clearTable() {
        this.setColumns(this.columns);
    }

    /**
     * Adds a row of data to this using the double array of specified datum
     * @param datum desired row of data to add to this
     */
    public void addRow(double[] datum) {
        for (int i = 0; i < columns.length; i++) {
            JLabel label;
            if (i < datum.length) {
                label = new JLabel(Double.toString(datum[i]), SwingConstants.CENTER);
            } else {
                label = new JLabel();
            }
            label.setOpaque(true);
            if (i == 0) {
                label.setFont(font);
                label.setBackground(CGraphite.blue);
                label.setForeground(foreground);
                label.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.BLUE));
            } else {
                label.setFont(font);
                label.setBackground(CGraphite.gray);
                label.setForeground(foreground);
                label.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.BLUE));
            }
            view.add(label);
        }
        data.add(datum);
        view.setPreferredSize(new Dimension(columns.length * COLUMN_WIDTH, ROW_HEIGHT * (1 + data.size())));
        scroll.revalidate();
        scroll.repaint();

    }

    public String getCSV() {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < columns.length; i++) {
            output.append(columns[i] + ",");
        }
        output.append("\n");
        for (double[] datum : data) {
            for (int i = 0; i < datum.length; i++) {
                output.append(datum[i] + ",");
            }
            output.append("\n");
        }
        output.append("\n");
        return output.toString();
    }

    public Insets getInsets() {
        return new Insets(10, 10, 35, 10);
    }

}
