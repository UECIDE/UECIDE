package uk.co.majenko.bmpedit;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import javax.imageio.*;
import java.io.*;

public class ToolsPanel extends JToolBar { //JPanel {

    Color fgColor = new Color(255, 255, 255);
    Color bgColor = new Color(0, 0, 0);

    public static final int NONE = 0;
    public static final int DRAW = 1;
    public static final int PICK = 2;
    public static final int ERASE = 3;
    public static final int SELECT = 4;

    static final Color selectedColor = new Color(0, 100, 0);

    public int selectedTool = NONE;

    ToolButton draw;
    ToolButton erase;
    ToolButton pick;
    ToolButton select;

    JButton foreground;
    JButton background;

    BMPEdit editor;

    JSlider toolSize;

    public ToolsPanel(BMPEdit ed) throws IOException {
        super();
        editor = ed;

        setOrientation(JToolBar.VERTICAL);
        setFloatable(false);

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.VERTICAL;

        draw = new ToolButton("/uk/co/majenko/bmpedit/icons/draw.png", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectedTool = DRAW;
                updateTools();
            }
        });
        draw.setSelectedColor(selectedColor);
        add(draw, c);
        c.gridy++;

        erase = new ToolButton("/uk/co/majenko/bmpedit/icons/erase.png", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectedTool = ERASE;
                updateTools();
            }
        });
        erase.setSelectedColor(selectedColor);
        add(erase, c);
        c.gridy++;

        pick = new ToolButton("/uk/co/majenko/bmpedit/icons/pick.png", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectedTool = PICK;
                updateTools();
            }
        });
        pick.setSelectedColor(selectedColor);
        add(pick, c);
        c.gridy++;

        select = new ToolButton("/uk/co/majenko/bmpedit/icons/select.png", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectedTool = SELECT;
                updateTools();
            }
        });
        select.setSelectedColor(selectedColor);
        add(select, c);
        c.gridy++;

//        addSeparator();

        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setSize(new Dimension(32, 128));
        p.setMinimumSize(new Dimension(32, 128));
        p.setMaximumSize(new Dimension(32, 128));
        p.setPreferredSize(new Dimension(32, 128));

        toolSize = new JSlider(1, 32, 1);
        toolSize.setOrientation(JSlider.VERTICAL);
        toolSize.setMajorTickSpacing(1);
        toolSize.setMinorTickSpacing(1);
        toolSize.setPaintTicks(true);
        toolSize.setPaintLabels(false);
        toolSize.setSnapToTicks(true);
        toolSize.setBorder(new EmptyBorder(0, 0, 0, 0));
        p.add(toolSize, BorderLayout.CENTER);

        JTextField sizeValue = new JTextField(4);
        sizeValue.setText("1");

        toolSize.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                sizeValue.setText("" + toolSize.getValue());
            }
        });

        sizeValue.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    toolSize.setValue(Integer.parseInt(sizeValue.getText()));
                } catch (Exception ignored) {
                }
                sizeValue.setText("" + toolSize.getValue());
            }
        });
    

        p.add(sizeValue, BorderLayout.SOUTH);

        add(p, c);
        c.gridy++;

        c.weighty = 1f;
//        addSeparator();
        add(Box.createVerticalGlue(), c);
        c.gridy++;
        c.weighty = 0f;

        foreground = new JButton();
        foreground.setSize(new Dimension(32, 32));
        foreground.setMinimumSize(new Dimension(32, 32));
        foreground.setMaximumSize(new Dimension(32, 32));
        foreground.setPreferredSize(new Dimension(32, 32));
        foreground.setBackground(fgColor);
        add(foreground, c);
        c.gridy++;

        foreground.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JColorChooser fc = new JColorChooser();
                Color clr = fc.showDialog(ToolsPanel.this, "Select Foreground Color", fgColor);
                if(clr != null) {
                    fgColor = clr;
                    foreground.setBackground(clr);
                }
            }
        });

        background = new JButton();
        background.setSize(new Dimension(32, 32));
        background.setMinimumSize(new Dimension(32, 32));
        background.setMaximumSize(new Dimension(32, 32));
        background.setPreferredSize(new Dimension(32, 32));
        background.setBackground(bgColor);
        add(background, c);

        background.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JColorChooser fc = new JColorChooser();
                Color clr = fc.showDialog(ToolsPanel.this, "Select Background Color", bgColor);
                if(clr != null) {
                    bgColor = clr;
                    background.setBackground(clr);
                }
            }
        });


        selectedTool = DRAW;

        updateTools();
    }

    public Color getForegroundColor() {
        return fgColor;
    }

    public Color getBackgroundColor() {
        return bgColor;
    }

    public void updateTools() {
        draw.setSelected(selectedTool == DRAW);
        erase.setSelected(selectedTool == ERASE);
        pick.setSelected(selectedTool == PICK);
        select.setSelected(selectedTool == SELECT);

        if (selectedTool == SELECT) {
            editor.getImagePanel().showRubberband();
        } else {
            editor.getImagePanel().hideRubberband();
        }
    }

    public int getSelectedTool() { 
        return selectedTool;
    }

    public void setForegroundColor(Color c) {
        fgColor = c;
        foreground.setBackground(fgColor);
    }

    public void setBackgroundColor(Color c) {
        bgColor = c;
        background.setBackground(bgColor);
    }

    public void setSelectedTool(int t) {
        selectedTool = t;
        updateTools();
    }

    public int getToolSize() { 
        return toolSize.getValue();
    }
}
