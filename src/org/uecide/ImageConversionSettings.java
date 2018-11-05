package org.uecide;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class ImageConversionSettings extends JPanel {
    int format;

    Editor editor;
    String filename;

    JComboBox<KeyValuePair> convertAs;
    JComboBox<String> dataType;
    JTextField prefix;
    JButton color;

    JLabel prefixLabel;
    JLabel dataTypeLabel;
    JLabel transpLabel;

    JSlider threshold;
    JLabel thresholdLabel;
    JLabel thresholdValue;
    JPanel thresholdPanel;

    ImageConversionSettings(Editor ed, String file) {
        super();
        editor = ed;
        filename = file;

        setLayout(new BorderLayout());

        JPanel inner = new JPanel();
        inner.setLayout(new GridBagLayout());

        add(inner, BorderLayout.CENTER);

        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.PAGE_START;

        c.weightx = 0.5;
        inner.add(new JLabel("Convert to:"), c);
        c.gridy++;

        dataTypeLabel = new JLabel("Data type:");
        inner.add(dataTypeLabel, c);
        c.gridy++;

        prefixLabel = new JLabel("Variable prefix:");
        inner.add(prefixLabel, c);
        c.gridy++;

        transpLabel = new JLabel("Transparecy color:");
        inner.add(transpLabel, c);
        c.gridy++;

        thresholdLabel = new JLabel("Threshold:");
        inner.add(thresholdLabel, c);
        c.gridy++;

        convertAs = new JComboBox<KeyValuePair>(ImageFileConverter.conversionOptions);
        c.gridx = 1;
        c.gridy = 0;
        inner.add(convertAs, c);
        c.gridy++;

        int selectedConversion = editor.loadedSketch.getInteger("binary." + filename + ".conversion");

        for (int i = 0; i < convertAs.getItemCount(); i++) {
            KeyValuePair pair = convertAs.getItemAt(i);
            if ((Integer)pair.getKey() == selectedConversion) {
                convertAs.setSelectedIndex(i);
            }
        }

        convertAs.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                KeyValuePair kv = (KeyValuePair)convertAs.getSelectedItem();
                showTransparency(ImageFileConverter.wantsTransparencyColor((Integer)kv.getKey()));
                showDataType(ImageFileConverter.wantsDataType((Integer)kv.getKey()));
                showThreshold(ImageFileConverter.wantsThreshold((Integer)kv.getKey()));
                showPrefix((Integer)kv.getKey() != 0);
            }
        });

        String[] dataTypes = {
            "uint8_t",
            "uint16_t",
            "uint32_t"
        };

        dataType = new JComboBox<String>(dataTypes);
        String selectedDataType = editor.loadedSketch.get("binary." + filename + ".datatype");
        for (int i = 0; i < dataType.getItemCount(); i++) {
            String opt = dataType.getItemAt(i);
            if (opt.equals(selectedDataType)) {
                dataType.setSelectedIndex(i);
            }
        }

        inner.add(dataType, c);
        c.gridy++;

        prefix = new JTextField(20);

        String existingPrefix = editor.loadedSketch.get("binary." + filename + ".prefix");
        if ((existingPrefix == null) || (existingPrefix.trim().equals(""))) { 
            existingPrefix = createPrefix(filename);
        }
        prefix.setText(existingPrefix);
        inner.add(prefix, c);
        c.gridy++;

        color = new JButton();
        color.setSize(new Dimension(32, 16));
        color.setMinimumSize(new Dimension(32, 16));
        color.setMaximumSize(new Dimension(32, 16));
        color.setPreferredSize(new Dimension(32, 16));

        Color clr = Color.BLACK;

        if (editor.loadedSketch.get("binary." + filename + ".transparency") != null) {
            clr = editor.loadedSketch.getColor("binary." + filename + ".transparency");
        }

        color.setBackground(clr);
        color.setBorderPainted(false);
        color.setFocusPainted(false);

        inner.add(color, c);
        c.gridy++;

        color.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JColorChooser jc = new JColorChooser();
                Color selectedColor = jc.showDialog(ImageConversionSettings.this, "Select Color", color.getBackground());
                if (selectedColor != null) {
                    color.setBackground(selectedColor);
                }
            }
        });

        thresholdPanel = new JPanel();
        thresholdPanel.setLayout(new BorderLayout());

        threshold = new JSlider(0, 255);
        int thresh = 127;
        if (editor.loadedSketch.get("binary." + filename + ".threshold") != null) {
            thresh = editor.loadedSketch.getInteger("binary." + filename + ".threshold");
        }
        threshold.setOrientation(JSlider.HORIZONTAL);
        threshold.setValue(thresh);

        thresholdPanel.add(threshold, BorderLayout.CENTER);

        thresholdValue = new JLabel();
        thresholdValue.setSize(new Dimension(50, 0));
        thresholdValue.setPreferredSize(new Dimension(50, 0));
        thresholdValue.setText("" + thresh);
        thresholdValue.setHorizontalAlignment(JLabel.RIGHT);
        thresholdPanel.add(thresholdValue, BorderLayout.EAST); 

        inner.add(thresholdPanel, c);

        threshold.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                thresholdValue.setText("" + threshold.getValue());
            }
        });


        showTransparency(ImageFileConverter.wantsTransparencyColor(selectedConversion));
        showDataType(ImageFileConverter.wantsDataType(selectedConversion));
        showPrefix(selectedConversion != 0);
        showThreshold(ImageFileConverter.wantsThreshold(selectedConversion));

        setSize(new Dimension(300, 200));
    }

    public Dimension getSize() {
        return new Dimension(400, 150);
    }

    public Dimension getPreferredSize() { return getSize(); }
    public Dimension getMaximumSize() { return getSize(); }
    public Dimension getMinimumSize() { return getSize(); }

    public int getConversionType() {
        KeyValuePair pair = (KeyValuePair)(convertAs.getSelectedItem());
        return (Integer)pair.getKey();
    }

    public String getPrefix() {
        return prefix.getText();
    }

    public Color getTransparency() {
        return color.getBackground();
    }

    String createPrefix(String name) {
        name = name.toLowerCase();
        int idx = name.lastIndexOf(".");
        if (idx >= 0) {
            name = name.substring(0, idx);
        }
        name = name.replace(".", "_");
        name = name.replace(" ", "_");
        name = name.replace("-", "_");
        return name;
    }

    public String getDataType() {
        return (String)dataType.getSelectedItem();
    }

    public int getThreshold() {
        return threshold.getValue();
    }

    void showTransparency(boolean s) {
        transpLabel.setVisible(s);
        color.setVisible(s);
    }

    void showDataType(boolean s) {
        dataTypeLabel.setVisible(s);
        dataType.setVisible(s);
    }

    void showPrefix(boolean s) {
        prefixLabel.setVisible(s);
        prefix.setVisible(s);
    }

    void showThreshold(boolean s) {
        thresholdLabel.setVisible(s);
        thresholdPanel.setVisible(s);
    }

        
}
