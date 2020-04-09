package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.PropertyFile;
import org.uecide.Utils;

import javax.swing.JFrame;
import javax.swing.JCheckBox;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.BoxLayout;
import javax.swing.Box;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.awt.Dimension;
import java.awt.Component;
import java.awt.Point;
import java.awt.Dialog;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

import java.io.File;

import java.util.HashMap;

public class BinaryFileConversionOptions extends JDialog {
    File file;
    Context ctx;

    JPanel inner;
    JPanel options;
    JCheckBox doConversion;

    GridBagConstraints constraints;
    HashMap<String, JComponent> components;
    

    public BinaryFileConversionOptions(Context c, File f) {
        super(((SwingGui)c.getGui()).getFrame(), "Binary File Conversion", Dialog.ModalityType.APPLICATION_MODAL);
        ctx = c;
        file = f;
        setResizable(false);
        setLocationRelativeTo(null);

        inner = new JPanel();
        inner.setBorder(new EmptyBorder(5, 5, 5, 5));

        add(inner);

        inner.setLayout(new BorderLayout());

        doConversion = new JCheckBox("Convert and include this file");
        doConversion.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                for (JComponent cmp : components.values()) {
                    cmp.setEnabled(doConversion.isSelected());
                }
            }
        });
        doConversion.setSelected(ctx.getSketch().getSettings().getBoolean("binary." + Utils.sanitize(file.getName()) + ".conversion"));
        inner.add(doConversion, BorderLayout.NORTH);

        options = new JPanel();
        options.setLayout(new GridBagLayout());
        constraints = new GridBagConstraints();
        inner.add(options, BorderLayout.CENTER);

        constraints.gridx = 0;
        constraints.gridy = 0;

        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        components = new HashMap<String, JComponent>();

        String extension = "";
        int i = file.getName().lastIndexOf('.');
        if (i > 0) {
            extension = file.getName().substring(i+1).toLowerCase();
        } 

        switch (extension) {
            case "gif":
            case "jpg":
            case "jpeg":
            case "bmp":
            case "png":
            case "565":
                populateGraphic();
                break;
            
            case "wav":
            case "mp3":
            case "ogg":
            case "flac":
                populateAudio();
                break;

            default:
                populateGeneric();
                break;
        }

        for (JComponent cmp : components.values()) {
            cmp.setEnabled(doConversion.isSelected());
        }

        JButton ok = new JButton("Ok");
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                save();
                dispose();
            }
        });
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                dispose();
            }
        });
        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));

        ok.setAlignmentX(Component.RIGHT_ALIGNMENT);
        cancel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        buttons.setAlignmentX(Component.RIGHT_ALIGNMENT);
        buttons.add(Box.createHorizontalGlue());
        buttons.add(ok);
        buttons.add(cancel);
        add(buttons, BorderLayout.SOUTH);
        pack();
        forceLocation();
        setVisible(true);
    }

    @SuppressWarnings("unchecked")
    void save() {
        PropertyFile pf = ctx.getSketch().getSettings();
        String pfx = "binary." + Utils.sanitize(file.getName()) + ".";
        pf.setBoolean(pfx + "conversion", doConversion.isSelected());
        for (String cmpname : components.keySet()) {
            JComponent cmp = components.get(cmpname);

            if (cmp instanceof JTextField) {
                JTextField c1 = (JTextField)cmp;
                pf.set(pfx + cmpname, c1.getText());
            } else if (cmp instanceof JComboBox) {
                JComboBox<KVPair<String, String>> c1 = (JComboBox<KVPair<String, String>>)cmp;
                KVPair<String, String>pair = (KVPair<String, String>)c1.getSelectedItem();
                pf.set(pfx + cmpname, pair.getKey());
            }
        }
        ctx.getSketch().saveSettings();
    }

    void populateGraphic() {

        constraints.gridx = 0;
        constraints.weightx = 0.1d;
        options.add(new JLabel("Convert to:"), constraints);

        KVPair[] formats = new KVPair[2];
        formats[0] = new KVPair<String, String>("verbatim", "Keep as-is");
        formats[1] = new KVPair<String, String>("rgb565", "RGB565");
        JComboBox<KVPair> format = new JComboBox<KVPair>(formats);
        components.put("format", format);
        constraints.gridx = 1;
        constraints.weightx = 0.0d;
        options.add(format, constraints);
        constraints.gridy++;

        populateGeneric();






        String theformat = ctx.getSketch().getSettings().get("binary." + Utils.sanitize(file.getName()) + ".format");
        format.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                KVPair<String, String> selPair = (KVPair<String, String>)format.getSelectedItem();
                if (selPair.getKey().equals("verbatim")) {
                    enableComponent("type");
                    KVPair<String, String> selPair2 = (KVPair<String, String>)((JComboBox)components.get("type")).getSelectedItem();
                    if (selPair2.getKey().equals("uint8_t") || selPair2.getKey().equals("int8_t")) {
                        disableComponent("endian");
                    } else {
                        enableComponent("endian");
                    }
                } else {
                    disableComponent("type");
                    disableComponent("endian");
                }
            }
        });


        if (theformat != null) {
            int i = 0;
            for (KVPair p : formats) {
                if (p.getKey().equals(theformat)) {
                    format.setSelectedIndex(i);
                    if (p.getKey().equals("verbatim")) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                enableComponent("type");
                                enableComponent("endian");
                            }
                        });
                    } else {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                disableComponent("type");
                                disableComponent("endian");
                            }
                        });
                    }
                }
                i++;
            }
        }

    }

    void populateAudio() {
        populateGeneric();
    }

    void populateGeneric() {
        constraints.gridx = 0;
        constraints.weightx = 0.1d;
        options.add(new JLabel("Variable prefix:"), constraints);
        JTextField prefix = new JTextField();
        String pref = ctx.getSketch().getSettings().get("binary." + Utils.sanitize(file.getName()) + ".prefix");
        if (pref == null || pref.equals("")) {
            pref = Utils.sanitize(file.getName());
        }
        prefix.setText(pref);
        components.put("prefix", prefix);
        constraints.gridx = 1;
        constraints.weightx = 0.9d;
        options.add(prefix, constraints);
        constraints.gridy++;

        constraints.gridx = 0;
        constraints.weightx = 0.1d;
        options.add(new JLabel("Variable type:"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 0.9d;
        KVPair[] types = new KVPair[6];
        types[0] = new KVPair<String, String>("uint8_t", "8-bit, unsigned");
        types[1] = new KVPair<String, String>("int8_t", "8-bit, signed");
        types[2] = new KVPair<String, String>("uint16_t", "16-bit, unsigned");
        types[3] = new KVPair<String, String>("int16_t", "16-bit, signed");
        types[4] = new KVPair<String, String>("uint32_t", "32-bit, unsigned");
        types[5] = new KVPair<String, String>("int32_t", "32-bit, signed");
        JComboBox<KVPair> vartype = new JComboBox<KVPair>(types);
        vartype.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                KVPair<String, String> selPair = (KVPair<String, String>)vartype.getSelectedItem();
                if (selPair.getKey().equals("uint8_t") || selPair.getKey().equals("int8_t")) {
                    disableComponent("endian");
                } else {
                    enableComponent("endian");
                }
            }
        });

        components.put("type", vartype);
        options.add(vartype, constraints);
        constraints.gridy++;

        constraints.gridx = 0;
        constraints.weightx = 0.1d;
        options.add(new JLabel("Endian:"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 0.9d;
        KVPair[] endians = new KVPair[2];
        endians[0] = new KVPair<String, String>("big", "Big-Endian");
        endians[1] = new KVPair<String, String>("little", "Little-Endian");
        JComboBox<KVPair> endian = new JComboBox<KVPair>(endians);


        components.put("endian", endian);
        options.add(endian, constraints);
        constraints.gridy++;




        String thetype = ctx.getSketch().getSettings().get("binary." + Utils.sanitize(file.getName()) + ".type");
        if (thetype != null) {
            int i = 0;
            for (KVPair p : types) {
                if (p.getKey().equals(thetype)) {
                    vartype.setSelectedIndex(i);
                    if (p.getKey().equals("uint8_t") || p.getKey().equals("int8_t")) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                disableComponent("endian");
                            }
                        });
                    } else {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                enableComponent("endian");
                            }
                        });
                    }
                }
                i++;
            }
        }
            
        String theendian = ctx.getSketch().getSettings().get("binary." + Utils.sanitize(file.getName()) + ".endian");
        if (theendian != null) {
            int i = 0;
            for (KVPair p : endians) {
                if (p.getKey().equals(theendian)) {
                    endian.setSelectedIndex(i);
                }
                i++;
            }
        }

    }

    void forceLocation() {
        Dimension d = getSize();

        JFrame owner = ((SwingGui)ctx.getGui()).getFrame();

        Point oloc = owner.getLocationOnScreen();
        Dimension osz = owner.getSize();

        Point nloc = new Point(
            oloc.x + osz.width/2  - d.width/2,
            oloc.y + osz.height/2 - d.height/2
        );

        setLocation(nloc);
    }




    public void enableComponent(String name) {
        JComponent c = components.get(name);
        if (c != null) {
            c.setEnabled(true);
        }
    }

    public void disableComponent(String name) {
        JComponent c = components.get(name);
        if (c != null) {
            c.setEnabled(false);
        }
    }





    class KVPair<K,V> implements Comparable {
        public K key;
        public V value;

        public KVPair(K k, V v) {
            key = k;
            value = v;
        }

        public String toString() {
            return (String)value;
        }

        public int compareTo(Object o) {
            return 0;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    }




}
