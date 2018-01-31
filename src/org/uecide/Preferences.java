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

import org.uecide.plugin.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.Method;
import java.net.URI;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.event.*;

import javax.swing.tree.*;

import org.json.simple.*;
import org.json.simple.parser.*;


import say.swing.*;

import de.muntjak.tinylookandfeel.*;

public class Preferences extends JDialog implements TreeSelectionListener {

    Editor editor;
    ScrollablePanel advancedTreeBody;

    PropertyFile changedPrefs = new PropertyFile();

    static class KVPair implements Comparable {
        String key;
        String value;

        public KVPair(String k, String v) {
            key = k;
            value = v;
        }

        public String toString() {
            return value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public int compareTo(Object b) {
            KVPair bo = (KVPair)b;
            return value.compareTo(bo.getValue());
        }
    }

    static public String fontToString(Font f) {
        String font = f.getName();
        String style = "";
        font += ",";

        if((f.getStyle() & Font.BOLD) != 0) {
            style += "bold";
        }

        if((f.getStyle() & Font.ITALIC) != 0) {
            style += "italic";
        }

        if(style.equals("")) {
            style = "plain";
        }

        font += style;
        font += ",";
        font += Integer.toString(f.getSize());
        return font;
    }

    static public Font stringToFont(String value) {
        if(value == null) {
            value = "Monospaced,plain,12";
        }

        String[] pieces = value.split(",");

        if(pieces.length != 3) {
            return new Font("Monospaced", Font.PLAIN, 12);
        }

        String name = pieces[0];
        int style = Font.PLAIN;  // equals zero

        if(pieces[1].indexOf("bold") != -1) {
            style |= Font.BOLD;
        }

        if(pieces[1].indexOf("italic") != -1) {
            style |= Font.ITALIC;
        }

        int size;

        try {
            size = Integer.parseInt(pieces[2]);

            if(size <= 0) size = 12;
        } catch(Exception e) {
            size = 12;
        }

        Font font = new Font(name, style, size);

        if(font == null) {
            font = new Font("Monospaced", Font.PLAIN, 12);
        }

        return font;
    }

    public Dimension getMinimumSize() {
        return new Dimension(700, 500);
    }

    public Dimension getPreferredSize() {
        return new Dimension(700, 500);
    }

    public Preferences(Editor ed) {
        editor = ed;

        // setup dialog for the prefs

        setTitle(Base.i18n.string("win.preferences"));
        setResizable(true);
        setLayout(new BorderLayout());
        setModalityType(ModalityType.APPLICATION_MODAL);
        JPanel outer = new JPanel();

        outer.setBorder(new EmptyBorder(10, 10, 10, 10));
        outer.setLayout(new BorderLayout());

        Box buttonLine = Box.createHorizontalBox();
        buttonLine.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton(Base.i18n.string("misc.cancel"));
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                disposeFrame();
            }
        });
        JButton okButton = new JButton(Base.i18n.string("misc.ok"));
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                applyFrame();
                disposeFrame();
            }
        });

        buttonLine.add(cancelButton);
        buttonLine.add(okButton);

        outer.add(buttonLine, BorderLayout.SOUTH);

        for(Class<?> pluginClass : Base.plugins.values()) {
            try {
                Method getPreferencesTree = pluginClass.getMethod("getPreferencesTree");
                if (getPreferencesTree != null) {
                    PropertyFile pf = (PropertyFile)(getPreferencesTree.invoke(null));
                    Base.preferencesTree.mergeData(pf);
                }
            } catch (Exception e) {
            }
        }

        for(Class<?> pluginClass : Base.lookAndFeels.values()) {
            try {
                Method getPreferencesTree = pluginClass.getMethod("getPreferencesTree");
                if (getPreferencesTree != null) {
                    PropertyFile pf = (PropertyFile)(getPreferencesTree.invoke(null));
                    Base.preferencesTree.mergeData(pf);
                }
            } catch (Exception e) {
            }
        }

        JPanel treeSettings = new JPanel();
        populateAdvancedSettings(treeSettings);
        outer.add(treeSettings, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                disposeFrame();
            }
        });

        add(outer, BorderLayout.CENTER);

        ActionListener disposer = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                disposeFrame();
            }
        };
        Base.registerWindowCloseKeys(getRootPane(), disposer);
//        Base.setIcon(this);

        // handle window closing commands for ctrl/cmd-W or hitting ESC.

        pack();
        if (editor != null) setLocationRelativeTo(editor);
        setVisible(true);
    }

    static class PrefTreeEntry {
        String key;
        String name;
    
        PrefTreeEntry(String k, String n) {
            key = k;
            name = n;
        }
        
        public String getName() { return name; }
        public String getKey() { return key; }
        public String toString() { return name; }
    }

    public void populateAdvancedSettings(JPanel p) {
        p.setLayout(new BorderLayout());

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(Base.i18n.string("tree.preferences"));
        DefaultTreeModel treeModel = new DefaultTreeModel(root);

        JTree tree = new JTree(treeModel);
        advancedTreeBody = new ScrollablePanel();
        advancedTreeBody.setLayout(new GridBagLayout());

        
        JScrollPane tscroll = new JScrollPane(tree);
        JScrollPane bscroll = new JScrollPane(advancedTreeBody, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tscroll, bscroll);

        advancedTreeBody.setAlignmentX(Component.LEFT_ALIGNMENT);

        populatePreferencesTree(root, Base.preferencesTree, null);

        tree.expandPath(new TreePath(root.getPath()));
        tree.setRootVisible(true);

        p.add(split, BorderLayout.CENTER);
        tree.addTreeSelectionListener(this);
    }

    public void populatePreferencesTree(DefaultMutableTreeNode root, PropertyFile pf, String parents) {

        for (String prop : pf.childKeys()) {
            if (pf.keyExists(prop + ".type")) {
                if (pf.get(prop + ".type").equals("section")) {
                    String par = prop;
                    if (parents != null) {
                        par = parents + "." + prop;
                    }
                    PrefTreeEntry pe = new PrefTreeEntry(par, pf.get(prop + ".name"));
                    DefaultMutableTreeNode node = new DefaultMutableTreeNode(pe);
                    root.add(node);
                    populatePreferencesTree(node, pf.getChildren(prop), par);
                }
            }
        }
    }


    /**
     * Close the window after an OK or Cancel.
     */
    protected void disposeFrame() {
        dispose();
    }


    /**
     * Change internal settings based on what was chosen in the prefs,
     * then send a message to the editor saying that it's time to do the same.
     */
    protected void applyFrame() {

        // Two special areas need to have any old subkeys removed - the port list and the library list.

        PropertyFile sub = changedPrefs.getChildren("editor.serial.ports");
        if (sub.size() > 0) {
            Base.preferences.removeAll("editor.serial.ports");
        }

        sub = changedPrefs.getChildren("locations.library");
        if (sub.size() > 0) {
            Base.preferences.removeAll("locations.library");
        }

        Base.preferences.mergeData(changedPrefs);
        Base.preferences.save();
        Base.applyPreferences();
        Base.cleanAndScanAllSettings();
    }

    @SuppressWarnings("unchecked")
    public Box addPreferenceEntry(final String key) {

        final String fkey = key;
        Box b = Box.createHorizontalBox();
        b.setAlignmentX(Component.LEFT_ALIGNMENT);

        String type = Base.preferencesTree.get(key + ".type");
        if (type == null) {
            return null;
        }

        String name = Base.preferencesTree.get(key + ".name");
        if (name == null) {
            return null;
        }

        // Load the default setting
        String def = Base.preferencesTree.getPlatformSpecific(key + ".default");
        if (def == null) {
            def = "";
        }

        // Override it with our saved preference
        String value = Base.preferences.get(key);
        if (value == null) {
            value = def;
        }

        // Override it again with whatever we have already edited
        String preset = changedPrefs.get(fkey);
        if (preset != null) {
            value = preset;
        }

        if (type.equals("section")) {
            return null;
        } else if (type.equals("dirselect")) {
            b.add(new JLabel(name + ": "));
            final JTextField f = new JTextField();
            f.setText(value);

            f.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    changedPrefs.set(fkey, f.getText());
                }
            });

            f.addKeyListener(new KeyListener() {
                public void keyReleased(KeyEvent e) {
                    changedPrefs.set(fkey, f.getText());
                }

                public void keyPressed(KeyEvent e) {
                }

                public void keyTyped(KeyEvent e) {
                }
            });

            b.add(f);
            JButton btn = new JButton("Select...");
            btn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JFileChooser fc = new JFileChooser();
                    fc.setDialogType(JFileChooser.OPEN_DIALOG);
                    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    fc.setCurrentDirectory(new File(f.getText()));
                    fc.setDialogTitle("Select Directory");
                    int n = fc.showDialog(Preferences.this, "Select");
                    if (n == JFileChooser.APPROVE_OPTION) {
                        f.setText(fc.getSelectedFile().getAbsolutePath());
                        changedPrefs.set(fkey, f.getText());
                    }
                }
            });
            b.add(btn);
        } else if (type.equals("fileselect")) {
            b.add(new JLabel(name + ": "));
            final JTextField f = new JTextField();
            f.setText(value);

            f.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    changedPrefs.set(fkey, f.getText());
                }
            });

            f.addKeyListener(new KeyListener() {
                public void keyReleased(KeyEvent e) {
                    changedPrefs.set(fkey, f.getText());
                }

                public void keyPressed(KeyEvent e) {
                }

                public void keyTyped(KeyEvent e) {
                }
            });

            b.add(f);
            JButton btn = new JButton("Select...");
            btn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JFileChooser fc = new JFileChooser();
                    fc.setDialogType(JFileChooser.OPEN_DIALOG);
                    fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                    fc.setCurrentDirectory(new File(f.getText()));
                    fc.setDialogTitle("Select File");
                    int n = fc.showDialog(Preferences.this, Base.i18n.string("misc.select"));
                    if (n == JFileChooser.APPROVE_OPTION) {
                        f.setText(fc.getSelectedFile().getAbsolutePath());
                        changedPrefs.set(fkey, f.getText());
                    }
                }
            });
            b.add(btn);
        } else if (type.equals("fontselect")) {
            b.add(new JLabel(name + ": "));
            final JTextField f = new JTextField();
            f.setText(value);

            f.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    changedPrefs.set(fkey, f.getText());
                }
            });

            f.addKeyListener(new KeyListener() {
                public void keyReleased(KeyEvent e) {
                    changedPrefs.set(fkey, f.getText());
                }

                public void keyPressed(KeyEvent e) {
                }

                public void keyTyped(KeyEvent e) {
                }
            });

            b.add(f);
            JButton btn = new JButton("Select...");
            btn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {


                    JFontChooser fc = new JFontChooser();
                    fc.setSelectedFont(stringToFont(f.getText()));
                    int res = fc.showDialog(Preferences.this);

                    if(res == JFontChooser.OK_OPTION) {
                        Font fnt = fc.getSelectedFont();
                        changedPrefs.setFont(fkey, fnt);
                        f.setText(fontToString(fnt));
                    }
                }
            });
            b.add(btn);
        } else if (type.equals("string")) {
            b.add(new JLabel(name + ": "));
            final JTextField f = new JTextField();
            f.setText(value);

            f.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    changedPrefs.set(fkey, f.getText());
                }
            });

            f.addKeyListener(new KeyListener() {
                public void keyReleased(KeyEvent e) {
                    changedPrefs.set(fkey, f.getText());
                }

                public void keyPressed(KeyEvent e) {
                }

                public void keyTyped(KeyEvent e) {
                }
            });
            b.add(f);
        } else if (type.equals("checkbox")) {
            JCheckBox cb = new JCheckBox(name);
            cb.setSelected(value.equals("true"));
            cb.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JCheckBox ecb = (JCheckBox)e.getSource();
                    changedPrefs.setBoolean(fkey, ecb.isSelected());
                }
            });
            b.add(cb);
        } else if (type.equals("range")) {
            b.add(new JLabel(name + ": "));
            final int vmin = Base.preferencesTree.getInteger(key + ".min");
            final int vmax = Base.preferencesTree.getInteger(key + ".max");
            int ival = vmin;
            try {
                ival = Integer.parseInt(value);
            } catch (Exception ee) {
                ival = vmin;
            }
                
            final JSpinner sb = new JSpinner(new SpinnerNumberModel(
                ival, vmin, vmax, 1
            ));

            sb.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    int val = (Integer)sb.getValue();
                    changedPrefs.setInteger(fkey, val);
                }
            });


            b.add(sb);
        } else if (type.equals("dropdown")) {
            b.add(new JLabel(name + ": "));

            HashMap<String, String> options = new HashMap<String, String>();


            if (Base.preferencesTree.get(key + ".options.script") != null) {
                String source = Base.preferencesTree.getSource(key + ".options.script");
                Context ctx = new Context();;
                ctx.mergeSettings(Base.preferencesTree);
                String keyToExecute = key + ".options.script";
                if (source != null) {
                    if (source.startsWith("board:")) { ctx.setBoard(Base.boards.get(source.substring(6))); } 
                    if (source.startsWith("core:")) { ctx.setCore(Base.cores.get(source.substring(5))); } 
                    if (source.startsWith("compiler:")) { ctx.setCompiler(Base.compilers.get(source.substring(9))); } 
                    keyToExecute = "prefs." + keyToExecute;
                }

                Object hash = ctx.executeKey(keyToExecute);
                if (hash instanceof HashMap) {
                    options = (HashMap<String, String>)hash;
                }

            } else {
                PropertyFile opts = Base.preferencesTree.getChildren(key + ".options");
                String[] keys = opts.childKeys();
                for (String k : keys) {
                    options.put(k, opts.get(k));
                }
            }

            ArrayList<KVPair> kvlist = new ArrayList<KVPair>();
            for (String k : options.keySet()) {
                KVPair kv = new KVPair(k, options.get(k));
                kvlist.add(kv);
            }
            KVPair opList[] = kvlist.toArray(new KVPair[0]);
            Arrays.sort(opList);
            JComboBox cb = new JComboBox(opList);

            for (KVPair op : opList) {
                if (op.getKey().equals(get(key))) {
                    cb.setSelectedItem(op);
                }
            }

            cb.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JComboBox widget = (JComboBox)e.getSource();
                    KVPair data = (KVPair)widget.getSelectedItem();
                    changedPrefs.set(fkey, data.getKey());
                }
            });
            b.add(cb);
        } else if (type.equals("liblist")) {
            JPanel p = new JPanel();
            p.setLayout(new BorderLayout());
            final LibraryLocationList liblist = new LibraryLocationList();

            PropertyFile items = changedPrefs.getChildren(key);

            if (items.size() == 0) {
                items = Base.preferences.getChildren(key);
            }

            String[] subKeys = items.childKeys();
            for (String subKey : subKeys) {
                String itemName = items.get(subKey + ".name");
                String itemPath = items.get(subKey + ".path");
                liblist.addLibraryLocation(itemName, itemPath);
            }

            liblist.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    HashMap<String, String> list = liblist.getLibraryList();
                    for (String k : list.keySet()) {
                        String libName = k.trim();
                        String libPath = list.get(k);
                        String libCode = libName.toLowerCase().replaceAll("\\s+","");

                        changedPrefs.set(key + "." + libCode + ".name", libName);
                        changedPrefs.set(key + "." + libCode + ".path", libPath);
                    }
                }
            });

            p.add(new JLabel(name + ":"), BorderLayout.NORTH);
            p.add(liblist, BorderLayout.CENTER);
            b.add(p);
        } else if (type.equals("portlist")) {
            JPanel p = new JPanel();
            p.setLayout(new BorderLayout());
            final PortList portlist = new PortList();

            PropertyFile items = changedPrefs.getChildren(key);

            if (items.size() == 0) {
                items = Base.preferences.getChildren(key);
            }

            String[] subKeys = items.childKeys();
            for (String subKey : subKeys) {
                String itemName = items.get(subKey);
                portlist.addPort(itemName);
            }

            portlist.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ArrayList<String> list = portlist.getPortList();
                    int i = 0;
                    for (String k : list) {
                        String libName = k.trim();
                        changedPrefs.set(key + "." + i, k);
                        i++;
                    }
                }
            });

            p.add(new JLabel(name + ":"), BorderLayout.NORTH);
            p.add(portlist, BorderLayout.CENTER);
            b.add(p);
        } else {
            b.add(new JLabel(name + ": "));
            b.add(new JLabel(value));
        }

        b.setBorder(new EmptyBorder(2, 2, 2, 2));

        return b;
    }

    static class ScrollablePanel extends JPanel implements Scrollable {
        public Dimension getPreferredScrollableViewportSize() {
            return super.getPreferredSize();
        }

        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
           return 10;
        }

        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return ((orientation == SwingConstants.VERTICAL) ? visibleRect.height : visibleRect.width) - 10;
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    // Interface to the global preferences.  These will first check the user's preferences
    // file for the value and if not found then will get the value from the .default entry
    // for the key in the preferences tree.

    public static String get(String key) {
        String data = Base.preferences.get(key);
        if (data == null) {
            data = Base.preferencesTree.get(key + ".default");
        }
        return data;
    }

    public static String[] getArray(String key) {
        String data = get(key);
        if (data == null || data.equals("")) {
            return null;
        }
        return data.split("::");
    }

    public static Boolean getBoolean(String key) {
        String data = get(key);
        if (data == null || data.equals("")) {
            return false;
        }
        data = data.toLowerCase();
        if (data.startsWith("y") || data.startsWith("t")) {
            return true;
        }
        return false;
    }

    public static Color getColor(String key) {
        Color parsed = Color.GRAY;
        String s = get(key);

        if((s != null) && (s.indexOf("#") == 0)) {
            try {
                parsed = new Color(Integer.parseInt(s.substring(1), 16));
            } catch(Exception e) { }
        }

        return parsed;
    }

    public static File getFile(String key) {
        String data = get(key);
        if (data == null || data.equals("")) {
            return null;
        }
        return new File(data);
    }

    public static String getFontCSS(String key) {
        Font f = getFont(key);

        String out = "font-family: " + f.getFamily() + ";";
        out += " font-size: " + f.getSize() + "px;";

        if (f.isBold()) {
            out += " font-weight: bold;";
        }

        if (f.isItalic()) {
            out += " font-style: italic;";
        }

        return out;
    }

    public static Font getFontNatural(String key) {
        String data = get(key);
        Font font = null;
        if (data == null || data.equals("")) {
            font = Base.preferences.stringToFont("Monospaced,plain,12");
        } else {
            font = Base.preferences.stringToFont(data);
        }
        return font;
    }

    public static Font getFont(String key) {
        String data = get(key);
        Font font = null;
        if (data == null || data.equals("")) {
            font = Base.preferences.stringToFont("Monospaced,plain,12");
        } else {
            font = Base.preferences.stringToFont(data);
        }
        float size = font.getSize();
        int scale = getInteger("theme.fonts.scale");
        if (scale == 0) {
            scale = 100;
        }
        Font out = font.deriveFont(size * (float)scale / 100f);
        if (out.getSize() <= 0) {
            out = font.deriveFont(1f);
        }
        return out;
    }

    public static Integer getInteger(String key) {
        String data = get(key);
        if (data == null || data.equals("")) {
            return 0;
        }
        int val = 0;
        try {
            val = Integer.parseInt(data);
        } catch (Exception e) {
        }
        return val;
    }

    public static void set(String key, String value) { Base.preferences.set(key, value); Base.preferences.saveDelay(); }
    public static void setBoolean(String key, Boolean value) { Base.preferences.setBoolean(key, value); Base.preferences.saveDelay(); }
    public static void setColor(String key, Color value) { Base.preferences.setColor(key, value); Base.preferences.saveDelay(); }
    public static void setFile(String key, File value) { Base.preferences.setFile(key, value); Base.preferences.saveDelay(); }
    public static void setFile(String key, Font value) { Base.preferences.setFont(key, value); Base.preferences.saveDelay(); }
    public static void setInteger(String key, int value) { Base.preferences.setInteger(key, value); Base.preferences.saveDelay(); }

    public static void save() { Base.preferences.save(); }

    public static void unset(String key) { Base.preferences.unset(key); Base.preferences.saveDelay(); }
        
    public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)(e.getPath().getLastPathComponent());
        PrefTreeEntry pe = (PrefTreeEntry)(node.getUserObject());

        PropertyFile pf = Base.preferencesTree.getChildren(pe.getKey());
        advancedTreeBody.removeAll();
        advancedTreeBody.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;

        int size = 0;
        for (String pref : pf.childKeys()) {
            if (pf.keyExists(pref + ".type")) {
                Box b = addPreferenceEntry(pe.getKey() + "." + pref);
                if (b != null) {
                    Dimension s = b.getPreferredSize();
                    size += s.height;
                    advancedTreeBody.add(b, c);
                    c.gridy++;
                }
            }
        }

        Dimension s1 = advancedTreeBody.getPreferredSize();
        advancedTreeBody.setSize(new Dimension(size, s1.width));

        advancedTreeBody.validate();
        advancedTreeBody.repaint();
    }
}



