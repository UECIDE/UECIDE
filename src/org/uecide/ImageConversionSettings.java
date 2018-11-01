package org.uecide;

import javax.swing.*;
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

    ImageConversionSettings(Editor ed, String file) {
        super();
        editor = ed;
        filename = file;

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;

        add(new JLabel("Convert to:"), c);
        c.gridy++;

        add(new JLabel("Data type:"), c);
        c.gridy++;

        add(new JLabel("Variable prefix:"), c);
        c.gridy++;

        add(new JLabel("Transparecy color:"), c);

        convertAs = new JComboBox<KeyValuePair>(ImageFileConverter.conversionOptions);
        c.gridx = 1;
        c.gridy = 0;
        add(convertAs, c);
        c.gridy++;

        int selectedConversion = editor.loadedSketch.getInteger("binary." + filename + ".conversion");

        for (int i = 0; i < convertAs.getItemCount(); i++) {
            KeyValuePair pair = convertAs.getItemAt(i);
            if ((Integer)pair.getKey() == selectedConversion) {
                convertAs.setSelectedIndex(i);
            }
        }

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

        add(dataType, c);
        c.gridy++;

        prefix = new JTextField(20);

        String existingPrefix = editor.loadedSketch.get("binary." + filename + ".prefix");
        if ((existingPrefix == null) || (existingPrefix.trim().equals(""))) { 
            existingPrefix = createPrefix(filename);
        }
        prefix.setText(existingPrefix);
        add(prefix, c);
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

        add(color, c);
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
    }

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
}
