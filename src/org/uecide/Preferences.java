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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import say.swing.*;

import de.muntjak.tinylookandfeel.*;

public class Preferences {

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
    JCheckBox exportSeparateBox;
    JCheckBox deletePreviousBox;
    JCheckBox compileInSketchFolder;
    JCheckBox disableLineNumbers;
    JCheckBox memoryOverrideBox;
    JTextField memoryField;
    JTextField externalEditorField;
    JTextField editorFontField;
    JTextField consoleFontField;
    JCheckBox autoAssociateBox;
    JCheckBox saveHex;
    JCheckBox saveLss;
    JCheckBox createLss;
    JCheckBox disablePrototypes;
    JCheckBox combineIno;
    JCheckBox verboseCompile;
    JCheckBox verboseUpload;
    JCheckBox useSpacesForTabs;
    JCheckBox visibleTabs;
    JCheckBox checkNewVersion;
    JCheckBox autoSave;
    JCheckBox crashReport;
    JTextField tabSize;
    JCheckBox hideSecondaryToolbar;

    JCheckBox keepFindOpen;

    JTabbedPane tabs;

    JTextField pluginsLocationField;
    JTextField cacheLocationField;
    JTextField boardsLocationField;
    JTextField coresLocationField;
    JTextField compilersLocationField;
    JCheckBox  createBackups;
    JTextField backupNumber;

    Editor editor;

    JComboBox selectedTheme;
    JComboBox selectedEditorTheme;
    JComboBox selectedIconTheme;
    JCheckBox useSystemDecorator;

    JList extraPortList;
    DefaultListModel extraPortListModel = new DefaultListModel();
    JTextField portInput;

    public static TreeMap<String, String>themes;

    JTable libraryLocationTable;
    class LibraryDetail {
        public String key;
        public String description;
        public String path;
    };

    class KVPair {
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
        properties = new PropertyFile(Base.getSettingsFile("preferences.txt"), Base.getContentFile("lib/preferences.txt"));
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

        JPanel mainSettings = new JPanel(new GridBagLayout());
        JPanel advancedSettings = new JPanel(new GridBagLayout());
        JPanel locationSettings = new JPanel(new GridBagLayout());
        JPanel librarySettings = new JPanel(new GridBagLayout());

        mainSettings.setBorder(new EmptyBorder(5, 5, 5, 5));
        advancedSettings.setBorder(new EmptyBorder(5, 5, 5, 5));
        locationSettings.setBorder(new EmptyBorder(5, 5, 5, 5));
        librarySettings.setBorder(new EmptyBorder(5, 5, 5, 5));

        tabs.add(Translate.t("Editor"), mainSettings);
        tabs.add(Translate.t("Compiler"), advancedSettings);
        tabs.add(Translate.t("Locations"), locationSettings);
        tabs.add(Translate.t("Libraries"), librarySettings);

        populateEditorSettings(mainSettings);
        populateCompilerSettings(advancedSettings);
        populateLocationSettings(locationSettings);
        populateLibrarySettings(librarySettings);

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

//    pane.addKeyListener(new KeyAdapter() {
//        public void keyPressed(KeyEvent e) {
//          KeyStroke wc = Editor.WINDOW_CLOSE_KEYSTROKE;
//          if ((e.getKeyCode() == KeyEvent.VK_ESCAPE) ||
//              (KeyStroke.getKeyStrokeForEvent(e).equals(wc))) {
//            disposeFrame();
//          }
//        }
//      });

        for(Class<?> pluginClass : Base.plugins.values()) {
            try {
                Method populatePreferences = pluginClass.getMethod("populatePreferences", JPanel.class);
                Method getPreferencesTitle = pluginClass.getMethod("getPreferencesTitle");

                if(getPreferencesTitle == null || populatePreferences == null) {
                    continue;
                }

                String title = (String)(getPreferencesTitle.invoke(null));

                if(title != null) {
                    JPanel pluginSettings = new JPanel(new GridBagLayout());
                    pluginSettings.setBorder(new EmptyBorder(5, 5, 5, 5));
                    populatePreferences.invoke(null, pluginSettings);
                    tabs.add(title, pluginSettings);
                }
            } catch(Exception e) {
                e.printStackTrace();
                //Base.error(e);
            }
        }

        dialog.pack();

        Dimension size = dialog.getSize();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        if (editor != null) dialog.setLocationRelativeTo(editor);
//        dialog.setLocation((screen.width - size.width) / 2,
//                           (screen.height - size.height) / 2);

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
                            dialog.pack();
                            if (editor != null) dialog.setLocationRelativeTo(editor);
                            
                        } catch(Exception ignored) {
                        }
                    }
                });
            }
        });

        String currentLaf = Base.preferences.get("editor.laf");

        for(String k : keys) {
            if(themes.get(k).equals(currentLaf)) {
                selectedTheme.setSelectedItem(k);
            }
        }

        p.add(selectedTheme, c);

        c.gridx = 2;

        useSystemDecorator = new JCheckBox(Translate.t("Use System Decorator"));
        p.add(useSystemDecorator, c);
        useSystemDecorator.setSelected(Base.preferences.getBoolean("editor.laf.decorator"));

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

                                System.err.println(props.toString());

                                Class<?> cls = Class.forName(laf);
                                Class[] cArg = new Class[1];
                                cArg[0] = Properties.class;
                                Method mth = cls.getMethod("setCurrentTheme", cArg);
                                mth.invoke(cls, props);
                            }

                            UIManager.setLookAndFeel(laf);
                            SwingUtilities.updateComponentTreeUI(dialog);
                            Base.updateLookAndFeel();
                            dialog.pack();
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

            if(t.equals(Base.preferences.get("theme.selected", "default"))) {
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

        editorFontField.setText(Base.preferences.get("editor.font"));

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

        consoleFontField.setText(Base.preferences.get("console.font"));

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

        c.gridx = 0;
        c.gridy++;
        useSpacesForTabs = new JCheckBox(Translate.t("Editor uses spaces for tabs"));
        p.add(useSpacesForTabs, c);

        c.gridx = 0;
        c.gridy++;
        visibleTabs = new JCheckBox(Translate.t("Show tabs and indents"));
        p.add(visibleTabs, c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        label = new JLabel("Number of spaces to use for a tab:");
        p.add(label, c);
        c.gridx++;
        tabSize = new JTextField(5);
        p.add(tabSize, c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        keepFindOpen = new JCheckBox(Translate.t("Keep Find & Replace permanantly open"));
        p.add(keepFindOpen, c);
        keepFindOpen.setSelected(Base.preferences.getBoolean("editor.keepfindopen"));

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        createBackups = new JCheckBox(Translate.t("Create backup copies of your sketch as you save"));
        p.add(createBackups, c);
        createBackups.setSelected(Base.preferences.getBoolean("version.enabled"));

        c.gridx = 0;
        c.gridwidth = 1;
        c.gridy++;
        label = new JLabel(Translate.t("Number of backup copies to keep:"));
        p.add(label, c);
        c.gridx++;
        backupNumber = new JTextField(5);
        backupNumber.setText(Integer.toString(Base.preferences.getInteger("version.keep")));
        p.add(backupNumber, c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        hideSecondaryToolbar = new JCheckBox(Translate.t("Hide the secondary editor toolbar"));
        p.add(hideSecondaryToolbar, c);
        hideSecondaryToolbar.setSelected(Base.preferences.getBoolean("editor.subtoolbar.hidden"));

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        checkNewVersion = new JCheckBox(Translate.t("Check for new version on startup"));
        p.add(checkNewVersion, c);
        checkNewVersion.setSelected(Base.preferences.getBoolean("version.check"));

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        autoSave = new JCheckBox(Translate.t("Automatically save sketch before compile"));
        p.add(autoSave, c);
        autoSave.setSelected(Base.preferences.getBoolean("editor.autosave"));

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        crashReport = new JCheckBox(Translate.t("Disable crash reporter"));
        p.add(crashReport, c);
        crashReport.setSelected(Base.preferences.getBoolean("crash.noreport"));
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
        lab = new JLabel(Translate.t("Plugins Location"));
        p.add(lab, c);
        c.gridy++;
        c.gridwidth = 1;
        pluginsLocationField = new JTextField(40);
        pluginsLocationField.setEditable(false);
        p.add(pluginsLocationField, c);
        c.gridx = 1;
        but = new JButton(Translate.t("Select Folder..."));
        but.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                File dflt = new File(pluginsLocationField.getText());
                File file = Base.selectFolder("Select new plugins location", dflt, dialog);

                if(file != null) {
                    pluginsLocationField.setText(file.getAbsolutePath());
                }
            }
        });
        p.add(but, c);
        c.gridy++;

        c.gridx = 0;
        c.gridwidth = 2;
        lab = new JLabel(Translate.t("Cache Location"));
        p.add(lab, c);
        c.gridy++;
        c.gridwidth = 1;
        cacheLocationField = new JTextField(40);
        cacheLocationField.setEditable(false);
        p.add(cacheLocationField, c);
        c.gridx = 1;
        but = new JButton(Translate.t("Select Folder..."));
        but.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                File dflt = new File(cacheLocationField.getText());
                File file = Base.selectFolder("Select new cache location", dflt, dialog);

                if(file != null) {
                    cacheLocationField.setText(file.getAbsolutePath());
                }
            }
        });
        p.add(but, c);
        c.gridy++;

        c.gridx = 0;
        c.gridwidth = 2;
        lab = new JLabel(Translate.t("Boards Location"));
        p.add(lab, c);
        c.gridy++;
        c.gridwidth = 1;
        boardsLocationField = new JTextField(40);
        boardsLocationField.setEditable(false);
        p.add(boardsLocationField, c);
        c.gridx = 1;
        but = new JButton(Translate.t("Select Folder..."));
        but.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                File dflt = new File(boardsLocationField.getText());
                File file = Base.selectFolder("Select new boards location", dflt, dialog);

                if(file != null) {
                    boardsLocationField.setText(file.getAbsolutePath());
                }
            }
        });
        p.add(but, c);
        c.gridy++;

        c.gridx = 0;
        c.gridwidth = 2;
        lab = new JLabel(Translate.t("Cores Location"));
        p.add(lab, c);
        c.gridy++;
        c.gridwidth = 1;
        coresLocationField = new JTextField(40);
        coresLocationField.setEditable(false);
        p.add(coresLocationField, c);
        c.gridx = 1;
        but = new JButton(Translate.t("Select Folder..."));
        but.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                File dflt = new File(coresLocationField.getText());
                File file = Base.selectFolder("Select new cores location", dflt, dialog);

                if(file != null) {
                    coresLocationField.setText(file.getAbsolutePath());
                }
            }
        });
        p.add(but, c);
        c.gridy++;

        c.gridx = 0;
        c.gridwidth = 2;
        lab = new JLabel(Translate.t("Compilers Location"));
        p.add(lab, c);
        c.gridy++;
        c.gridwidth = 1;
        compilersLocationField = new JTextField(40);
        compilersLocationField.setEditable(false);
        p.add(compilersLocationField, c);
        c.gridx = 1;
        but = new JButton(Translate.t("Select Folder..."));
        but.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                File dflt = new File(compilersLocationField.getText());
                File file = Base.selectFolder("Select new compilers location", dflt, dialog);

                if(file != null) {
                    compilersLocationField.setText(file.getAbsolutePath());
                }
            }
        });
        p.add(but, c);
        c.gridy++;

        c.gridx = 0;
        c.gridwidth = 1;
        lab = new JLabel(Translate.t("Changing these settings will require a restart of the IDE."));
        p.add(lab, c);
        c.gridx = 1;
        c.gridheight = 2;

        but = new JButton(Translate.t("Select All..."));
        but.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                File dflt = new File(compilersLocationField.getText());
                File file = Base.selectFolder("Select new location for all items", dflt, dialog);

                if(file != null) {
                    pluginsLocationField.setText(new File(file, "plugins").getAbsolutePath());
                    cacheLocationField.setText(new File(file, "cache").getAbsolutePath());
                    boardsLocationField.setText(new File(file, "boards").getAbsolutePath());
                    coresLocationField.setText(new File(file, "cores").getAbsolutePath());
                    compilersLocationField.setText(new File(file, "compilers").getAbsolutePath());
                }
            }
        });
        p.add(but, c);

        c.gridheight = 1;
        c.gridy++;

        c.gridx = 0;
        c.gridwidth = 1;
        lab = new JLabel(Translate.t("You will also have to reinstall any plugins, boards, cores and compilers."));
        p.add(lab, c);
        c.gridy++;

    }

    public void populateCompilerSettings(JPanel p) {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 0;

        compileInSketchFolder =
            new JCheckBox(Translate.t("Compile the sketch in the sketch folder"));
        compileInSketchFolder.setSelected(Base.preferences.getBoolean("compiler.buildinsketch"));
        p.add(compileInSketchFolder, c);

        c.gridy++;

        disableLineNumbers =
            new JCheckBox(Translate.t("Disable insertion of #line numbering (useful for debugging)"));
        disableLineNumbers.setSelected(Base.preferences.getBoolean("compiler.disableline"));
        p.add(disableLineNumbers, c);

        c.gridy++;

        deletePreviousBox =
            new JCheckBox(Translate.t("Remove old build folder before each build"));
        p.add(deletePreviousBox, c);

        c.gridy++;

        saveHex =
            new JCheckBox(Translate.t("Save HEX file to sketch folder"));
        p.add(saveHex, c);

        c.gridy++;
        createLss =
            new JCheckBox(Translate.t("Generate assembly listing (requires core support)"));
        createLss.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveLss.setEnabled(createLss.isSelected());
            }
        });
        p.add(createLss, c);

        c.gridy++;
        c.gridwidth = 1;
        c.weightx = 0.1;
        p.add(Box.createHorizontalGlue(), c);
        c.gridx = 1;
        c.weightx = 0.9;
        saveLss =
            new JCheckBox(Translate.t("Save assembly listing to sketch folder"));
        p.add(saveLss, c);

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 2;

        disablePrototypes =
            new JCheckBox(Translate.t("Disable adding of function prototypes"));
        p.add(disablePrototypes, c);

        c.gridy++;
        combineIno =
            new JCheckBox(Translate.t("Combine all INO/PDE files into one CPP file"));
        p.add(combineIno, c);

        c.gridx = 0;
        c.gridwidth = 2;
        c.gridy++;
        verboseCompile =
            new JCheckBox(Translate.t("Verbose output during compile"));
        p.add(verboseCompile, c);

        c.gridx = 0;
        c.gridwidth = 2;
        c.gridy++;
        verboseUpload =
            new JCheckBox(Translate.t("Verbose output during upload"));
        p.add(verboseUpload, c);


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
        // put each of the settings into the table

        Base.preferences.setBoolean("compiler.buildinsketch", compileInSketchFolder.isSelected());
        Base.preferences.setBoolean("compiler.disableline", disableLineNumbers.isSelected());
        Base.preferences.setBoolean("export.delete_target_folder", deletePreviousBox.isSelected());
        Base.preferences.setBoolean("compiler.generate_lss", createLss.isSelected());
        Base.preferences.setBoolean("export.save_lss", saveLss.isSelected());
        Base.preferences.setBoolean("export.save_hex", saveHex.isSelected());
        Base.preferences.setBoolean("compiler.disable_prototypes", disablePrototypes.isSelected());
        Base.preferences.setBoolean("compiler.combine_ino", combineIno.isSelected());
        Base.preferences.setBoolean("compiler.verbose", verboseCompile.isSelected());
        Base.preferences.setBoolean("export.verbose", verboseUpload.isSelected());
        Base.preferences.setBoolean("version.enabled", createBackups.isSelected());
        Base.preferences.setBoolean("editor.keepfindopen", keepFindOpen.isSelected());
        Base.preferences.set("version.keep", backupNumber.getText());
        Base.preferences.setBoolean("editor.subtoolbar.hidden", hideSecondaryToolbar.isSelected());
        Base.preferences.setBoolean("version.check", checkNewVersion.isSelected());
        Base.preferences.setBoolean("editor.autosave", autoSave.isSelected());
        Base.preferences.setBoolean("crash.noreport", crashReport.isSelected());

        Base.preferences.setBoolean("editor.expandtabs", useSpacesForTabs.isSelected());
        Base.preferences.setBoolean("editor.showtabs", visibleTabs.isSelected());
        Base.preferences.set("editor.tabsize", tabSize.getText());
        Base.preferences.set("theme.selected", ((KVPair)selectedEditorTheme.getSelectedItem()).getKey());

        Base.preferences.set("editor.icons", (String)selectedIconTheme.getSelectedItem());
        Base.iconSet = (String)selectedIconTheme.getSelectedItem();

        File f = new File(sketchbookLocationField.getText());

        if(!f.exists()) {
            f.mkdirs();
        }

        Base.preferences.setFile("sketchbook.path", f);

//    setBoolean("sketchbook.closing_last_window_quits",
//               closingLastQuitsBox.isSelected());
        //setBoolean("sketchbook.prompt", sketchPromptBox.isSelected());
        //setBoolean("sketchbook.auto_clean", sketchCleanBox.isSelected());

        Base.preferences.set("editor.font", editorFontField.getText());
        Base.preferences.set("editor.external.command", externalEditorField.getText());
        Base.preferences.set("console.font", consoleFontField.getText());

        Base.preferences.set("location.plugins", pluginsLocationField.getText());
        Base.preferences.set("location.cache", cacheLocationField.getText());
        Base.preferences.set("location.boards", boardsLocationField.getText());
        Base.preferences.set("location.cores", coresLocationField.getText());
        Base.preferences.set("location.compilers", compilersLocationField.getText());

        String value = (String)selectedTheme.getSelectedItem();
        String laf = themes.get(value);
        Base.preferences.set("editor.laf", laf);
        Base.preferences.setBoolean("editor.laf.decorator", useSystemDecorator.isSelected());

        if(autoAssociateBox != null) {
            Base.preferences.setBoolean("platform.auto_file_type_associations", autoAssociateBox.isSelected());
        }

        for(Class<?> pluginClass : Base.plugins.values()) {
            try {
                Method savePreferences = pluginClass.getMethod("savePreferences");

                if(savePreferences == null) {
                    continue;
                }

                savePreferences.invoke(null);
            } catch(Exception e) {
            }
        }

        if(Base.isPosix()) {
            int i = 0;
            String pref = Base.preferences.get("serial.ports." + Integer.toString(i));

            while(pref != null) {
                Base.preferences.unset("serial.ports." + Integer.toString(i));
                i++;
                pref = Base.preferences.get("serial.ports." + Integer.toString(i));
            }

            i = 0;

            for(Enumeration en = extraPortListModel.elements(); en.hasMoreElements();) {
                String s = (String)en.nextElement();
                Base.preferences.set("serial.ports." + Integer.toString(i), s);
                i++;
            }

            Serial.fillExtraPorts();
        }

        for(String k : Base.preferences.childKeysOf("library")) {
            Debug.message("Removing library entry " + k);
            Base.preferences.unset("library." + k + ".name");
            Base.preferences.unset("library." + k + ".path");
        }

        for(int i = 0; i < libraryLocationModel.getRowCount(); i++) {
            String k = (String)libraryLocationModel.getValueAt(i, 0);
            k = k.toLowerCase();
            k = k.replaceAll(" ", "_");
            Base.preferences.set("library." + k + ".name", (String)libraryLocationModel.getValueAt(i, 1));
            Base.preferences.set("library." + k + ".path", (String)libraryLocationModel.getValueAt(i, 2));
            Debug.message("Adding library entry " + k);
        }

        Base.applyPreferences();
        Base.preferences.save();
        Base.cleanAndScanAllSettings();
        Editor.refreshAllEditors();
    }


    protected void showFrame() {

        // set all settings entry boxes to their actual status
        deletePreviousBox.setSelected(Base.preferences.getBoolean("export.delete_target_folder"));
        saveHex.setSelected(Base.preferences.getBoolean("export.save_hex"));
        createLss.setSelected(Base.preferences.getBoolean("compiler.generate_lss"));
        saveLss.setEnabled(Base.preferences.getBoolean("compiler.generate_lss"));
        saveLss.setSelected(Base.preferences.getBoolean("export.save_lss"));
        disablePrototypes.setSelected(Base.preferences.getBoolean("compiler.disable_prototypes"));
        combineIno.setSelected(Base.preferences.getBoolean("compiler.combine_ino"));
        verboseCompile.setSelected(Base.preferences.getBoolean("compiler.verbose"));
        verboseUpload.setSelected(Base.preferences.getBoolean("export.verbose"));

        useSpacesForTabs.setSelected(Base.preferences.getBoolean("editor.expandtabs"));
        visibleTabs.setSelected(Base.preferences.getBoolean("editor.showtabs"));
        tabSize.setText(Base.preferences.get("editor.tabsize") == null ? "4" : Base.preferences.get("editor.tabsize"));

        sketchbookLocationField.setText(Base.preferences.get("sketchbook.path"));

        pluginsLocationField.setText(Base.getUserPluginsFolder().getAbsolutePath());
        cacheLocationField.setText(Base.getUserCacheFolder().getAbsolutePath());
        boardsLocationField.setText(Base.getUserBoardsFolder().getAbsolutePath());
        coresLocationField.setText(Base.getUserCoresFolder().getAbsolutePath());
        compilersLocationField.setText(Base.getUserCompilersFolder().getAbsolutePath());

        if(autoAssociateBox != null) {
            autoAssociateBox.  setSelected(Base.preferences.getBoolean("platform.auto_file_type_associations"));
        }

        if(Base.isPosix()) {
            ArrayList<String> pl = Serial.getExtraPorts();

            extraPortListModel.clear();

            for(String port : pl) {
                extraPortListModel.addElement(port);
            }
        }

        int i = 0;

        for(String k : Base.preferences.childKeysOf("library")) {
            libraryLocationModel.setValueAt(k, i, 0);
            libraryLocationModel.setValueAt(Base.preferences.get("library." + k + ".name"), i, 1);
            libraryLocationModel.setValueAt(Base.preferences.get("library." + k + ".path"), i, 2);
            i++;
        }

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
}

