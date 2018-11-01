package org.uecide;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class BasicConversionSettings extends JPanel {
    int format;

    Editor editor;
    String filename;

    JComboBox<KeyValuePair> convertAs;
    JTextField prefix;

    BasicConversionSettings(Editor ed, String file) {
        super();
        editor = ed;
        filename = file;

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;

        add(new JLabel("Convert to:"), c);
        c.gridy++;

        add(new JLabel("Variable prefix:"), c);
        c.gridy++;

        convertAs = new JComboBox<KeyValuePair>(BasicFileConverter.conversionOptions);
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

        prefix = new JTextField(20);

        String existingPrefix = editor.loadedSketch.get("binary." + filename + ".prefix");
        if ((existingPrefix == null) || (existingPrefix.trim().equals(""))) { 
            existingPrefix = createPrefix(filename);
        }
        prefix.setText(existingPrefix);
        add(prefix, c);
        c.gridy++;
    }

    public int getConversionType() {
        KeyValuePair pair = (KeyValuePair)(convertAs.getSelectedItem());
        return (Integer)pair.getKey();
    }

    public String getPrefix() {
        return prefix.getText();
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
}
