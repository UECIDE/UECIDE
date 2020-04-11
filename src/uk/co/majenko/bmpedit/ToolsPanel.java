package uk.co.majenko.bmpedit;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import javax.imageio.*;
import java.io.*;

public class ToolsPanel extends JToolBar { //JPanel {

    public static final int NONE = 0;
    public static final int DRAW = 1;
    public static final int PICK = 2;
    public static final int ERASE = 3;
    public static final int SELECT = 4;
    public static final int FILL = 5;

    static final Color selectedColor = new Color(0, 100, 0);

    ToolButton draw;
    ToolButton erase;
    ToolButton pick;
    ToolButton select;
    ToolButton fill;
    ColorButton color;

    BMPEdit editor;

    JSlider toolSize;
    JPanel toolSizePanel;

    Tool[] tools = {
        new DrawTool(this),
        new EraseTool(this),
        new FillTool(this),
        new SelectTool(this),
        new PickTool(this)
    };

    Tool selectedTool;

    public ToolsPanel(BMPEdit ed) throws IOException {
        super();
        editor = ed;

        setOrientation(JToolBar.VERTICAL);
        setFloatable(false);

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridy = 0;
        c.weighty = 0d;

        for (int i = 0; i < tools.length; i++) {
            c.gridx = i % 2;
            c.gridy = i / 2;

            ToolButton b = tools[i].getButton();
            b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ToolButton btn = (ToolButton)e.getSource();
                    selectTool(btn.getTool());
                }
            });
            add(b, c);
        }
        c.gridy++;
        c.gridwidth = 2;
        c.gridx = 0;

        add(new JLabel(" "), c);
        c.gridy++;

        color = new ColorButton();
        add(color, c);
        c.gridy++;

        add(new JLabel(" "), c);
        c.gridy++;

        for (int i = 0; i < tools.length; i++) {

            JPanel b = tools[i].getOptionsPanel();
            if (b != null) {
                add(b, c);
                c.gridy++;
            }
        }
        
        c.weighty = 1d;
        add(new JLabel(" "), c);
        c.gridy++;

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                selectTool(tools[0]);
            }
        });

    }

    public Color getForegroundColor() {
        return color.getForegroundColor();
    }

    public Color getBackgroundColor() {
        return color.getBackgroundColor();
    }

    public Tool getSelectedTool() { 
        return selectedTool;
    }

    public void setForegroundColor(Color c) {
        color.setForegroundColor(c);
    }

    public void setBackgroundColor(Color c) {
        color.setBackgroundColor(c);
    }

    public void setSelectedTool(Tool t) {
        selectedTool = t;
    }

    public int getToolSize() { 
        return toolSize.getValue();
    }

    public void selectTool(Tool t) {
        for (int i = 0; i < tools.length; i++) {
            if (tools[i] != t) {
                tools[i].deselect(editor.getImagePanel());
            }
        }
        selectedTool = t;
        t.select(editor.getImagePanel());
    }
}
