/*
 * Copyright (c) 2014, Majenko Technologies
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

public class Preferences implements TreeSelectionListener {

    // prompt text stuff

    static final String PROMPT_YES     = "Yes";
    static final String PROMPT_NO      = "No";
    static final String PROMPT_CANCEL  = "Cancel";
    static final String PROMPT_OK      = "OK";
    static final String PROMPT_BROWSE  = "Browse";

    /**
     * Standardized width for buttons. Mac OS X 10.3 wants 70 as its default,
     * Windows XP needs 66, and my Ubuntu machine needs 80+, so 80 seems proper.
     */
    static public int BUTTON_WIDTH  = 80;

    /**
     * Standardized button height. Mac OS X 10.3 (Java 1.4) wants 29,
     * presumably because it now includes the blue border, where it didn't
     * in Java 1.3. Windows XP only wants 23 (not sure what default Linux
     * would be). Because of the disparity, on Mac OS X, it will be set
     * inside a static block.
     */
    static public int BUTTON_HEIGHT = 24;

    // value for the size bars, buttons, etc

    static final int GRID_SIZE     = 33;


    // indents and spacing standards. these probably need to be modified
    // per platform as well, since macosx is so huge, windows is smaller,
    // and linux is all over the map

    static final int GUI_BIG     = 13;
    static final int GUI_BETWEEN = 10;
    static final int GUI_SMALL   = 6;

    // gui elements

    JFrame dialog;
    int wide, high;

    JTextField sketchbookLocationField;
    JTextField externalEditorField;
    JTextField editorFontField;
    JTextField consoleFontField;
    JCheckBox autoAssociateBox;
    JCheckBox useSpacesForTabs;
    JCheckBox visibleTabs;
    JCheckBox checkNewVersion;
    JCheckBox autoSave;
    JCheckBox crashReport;
    JTextField tabSize;
    JCheckBox hideSecondaryToolbar;

    JCheckBox keepFindOpen;

    JTabbedPane tabs;

    JTextField dataLocationField;
    JCheckBox  createBackups;
    JTextField backupNumber;

    Editor editor;

    JComboBox selectedTheme;
    JComboBox selectedEditorTheme;
    JComboBox selectedIconTheme;
    JCheckBox useSystemDecorator;

    ScrollablePanel advancedTreeBody;

    JCheckBox dlgMissingLibraries;

    JList extraPortList;
    DefaultListModel extraPortListModel = new DefaultListModel();
    JTextField portInput;

    public static TreeMap<String, String>themes;

    PropertyFile changedPrefs = new PropertyFile();

    JTable libraryLocationTable;
    class LibraryDetail {
        public String key;
        public String description;
        public String path;
    };

    class KVPair implements Comparable {
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

    class LibraryTableModel extends AbstractTableModel {
        private String[] columnNames = {"Key",
                                        "Description",
                                        "Path"
                                       };
        private ArrayList<LibraryDetail> data = new ArrayList<LibraryDetail>();

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return data.size();
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            switch(col) {
            case 0:
                return data.get(row).key;

            case 1:
                return data.get(row).description;

            case 2:
                return data.get(row).path;
            }

            return null;
        }

        /*
         * JTable uses this method to determine the default renderer/
         * editor for each cell.  If we didn't implement this method,
         * then the last column would contain text ("true"/"false"),
         * rather than a check box.
         */
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        /*
         * Don't need to implement this method unless your table's
         * editable.
         */
        public boolean isCellEditable(int row, int col) {
            return true;
        }

        /*
         * Don't need to implement this method unless your table's
         * data can change.
         */
        public void setValueAt(Object value, int row, int col) {
            if(row >= data.size()) {
                LibraryDetail lib = new LibraryDetail();

                switch(col) {
                case 0:
                    lib.key = (String)value;
                    break;

                case 1:
                    lib.description = (String)value;
                    break;

                case 2:
                    lib.path = (String)value;
                    break;

                default:
                    return;
                }

                data.add(lib);
            } else {
                switch(col) {
                case 0:
                    data.get(row).key = (String)value;
                    break;

                case 1:
                    data.get(row).description = (String)value;
                    break;

                case 2:
                    data.get(row).path = (String)value;
                    break;

                default:
                    return;
                }
            }

            fireTableCellUpdated(row, col);
        }
        public void deleteRow(int row) {
            data.remove(row);
            fireTableDataChanged();
        }

        public void addNewRow(String key, String name, String path) {
            LibraryDetail lib = new LibraryDetail();
            lib.key = key;
            lib.description = name;
            lib.path = path;
            data.add(lib);
            fireTableDataChanged();
        }

    }

    private LibraryTableModel libraryLocationModel = new LibraryTableModel();

    private JButton deleteSelectedLibraryEntry;
    // the calling editor, so updates can be applied

    // data model

    static PropertyFile properties;

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



    static protected void init() {
        properties = new PropertyFile(Base.getDataFile("preferences.txt"), Base.getContentFile("lib/preferences.txt"));
    }


    public Preferences(Editor ed) {
        editor = ed;

        // setup dialog for the prefs

        dialog = new JFrame("Preferences");
        dialog.setResizable(false);

        Container pane = dialog.getContentPane();
        pane.setLayout(new BorderLayout());

        Box outerBox = Box.createVerticalBox();
        pane.add(outerBox);

        tabs = new JTabbedPane();
        outerBox.add(tabs);
        Box buttonLine = Box.createHorizontalBox();
        outerBox.add(buttonLine);
        buttonLine.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton(Translate.t("Cancel"));
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                disposeFrame();
            }
        });
        JButton okButton = new JButton(Translate.t("OK"));
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                applyFrame();
                disposeFrame();
            }
        });

        buttonLine.add(cancelButton);
        buttonLine.add(okButton);

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

        JPanel advancedSettings = new JPanel(new GridBagLayout());
        JPanel librarySettings = new JPanel(new GridBagLayout());
        JPanel treeSettings = new JPanel(); //new GridBagLayout());

        advancedSettings.setBorder(new EmptyBorder(5, 5, 5, 5));
        librarySettings.setBorder(new EmptyBorder(5, 5, 5, 5));

        tabs.add(Translate.t("Libraries"), librarySettings);
        tabs.add(Translate.t("Advanced"), treeSettings);

        populateLibrarySettings(librarySettings);
        populateAdvancedSettings(treeSettings);

        if(Base.isPosix()) {
            JPanel serialSettings = new JPanel(new GridBagLayout());
            serialSettings.setBorder(new EmptyBorder(5, 5, 5, 5));
            tabs.add(Translate.t("Serial"), serialSettings);
            populateSerialSettings(serialSettings);
        }

        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                disposeFrame();
            }
        });

        ActionListener disposer = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                disposeFrame();
            }
        };
        Base.registerWindowCloseKeys(dialog.getRootPane(), disposer);
        Base.setIcon(dialog);


        // handle window closing commands for ctrl/cmd-W or hitting ESC.


        dialog.pack();
        if (editor != null) dialog.setLocationRelativeTo(editor);

    }

    class PrefTreeEntry {
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

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Preferences");
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
        tree.setRootVisible(false);

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

    public void populateSerialSettings(JPanel p) {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 3;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;

        JLabel label = new JLabel("Extra serial ports:");
        p.add(label, c);
        c.gridy++;

        extraPortList = new JList(extraPortListModel);

        JScrollPane jsp = new JScrollPane(extraPortList);

        p.add(jsp, c);
        c.gridy++;
        c.gridwidth = 1;
        portInput = new JTextField("/dev/");
        p.add(portInput, c);

        c.weightx = 0.2;
        c.gridx++;
        JButton add = new JButton("Add");
        add.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String t = portInput.getText();

                if(!t.startsWith("/dev/")) {
                    return;
                }

                for(Enumeration en = extraPortListModel.elements(); en.hasMoreElements();) {
                    String s = (String)en.nextElement();

                    if(s.equals(t)) {
                        return;
                    }
                }

                extraPortListModel.addElement(t);
                portInput.setText("/dev/");
            }
        });
        p.add(add, c);

        c.weightx = 0.2;
        c.gridx++;
        JButton del = new JButton("Delete");
        del.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int sel = extraPortList.getSelectedIndex();

                while(sel >= 0) {
                    extraPortListModel.remove(sel);
                    sel = extraPortList.getSelectedIndex();
                }
            }
        });
        p.add(del, c);

    }

    public void populateEditorSettings(JPanel p) {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 3;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 0;

        JLabel label;
        JButton button;

        c.gridwidth = 1;
        label = new JLabel("Window theme:");
        p.add(label, c);
        c.gridx = 1;
        c.gridwidth = 1;


        themes = new TreeMap<String, String>();
        UIManager.LookAndFeelInfo[] lafInfo = UIManager.getInstalledLookAndFeels();

        for(UIManager.LookAndFeelInfo info : lafInfo) {
            themes.put(info.getName(), info.getClassName());
        }

        // JTattoo collection
        themes.put("Acryl",     "com.jtattoo.plaf.acryl.AcrylLookAndFeel");
        themes.put("Aero",      "com.jtattoo.plaf.aero.AeroLookAndFeel");
        themes.put("Aluminium", "com.jtattoo.plaf.aluminium.AluminiumLookAndFeel");
        themes.put("Bernstein", "com.jtattoo.plaf.bernstein.BernsteinLookAndFeel");
        themes.put("Fast",      "com.jtattoo.plaf.fast.FastLookAndFeel");
        themes.put("Graphite",  "com.jtattoo.plaf.graphite.GraphiteLookAndFeel");
        themes.put("HiFi",      "com.jtattoo.plaf.hifi.HiFiLookAndFeel");
        themes.put("Luna",      "com.jtattoo.plaf.luna.LunaLookAndFeel");
        themes.put("McWin",     "com.jtattoo.plaf.mcwin.McWinLookAndFeel");
        themes.put("Mint",      "com.jtattoo.plaf.mint.MintLookAndFeel");
        themes.put("Noire",     "com.jtattoo.plaf.noire.NoireLookAndFeel");
        themes.put("Smart",     "com.jtattoo.plaf.smart.SmartLookAndFeel");

        // The fifesoft Windows LaF collection is only available on Windows.
        if(Base.isWindows()) {
            themes.put("Office 2003", "org.fife.plaf.Office2003.Office2003LookAndFeel");
            themes.put("Office XP", "org.fife.plaf.OfficeXP.OfficeXPLookAndFeel");
            themes.put("Visual Studio 2005", "org.fife.plaf.VisualStudio2005.VisualStudio2005LookAndFeel");
        }

        themes.put("Liquid",    "com.birosoft.liquid.LiquidLookAndFeel");

        // TinyLAF collection

        de.muntjak.tinylookandfeel.ThemeDescription[] tinyThemes = de.muntjak.tinylookandfeel.Theme.getAvailableThemes();

        for(de.muntjak.tinylookandfeel.ThemeDescription td : tinyThemes) {
            String themeName = td.getName();

            if(themeName.equals("")) {
                continue;
            }

            themes.put("Tiny: " + themeName, "de.muntjak.tinylookandfeel.TinyLookAndFeel;" + themeName);
        }

        String[] keys = themes.keySet().toArray(new String[0]);
        Arrays.sort(keys);


        selectedTheme = new JComboBox(keys);
        selectedTheme.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        try {
                            String value = (String)selectedTheme.getSelectedItem();
                            String laf = themes.get(value);
                            String lafTheme = "";

                            if(laf.indexOf(";") > -1) {
                                lafTheme = laf.substring(laf.lastIndexOf(";") + 1);
                                laf = laf.substring(0, laf.lastIndexOf(";"));
                            }

                            if(laf.startsWith("de.muntjak.tinylookandfeel.")) {

                                de.muntjak.tinylookandfeel.ThemeDescription[] tinyThemes = de.muntjak.tinylookandfeel.Theme.getAvailableThemes();
                                URI themeURI = null;

                                for(de.muntjak.tinylookandfeel.ThemeDescription td : tinyThemes) {
                                    if(td.getName().equals(lafTheme)) {
                                        de.muntjak.tinylookandfeel.Theme.loadTheme(td);
                                        break;
                                    }
                                }
                            }

                            if(laf.startsWith("com.jtattoo.plaf.")) {
                                Properties props = new Properties();
                                props.put("windowDecoration", useSystemDecorator.isSelected() ? "on" : "off");
                                props.put("logoString", "UECIDE");
                                props.put("textAntiAliasing", "on");

                                Class<?> cls = Class.forName(laf);
                                Class[] cArg = new Class[1];
                                cArg[0] = Properties.class;
                                Method mth = cls.getMethod("setCurrentTheme", cArg);
                                mth.invoke(cls, props);
                            } else {
                            }

                            UIManager.setLookAndFeel(laf);
                            SwingUtilities.updateComponentTreeUI(dialog);
                            Base.updateLookAndFeel();
//                            dialog.pack();
                            if (editor != null) dialog.setLocationRelativeTo(editor);
                            
                        } catch(Exception ignored) {
                        }
                    }
                });
            }
        });

        String currentLaf = Base.preferences.get("theme.window");

        for(String k : keys) {
            if(themes.get(k).equals(currentLaf)) {
                selectedTheme.setSelectedItem(k);
            }
        }

        p.add(selectedTheme, c);

        c.gridx = 2;

        useSystemDecorator = new JCheckBox(Translate.t("Use System Decorator"));
        p.add(useSystemDecorator, c);
        useSystemDecorator.setSelected(Base.preferences.getBoolean("theme.window_system"));

        useSystemDecorator.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        String value = (String)selectedTheme.getSelectedItem();
                        String laf = themes.get(value);
                        String lafTheme = "";

                        if(laf.indexOf(";") > -1) {
                            lafTheme = laf.substring(laf.lastIndexOf(";") + 1);
                            laf = laf.substring(0, laf.lastIndexOf(";"));
                        }

                        try {
                            if(laf.startsWith("de.muntjak.tinylookandfeel.")) {
                                de.muntjak.tinylookandfeel.ThemeDescription[] tinyThemes = de.muntjak.tinylookandfeel.Theme.getAvailableThemes();
                                URI themeURI = null;

                                for(de.muntjak.tinylookandfeel.ThemeDescription td : tinyThemes) {
                                    if(td.getName().equals(lafTheme)) {
                                        de.muntjak.tinylookandfeel.Theme.loadTheme(td);
                                        break;
                                    }
                                }
                            }

                            if(laf.startsWith("com.jtattoo.plaf.")) {
                                Properties props = new Properties();
                                props.put("windowDecoration", useSystemDecorator.isSelected() ? "off" : "on");
                                props.put("logoString", "UECIDE");
                                props.put("textAntiAliasing", "on");


                                Class<?> cls = Class.forName(laf);
                                Class[] cArg = new Class[1];
                                cArg[0] = Properties.class;
                                Method mth = cls.getMethod("setCurrentTheme", cArg);
                                mth.invoke(cls, props);
                            }

                            UIManager.setLookAndFeel(laf);
                            SwingUtilities.updateComponentTreeUI(dialog);
                            Base.updateLookAndFeel();
//                            dialog.pack();
                            if (editor != null) dialog.setLocationRelativeTo(editor);
                        } catch(Exception ignored) {
                        }
                    }
                });
            }
        });

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        label = new JLabel("Editor Theme:");
        p.add(label, c);
        c.gridwidth = 1;
        c.gridx = 1;


        ArrayList<KVPair> tlist = new ArrayList<KVPair>();
        HashMap<String, String> themes = Base.getThemeList();
        KVPair selectedPair = null;

        for(String t : themes.keySet()) {
            KVPair kv = new KVPair(t, themes.get(t));

            if(t.equals(Base.preferences.get("theme.editor", "default"))) {
                selectedPair = kv;
            }

            tlist.add(kv);
        }

        KVPair[] tarr = tlist.toArray(new KVPair[tlist.size()]);
        selectedEditorTheme = new JComboBox(tarr);
        selectedEditorTheme.setSelectedItem(selectedPair);

        p.add(selectedEditorTheme, c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        label = new JLabel("Icon Theme:");
        p.add(label, c);
        c.gridwidth = 1;
        c.gridx = 1;

        selectedIconTheme = new JComboBox(Base.iconSets.keySet().toArray(new String[0]));
        selectedIconTheme.setSelectedItem(Base.iconSet);

        p.add(selectedIconTheme, c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        label = new JLabel("Sketchbook location:");
        p.add(label, c);
        c.gridwidth = 2;
        c.gridy++;
        sketchbookLocationField = new JTextField(40);
        p.add(sketchbookLocationField, c);

        sketchbookLocationField.setEditable(false);
        button = new JButton(PROMPT_BROWSE);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                File dflt = new File(sketchbookLocationField.getText());
                File file = Base.selectFolder("Select new sketchbook location", dflt, dialog);

                if(file != null) {
                    sketchbookLocationField.setText(file.getAbsolutePath());
                }
            }
        });
        c.gridx = 2;
        c.gridwidth = 1;
        p.add(button, c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        label = new JLabel("External editor command: ");
        p.add(label, c);

        c.gridy++;
        c.gridwidth = 2;
        externalEditorField = new JTextField(40);
        externalEditorField.setText(Base.preferences.get("editor.external.command"));
        p.add(externalEditorField, c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        label = new JLabel("Editor font: ");
        p.add(label, c);

        c.gridy++;
        c.gridwidth = 2;
        editorFontField = new JTextField(40);
        editorFontField.setEditable(false);
        p.add(editorFontField, c);

        editorFontField.setText(Base.preferences.get("theme.fonts.editor"));

        JButton selectEditorFont = new JButton(Translate.t("Select Font..."));
        c.gridx = 2;
        c.gridwidth = 1;
        p.add(selectEditorFont, c);

        final Container parent = p;
        selectEditorFont.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFontChooser fc = new JFontChooser();
                fc.setSelectedFont(stringToFont(editorFontField.getText()));
                int res = fc.showDialog(parent);

                if(res == JFontChooser.OK_OPTION) {
                    Font f = fc.getSelectedFont();
                    editorFontField.setText(Preferences.fontToString(f));
                }
            }
        });

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;

        label = new JLabel("Console font: ");
        p.add(label, c);
        c.gridy++;
        c.gridwidth = 2;

        consoleFontField = new JTextField(40);
        consoleFontField.setEditable(false);
        p.add(consoleFontField, c);

        consoleFontField.setText(Base.preferences.get("theme.fonts.console"));

        JButton selectConsoleFont = new JButton(Translate.t("Select Font..."));
        c.gridx = 2;
        c.gridwidth = 1;
        p.add(selectConsoleFont, c);

        selectConsoleFont.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFontChooser fc = new JFontChooser();
                fc.setSelectedFont(stringToFont(consoleFontField.getText()));
                int res = fc.showDialog(parent);

                if(res == JFontChooser.OK_OPTION) {
                    Font f = fc.getSelectedFont();
                    consoleFontField.setText(Preferences.fontToString(f));
                }
            }
        });
        c.gridx = 0;

        if(Base.isWindows()) {
            c.gridy++;
            autoAssociateBox =
                new JCheckBox("Automatically associate .pde files with " + Base.theme.get("product.cap"));
            p.add(autoAssociateBox, c);
        }

    }

    public void populateLocationSettings(JPanel p) {
        JLabel lab;
        JButton but;
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridheight = 1;
        c.gridy = 0;

        c.gridx = 0;
        c.gridwidth = 2;
        lab = new JLabel(Translate.t("Data Location"));
        p.add(lab, c);
        c.gridy++;
        c.gridwidth = 1;
        dataLocationField = new JTextField(40);
        dataLocationField.setEditable(false);
        p.add(dataLocationField, c);
        c.gridx = 1;
        but = new JButton(Translate.t("Select Folder..."));
        but.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                File dflt = new File(dataLocationField.getText());
                File file = Base.selectFolder("Select new data location", dflt, dialog);

                if(file != null) {
                    dataLocationField.setText(file.getAbsolutePath());
                }
            }
        });
        p.add(but, c);
        c.gridy++;

        c.gridx = 0;
        c.gridwidth = 1;
        lab = new JLabel(Translate.t("Changing these settings will require a restart of the IDE."));
        p.add(lab, c);
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        lab = new JLabel(Translate.t("You will also have to reinstall any plugins, boards, cores and compilers."));
        p.add(lab, c);
        c.gridy++;

    }

    public Dimension getPreferredSize() {
        return new Dimension(wide, high);
    }


    // .................................................................


    /**
     * Close the window after an OK or Cancel.
     */
    protected void disposeFrame() {
        dialog.dispose();
    }


    /**
     * Change internal settings based on what was chosen in the prefs,
     * then send a message to the editor saying that it's time to do the same.
     */
    protected void applyFrame() {
        Base.preferences.mergeData(changedPrefs);
        Base.preferences.save();
        Base.applyPreferences();
        Base.cleanAndScanAllSettings();
        Editor.refreshAllEditors();
    }


    protected void showFrame() {
        dialog.setVisible(true);
    }


    private void populateLibrarySettings(JPanel p) {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 3;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;

        JLabel label = new JLabel(Translate.t("Categorized Library Locations"));
        p.add(label, c);
        c.gridy++;


        libraryLocationTable = new JTable(libraryLocationModel);
        JScrollPane jsp = new JScrollPane(libraryLocationTable);
        p.add(jsp, c);
        c.gridy++;
        c.gridwidth = 1;

        libraryLocationTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event) {
                int row = libraryLocationTable.getSelectedRow();

                if(row >= 0) {
                    deleteSelectedLibraryEntry.setEnabled(true);
                } else {
                    deleteSelectedLibraryEntry.setEnabled(false);
                }
            }
        });

        deleteSelectedLibraryEntry = new JButton(Translate.t("Delete selected entry"));
        deleteSelectedLibraryEntry.setEnabled(false);
        deleteSelectedLibraryEntry.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                int row = libraryLocationTable.getSelectedRow();

                if(row >= 0) {
                    libraryLocationModel.deleteRow(row);
                }

                row = libraryLocationTable.getSelectedRow();

                if(row >= 0) {
                    deleteSelectedLibraryEntry.setEnabled(true);
                } else {
                    deleteSelectedLibraryEntry.setEnabled(false);
                }
            }
        });
        c.gridx = 0;
        p.add(deleteSelectedLibraryEntry, c);

        JButton addDir = new JButton(Translate.t("Add subfolders"));
        addDir.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addLibrarySubFolders();
            }
        });
        c.gridx = 1;
        p.add(addDir, c);
        JButton addNewRow = new JButton(Translate.t("Add new entry"));
        addNewRow.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                addLibraryEntry();
            }
        });
        c.gridx = 2;
        p.add(addNewRow, c);
    }

    public void addLibraryEntry() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int r = fc.showOpenDialog(dialog);

        if(r == JFileChooser.APPROVE_OPTION) {
            File dir = fc.getSelectedFile();

            if(!dir.isDirectory()) {
                return;
            }

            String name = dir.getName();
            String code = "";

            for(int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);

                if(c >= 'a' && c <= 'z') {
                    code += c;
                } else if(c >= 'A' && c <= 'Z') {
                    code += Character.toLowerCase(c);
                } else if(c >= '0' && c <= '9') {
                    code += c;
                }
            }

            if(code.equals("")) {
                Random rng = new Random();
                code += (char)(rng.nextInt(26) + 'a');
                code += (char)(rng.nextInt(26) + 'a');
                code += (char)(rng.nextInt(26) + 'a');
                code += (char)(rng.nextInt(26) + 'a');
                code += (char)(rng.nextInt(26) + 'a');
                code += (char)(rng.nextInt(26) + 'a');
                code += (char)(rng.nextInt(26) + 'a');
                code += (char)(rng.nextInt(26) + 'a');
            }

            libraryLocationModel.addNewRow(code, name, dir.getAbsolutePath());
        }
    }

    public void addLibrarySubFolders() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int r = fc.showOpenDialog(dialog);

        if(r == JFileChooser.APPROVE_OPTION) {
            File dir = fc.getSelectedFile();

            if(!dir.isDirectory()) {
                return;
            }

            File[] files = dir.listFiles();

            for(File file : files) {
                if(file.isDirectory()) {
                    String name = file.getName();
                    String code = "";

                    for(int i = 0; i < name.length(); i++) {
                        char c = name.charAt(i);

                        if(c >= 'a' && c <= 'z') {
                            code += c;
                        } else if(c >= 'A' && c <= 'Z') {
                            code += Character.toLowerCase(c);
                        } else if(c >= '0' && c <= '9') {
                            code += c;
                        }
                    }

                    if(code.equals("")) {
                        Random rng = new Random();
                        code += (char)(rng.nextInt(26) + 'a');
                        code += (char)(rng.nextInt(26) + 'a');
                        code += (char)(rng.nextInt(26) + 'a');
                        code += (char)(rng.nextInt(26) + 'a');
                        code += (char)(rng.nextInt(26) + 'a');
                        code += (char)(rng.nextInt(26) + 'a');
                        code += (char)(rng.nextInt(26) + 'a');
                        code += (char)(rng.nextInt(26) + 'a');
                    }

                    libraryLocationModel.addNewRow(code, name, file.getAbsolutePath());
                }
            }
        }
    }

    public void populateDialogSettings(JPanel p) {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;

        p.add(new JLabel("Enable or disable different popup dialogs here."), c);

        c.gridy++;

        dlgMissingLibraries = new JCheckBox("Suggest installing missing libraries");
        dlgMissingLibraries.setSelected(Base.preferences.getBoolean("editor.dialog.missinglibs"));
        p.add(dlgMissingLibraries, c);
        
    }

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

        ArrayList<Box> blist = new ArrayList<Box>();
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
//        dialog.pack();
    }

    @SuppressWarnings("unchecked")
    public Box addPreferenceEntry(String key) {

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
                    int n = fc.showDialog(dialog, "Select");
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
                    int n = fc.showDialog(dialog, "Select");
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
                    int res = fc.showDialog(dialog);

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
                System.err.println("Source for " + key + " = " + source);
                ctx.mergeSettings(Base.preferencesTree);
                String keyToExecute = key + ".options.script";
                if (source != null) {
                    if (source.startsWith("board:")) { ctx.setBoard(Base.boards.get(source.substring(6))); } 
                    if (source.startsWith("core:")) { ctx.setCore(Base.cores.get(source.substring(5))); } 
                    if (source.startsWith("compiler:")) { ctx.setCompiler(Base.compilers.get(source.substring(9))); } 
                    keyToExecute = "prefs." + keyToExecute;
                }

                ctx.debugDump();
                
                Object hash = ctx.executeKey(keyToExecute);
                if (hash instanceof HashMap) {
                    options = (HashMap<String, String>)hash;
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
        } else {
            b.add(new JLabel(name + ": "));
            b.add(new JLabel(value));
        }

        b.setBorder(new EmptyBorder(2, 2, 2, 2));

        return b;
    }

    class ScrollablePanel extends JPanel implements Scrollable {
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
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

    public static Font getFont(String key) {
        String data = get(key);
        if (data == null || data.equals("")) {
            return Base.preferences.stringToFont("Monospaced,plain,12");
        }
        return Base.preferences.stringToFont(data);
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
        
}



