package uecide.app;

import uecide.plugin.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.Method;

import javax.swing.*;
import javax.swing.border.*;

import say.swing.*;

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
  JCheckBox externalEditorBox;
  JCheckBox memoryOverrideBox;
  JTextField memoryField;
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

  JTabbedPane tabs;


  // the calling editor, so updates can be applied

  Editor editor;


  // data model

    static PropertyFile properties;

    static public String fontToString(Font f)
    {
        String font = f.getName();
        String style = "";
        font += ",";
        if ((f.getStyle() & Font.BOLD) != 0) {
            style += "bold";
        }
        if ((f.getStyle() & Font.ITALIC) != 0) {
            style += "italic";
        }
        if (style.equals("")) {
            style = "plain";
        }
        font += style;
        font += ",";
        font += Integer.toString(f.getSize());
        return font;
    }

    static public Font stringToFont(String value) {
        if (value == null) {
            value="Monospaced,plain,12";
        }

        String[] pieces = value.split(",");
        if (pieces.length != 3) {
            return new Font("Monospaced", Font.PLAIN, 12);
        }

        String name = pieces[0];
        int style = Font.PLAIN;  // equals zero
        if (pieces[1].indexOf("bold") != -1) {
            style |= Font.BOLD;
        }
        if (pieces[1].indexOf("italic") != -1) {
            style |= Font.ITALIC;
        }
        int size;
        try {
            size = Integer.parseInt(pieces[2]);
            if (size <= 0) size = 12;
        } catch (Exception e) {
            size = 12;
        }
 
        Font font = new Font(name, style, size);

        if (font == null) {
            font = new Font("Monospaced", Font.PLAIN, 12);
        }
        return font;
    }



    static protected void init() {
        properties = new PropertyFile(Base.getSettingsFile("preferences.txt"), Base.getContentFile("lib/preferences.txt"));
    }


  public Preferences() {

    // setup dialog for the prefs

    //dialog = new JDialog(editor, "Preferences", true);
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

    mainSettings.setBorder(new EmptyBorder(5, 5, 5, 5));
    advancedSettings.setBorder(new EmptyBorder(5, 5, 5, 5));



    tabs.add(Translate.t("Editor"), mainSettings);
    tabs.add(Translate.t("Compiler"), advancedSettings);

    populateEditorSettings(mainSettings);
    populateCompilerSettings(advancedSettings);

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

    pane.addKeyListener(new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
          KeyStroke wc = Editor.WINDOW_CLOSE_KEYSTROKE;
          if ((e.getKeyCode() == KeyEvent.VK_ESCAPE) ||
              (KeyStroke.getKeyStrokeForEvent(e).equals(wc))) {
            disposeFrame();
          }
        }
      });

        String[] entries = Base.plugins.keySet().toArray(new String[0]);
        for (String entry : entries) {
            Plugin p = Base.plugins.get(entry);
            Method m = null;
            try {
                Class[] cArg = new Class[1];
                cArg[0] = new JPanel().getClass();
                m = p.getClass().getMethod("populatePreferences", cArg);
                if (m != null) {
                    JPanel pluginSettings = new JPanel(new GridBagLayout());
                    pluginSettings.setBorder(new EmptyBorder(5, 5, 5, 5));
                    m.invoke(p, pluginSettings);
                    tabs.add(p.getMenuTitle(), pluginSettings);
                }
            } catch (Exception e) {
            }
        }
        dialog.pack();

        Dimension size = dialog.getSize();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        dialog.setLocation((screen.width - size.width) / 2,
                          (screen.height - size.height) / 2);
            
    }

    public void populateEditorSettings(JPanel p) {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 0;

        JLabel label;
        JButton button;
        label = new JLabel("Sketchbook location:");
        p.add(label, c);
        c.gridwidth = 1;
        c.gridy++;
        sketchbookLocationField = new JTextField(40);
        p.add(sketchbookLocationField, c);

        sketchbookLocationField.setEditable(false);
        button = new JButton(PROMPT_BROWSE);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                File dflt = new File(sketchbookLocationField.getText());
                File file = Base.selectFolder("Select new sketchbook location", dflt, dialog);
                if (file != null) {
                    sketchbookLocationField.setText(file.getAbsolutePath());
                }
            }
        });
        c.gridx = 1;
        p.add(button, c);

        c.gridx = 0;
        c.gridy++;

        label = new JLabel("Editor font: ");
        p.add(label, c);

        c.gridy++;
        editorFontField = new JTextField(40);
        editorFontField.setEditable(false);
        p.add(editorFontField, c);

        editorFontField.setText(Base.preferences.get("editor.font"));

        JButton selectEditorFont = new JButton(Translate.t("Select Font..."));
        c.gridx = 1;
        p.add(selectEditorFont, c);

        final Container parent = p;
        selectEditorFont.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFontChooser fc = new JFontChooser(false);
                fc.setSelectedFont(stringToFont(editorFontField.getText()));
                int res = fc.showDialog(parent);
                if (res == JFontChooser.OK_OPTION) {
                    Font f = fc.getSelectedFont();
                    editorFontField.setText(Preferences.fontToString(f));
                }
            }
        });

        c.gridx = 0;
        c.gridy++;

        label = new JLabel("Console font: ");
        p.add(label, c);
        c.gridy++;

        consoleFontField = new JTextField(40);
        consoleFontField.setEditable(false);
        p.add(consoleFontField, c);

        consoleFontField.setText(Base.preferences.get("console.font"));

        JButton selectConsoleFont = new JButton(Translate.t("Select Font..."));
        c.gridx = 1;
        p.add(selectConsoleFont, c);

        selectConsoleFont.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFontChooser fc = new JFontChooser(false);
                fc.setSelectedFont(stringToFont(consoleFontField.getText()));
                int res = fc.showDialog(parent);
                if (res == JFontChooser.OK_OPTION) {
                    Font f = fc.getSelectedFont();
                    consoleFontField.setText(Preferences.fontToString(f));
                }
            }
        });
        c.gridy++;
        externalEditorBox = new JCheckBox(Translate.t("Use external editor"));
        p.add(externalEditorBox, c);

        if (Base.isWindows()) {
          c.gridy++;
          autoAssociateBox =
            new JCheckBox("Automatically associate .pde files with " + Base.theme.get("product.cap"));
          p.add(autoAssociateBox, c);
        }
    }

    public void populateCompilerSettings(JPanel p) {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 0;

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

    Base.preferences.setBoolean("export.delete_target_folder", deletePreviousBox.isSelected());
    Base.preferences.setBoolean("compiler.generate_lss", createLss.isSelected());
    Base.preferences.setBoolean("export.save_lss", saveLss.isSelected());
    Base.preferences.setBoolean("export.save_hex", saveHex.isSelected());
    Base.preferences.setBoolean("compiler.disable_prototypes", disablePrototypes.isSelected());
    Base.preferences.setBoolean("compiler.combine_ino", combineIno.isSelected());
    Base.preferences.setBoolean("compiler.verbose", verboseCompile.isSelected());
    Base.preferences.setBoolean("export.verbose", verboseUpload.isSelected());

    File f = new File(sketchbookLocationField.getText());
    if (!f.exists()) {
        f.mkdirs();
    }

    Base.preferences.setFile("sketchbook.path", f);

//    setBoolean("sketchbook.closing_last_window_quits",
//               closingLastQuitsBox.isSelected());
    //setBoolean("sketchbook.prompt", sketchPromptBox.isSelected());
    //setBoolean("sketchbook.auto_clean", sketchCleanBox.isSelected());

    Base.preferences.setBoolean("editor.external", externalEditorBox.isSelected());

    Base.preferences.set("editor.font", editorFontField.getText());
    Base.preferences.set("console.font", consoleFontField.getText());

    if (autoAssociateBox != null) {
      Base.preferences.setBoolean("platform.auto_file_type_associations", autoAssociateBox.isSelected());
    }

        String[] entries = Base.plugins.keySet().toArray(new String[0]);
        for (String entry : entries) {
            Plugin p = Base.plugins.get(entry);
            Method m = null;
            try {
                m = p.getClass().getMethod("savePreferences");
                if (m != null) {
                    m.invoke(p);
                }
            } catch (Exception e) {
            }
        }
    Base.applyPreferences();
    Base.preferences.save();
  }


  protected void showFrame(Editor editor) {
    this.editor = editor;

    // set all settings entry boxes to their actual status
    deletePreviousBox.  setSelected(Base.preferences.getBoolean("export.delete_target_folder"));
    saveHex.setSelected(Base.preferences.getBoolean("export.save_hex"));
    createLss.setSelected(Base.preferences.getBoolean("compiler.generate_lss"));
    saveLss.setEnabled(Base.preferences.getBoolean("compiler.generate_lss"));
    saveLss.setSelected(Base.preferences.getBoolean("export.save_lss"));
    disablePrototypes.setSelected(Base.preferences.getBoolean("compiler.disable_prototypes"));
    combineIno.setSelected(Base.preferences.getBoolean("compiler.combine_ino"));
    verboseCompile.setSelected(Base.preferences.getBoolean("compiler.verbose"));
    verboseUpload.setSelected(Base.preferences.getBoolean("export.verbose"));

    sketchbookLocationField.  setText(Base.preferences.get("sketchbook.path"));
    externalEditorBox.  setSelected(Base.preferences.getBoolean("editor.external"));

    if (autoAssociateBox != null) {
      autoAssociateBox.  setSelected(Base.preferences.getBoolean("platform.auto_file_type_associations"));
    }

    dialog.setVisible(true);
  }
}
