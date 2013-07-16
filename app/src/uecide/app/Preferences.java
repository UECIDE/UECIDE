/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-09 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package uecide.app;

import uecide.plugin.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.Method;

import javax.swing.*;
import javax.swing.border.*;
import processing.core.*;

import say.swing.*;



/**
 * Storage class for user preferences and environment settings.
 * <P>
 * This class no longer uses the Properties class, since
 * properties files are iso8859-1, which is highly likely to
 * be a problem when trying to save sketch folders and locations.
 * <p>
 * The GUI portion in here is really ugly, as it uses exact layout. This was
 * done in frustration one evening (and pre-Swing), but that's long since past,
 * and it should all be moved to a proper swing layout like BoxLayout.
 * <p>
 * This is very poorly put together, that the preferences panel and the actual
 * preferences i/o is part of the same code. But there hasn't yet been a
 * compelling reason to bother with the separation aside from concern about
 * being lectured by strangers who feel that it doesn't look like what they
 * learned in CS class.
 * <p>
 * Would also be possible to change this to use the Java Preferences API.
 * Some useful articles
 * <a href="http://www.onjava.com/pub/a/onjava/synd/2001/10/17/j2se.html">here</a> and
 * <a href="http://www.particle.kth.se/~lindsey/JavaCourse/Book/Part1/Java/Chapter10/Preferences.html">here</a>.
 * However, haven't implemented this yet for lack of time, but more
 * importantly, because it would entail writing to the registry (on Windows),
 * or an obscure file location (on Mac OS X) and make it far more difficult to
 * find the preferences to tweak them by hand (no! stay out of regedit!)
 * or to reset the preferences by simply deleting the preferences.txt file.
 */
public class Preferences {

  // what to call the feller

  static final String PREFS_FILE = "preferences.txt";


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
  JCheckBox checkUpdatesBox;
  JTextField editorFontField;
  JTextField consoleFontField;
  JCheckBox autoAssociateBox;
  JCheckBox saveHex;
  JCheckBox disablePrototypes;

  JTabbedPane tabs;


  // the calling editor, so updates can be applied

  Editor editor;


  // data model

  static Hashtable defaults;
  static Hashtable table = new Hashtable();;
  static File preferencesFile;

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


  static protected void init(String commandLinePrefs) {

    // start by loading the defaults, in case something
    // important was deleted from the user prefs
    try {
      load(Base.getLibStream("preferences.txt"));
    } catch (Exception e) {
      Base.showError(null, "Could not read default settings.\n" +
                           "You'll need to reinstall " + Theme.get("product.cap") + ".", e);
    }

    // check for platform-specific properties in the defaults
    String platformExt = "." + PConstants.platformNames[PApplet.platform];
    int platformExtLength = platformExt.length();
    Enumeration e = table.keys();
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      if (key.endsWith(platformExt)) {
        // this is a key specific to a particular platform
        String actualKey = key.substring(0, key.length() - platformExtLength);
        String value = get(key);
        table.put(actualKey, value);
      }
    }

    // clone the hash table
    defaults = (Hashtable) table.clone();

    // other things that have to be set explicitly for the defaults
    setColor("run.window.bgcolor", SystemColor.control);

    // Load a prefs file if specified on the command line
    if (commandLinePrefs != null) {
      try {
        load(new FileInputStream(commandLinePrefs));

      } catch (Exception poe) {
        Base.showError("Error",
                       "Could not read preferences from " +
                       commandLinePrefs, poe);
      }
    } else if (!Base.isCommandLine()) {
      // next load user preferences file
      preferencesFile = Base.getSettingsFile(PREFS_FILE);
      if (!preferencesFile.exists()) {
        // create a new preferences file if none exists
        // saves the defaults out to the file
        save();

      } else {
        // load the previous preferences file

        try {
          load(new FileInputStream(preferencesFile));

        } catch (Exception ex) {
          Base.showError("Error reading preferences",
                         "Error reading the preferences file. " +
                         "Please delete (or move)\n" +
                         preferencesFile.getAbsolutePath() +
                         " and restart " + Theme.get("product.cap") + ".", ex);
        }
      }
    }    
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

    GridBagConstraints c = new GridBagConstraints();
    mainSettings.setBorder(new EmptyBorder(5, 5, 5, 5));

    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridwidth = 2;
    c.gridheight = 1;
    c.gridx = 0;
    c.gridy = 0;

    JLabel label;
    JButton button;
    label = new JLabel("Sketchbook location:");
    mainSettings.add(label);

    c.gridwidth = 1;
    c.gridy++;
    sketchbookLocationField = new JTextField(40);
    mainSettings.add(sketchbookLocationField, c);

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
    mainSettings.add(button, c);

    c.gridx = 0;
    c.gridy++;

    label = new JLabel("Editor font: ");
    mainSettings.add(label, c);

    c.gridy++;
    editorFontField = new JTextField(40);
    editorFontField.setEditable(false);
    mainSettings.add(editorFontField, c);

    editorFontField.setText(get("editor.font"));

    JButton selectEditorFont = new JButton(Translate.t("Select Font..."));
    c.gridx = 1;
    mainSettings.add(selectEditorFont, c);

    final Container parent = mainSettings;
    final Font xFont = getFont("editor.font");
    selectEditorFont.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            JFontChooser fc = new JFontChooser(false);
            fc.setSelectedFont(xFont);
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
    mainSettings.add(label, c);
    c.gridy++;

    consoleFontField = new JTextField(40);
    consoleFontField.setEditable(false);
    mainSettings.add(consoleFontField, c);

    consoleFontField.setText(get("console.font"));

    JButton selectConsoleFont = new JButton(Translate.t("Select Font..."));
    c.gridx = 1;
    mainSettings.add(selectConsoleFont, c);

    final Font yFont = getFont("console.font");
    selectConsoleFont.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            JFontChooser fc = new JFontChooser(false);
            fc.setSelectedFont(yFont);
            int res = fc.showDialog(parent);
            if (res == JFontChooser.OK_OPTION) {
                Font f = fc.getSelectedFont();
                consoleFontField.setText(Preferences.fontToString(f));
            }
        }
    });

    c.gridx = 0;
    c.gridy = 0;
    c.gridwidth = 2;

    deletePreviousBox =
      new JCheckBox(Translate.t("Remove old build folder before each build"));
    advancedSettings.add(deletePreviousBox, c);

    c.gridy++;

    saveHex =
      new JCheckBox(Translate.t("Save HEX file to sketch folder"));
    advancedSettings.add(saveHex, c);

    c.gridy++;

    disablePrototypes =
      new JCheckBox(Translate.t("Disable adding of function prototypes"));
    advancedSettings.add(disablePrototypes, c);

    c.gridy++;
    externalEditorBox = new JCheckBox(Translate.t("Use external editor"));
    advancedSettings.add(externalEditorBox, c);


    c.gridy++;
    checkUpdatesBox = new JCheckBox(Translate.t("Check for updates on startup"));
    advancedSettings.add(checkUpdatesBox, c);

    if (Base.isWindows()) {
      c.gridy++;
      autoAssociateBox =
        new JCheckBox("Automatically associate .pde files with " + Theme.get("product.cap"));
      advancedSettings.add(autoAssociateBox, c);
    }

    tabs.add(Translate.t("Main Settings"), mainSettings);
    tabs.add(Translate.t("Advanced Settings"), advancedSettings);

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
    setBoolean("export.delete_target_folder",
               deletePreviousBox.isSelected());

    setBoolean("export.save_hex", saveHex.isSelected());
    setBoolean("compiler.disable_prototypes", disablePrototypes.isSelected());

//    setBoolean("sketchbook.closing_last_window_quits",
//               closingLastQuitsBox.isSelected());
    //setBoolean("sketchbook.prompt", sketchPromptBox.isSelected());
    //setBoolean("sketchbook.auto_clean", sketchCleanBox.isSelected());

    setBoolean("editor.external", externalEditorBox.isSelected());
    setBoolean("update.check", checkUpdatesBox.isSelected());

    /*
      // was gonna use this to check memory settings,
      // but it quickly gets much too messy
    if (getBoolean("run.options.memory")) {
      Process process = Runtime.getRuntime().exec(new String[] {
          "java", "-Xms" + memoryMin + "m", "-Xmx" + memoryMax + "m"
        });
      processInput = new SystemOutSiphon(process.getInputStream());
      processError = new MessageSiphon(process.getErrorStream(), this);
    }
    */

    set("editor.font", editorFontField.getText());
    set("console.font", consoleFontField.getText());

    if (autoAssociateBox != null) {
      setBoolean("platform.auto_file_type_associations",
                 autoAssociateBox.isSelected());
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
  }


  protected void showFrame(Editor editor) {
    this.editor = editor;

    // set all settings entry boxes to their actual status
    deletePreviousBox.
      setSelected(getBoolean("export.delete_target_folder"));
    saveHex.setSelected(getBoolean("export.save_hex"));
    disablePrototypes.setSelected(getBoolean("compiler.disable_prototypes"));

    //closingLastQuitsBox.
    //  setSelected(getBoolean("sketchbook.closing_last_window_quits"));
    //sketchPromptBox.
    //  setSelected(getBoolean("sketchbook.prompt"));
    //sketchCleanBox.
    //  setSelected(getBoolean("sketchbook.auto_clean"));

    sketchbookLocationField.
      setText(get("sketchbook.path"));
    externalEditorBox.
      setSelected(getBoolean("editor.external"));
    checkUpdatesBox.
      setSelected(getBoolean("update.check"));

    if (autoAssociateBox != null) {
      autoAssociateBox.
        setSelected(getBoolean("platform.auto_file_type_associations"));
    }

    dialog.setVisible(true);
  }


  // .................................................................


  static protected void load(InputStream input) throws IOException {
    load(input, table);
  }
  
  static public void load(InputStream input, Map table) throws IOException {  
    String[] lines = PApplet.loadStrings(input);  // Reads as UTF-8
    for (String line : lines) {
      if ((line.length() == 0) ||
          (line.charAt(0) == '#')) continue;

      // this won't properly handle = signs being in the text
      int equals = line.indexOf('=');
      if (equals != -1) {
        String key = line.substring(0, equals).trim();
        String value = line.substring(equals + 1).trim();
        table.put(key, value);
      }
    }
  }


  // .................................................................


  static protected void save() {
//    try {
    // on startup, don't worry about it
    // this is trying to update the prefs for who is open
    // before Preferences.init() has been called.
    if (preferencesFile == null) return;

    // Fix for 0163 to properly use Unicode when writing preferences.txt
    PrintWriter writer = PApplet.createWriter(preferencesFile);

    String[] keys = (String[]) table.keySet().toArray(new String[0]);
    Arrays.sort(keys);

    for (String key : keys) {
        writer.println(key + "=" + ((String) table.get(key)));
    }
/*

    Enumeration e = table.keys(); //properties.propertyNames();
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      writer.println(key + "=" + ((String) table.get(key)));
    }
*/
    writer.flush();
    writer.close();

//    } catch (Exception ex) {
//      Base.showWarning(null, "Error while saving the settings file", ex);
//    }
  }


  // .................................................................


  // all the information from preferences.txt

  //static public String get(String attribute) {
  //return get(attribute, null);
  //}
  
  static public String get(String attribute /*, String defaultValue */) {
    return (String) table.get(attribute);
    /*
    //String value = (properties != null) ?
    //properties.getProperty(attribute) : applet.getParameter(attribute);
    String value = properties.getProperty(attribute);

    return (value == null) ?
      defaultValue : value;
    */
  }


  static public String getDefault(String attribute) {
    return (String) defaults.get(attribute);
  }


  static public void set(String attribute, String value) {
    table.put(attribute, value);
  }


  static public void unset(String attribute) {
    table.remove(attribute);
  }


  static public boolean getBoolean(String attribute) {
    String value = get(attribute); //, null);
    return (new Boolean(value)).booleanValue();

    /*
      supposedly not needed, because anything besides 'true'
      (ignoring case) will just be false.. so if malformed -> false
    if (value == null) return defaultValue;

    try {
      return (new Boolean(value)).booleanValue();
    } catch (NumberFormatException e) {
      System.err.println("expecting an integer: " + attribute + " = " + value);
    }
    return defaultValue;
    */
  }


  static public void setBoolean(String attribute, boolean value) {
    set(attribute, value ? "true" : "false");
  }


  static public int getInteger(String attribute /*, int defaultValue*/) {
    return Integer.parseInt(get(attribute));

    /*
    String value = get(attribute, null);
    if (value == null) return defaultValue;

    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      // ignored will just fall through to returning the default
      System.err.println("expecting an integer: " + attribute + " = " + value);
    }
    return defaultValue;
    //if (value == null) return defaultValue;
    //return (value == null) ? defaultValue :
    //Integer.parseInt(value);
    */
  }


  static public void setInteger(String key, int value) {
    set(key, String.valueOf(value));
  }


  static public Color getColor(String name) {
    Color parsed = Color.GRAY;  // set a default
    String s = get(name);
    if ((s != null) && (s.indexOf("#") == 0)) {
      try {
        parsed = new Color(Integer.parseInt(s.substring(1), 16));
      } catch (Exception e) { }
    }
    return parsed;
  }


  static public void setColor(String attr, Color what) {
    set(attr, "#" + PApplet.hex(what.getRGB() & 0xffffff, 6));
  }


  static public Font getFont(String attr) {
    boolean replace = false;
    String value = get(attr);
    if (value == null) {
      value = getDefault(attr);
      replace = true;
    }
    if (value == null) {
        value="Monospaced,plain,12";
    }

    String[] pieces = PApplet.split(value, ',');
    if (pieces.length != 3) {
      value = getDefault(attr);
      pieces = PApplet.split(value, ',');
      replace = true;
    }

    String name = pieces[0];
    int style = Font.PLAIN;  // equals zero
    if (pieces[1].indexOf("bold") != -1) {
      style |= Font.BOLD;
    }
    if (pieces[1].indexOf("italic") != -1) {
      style |= Font.ITALIC;
    }
    int size = PApplet.parseInt(pieces[2], 12);
    Font font = new Font(name, style, size);

    // replace bad font with the default
    if (replace) {
      set(attr, value);
    }

    if (font == null) {
        font = new Font("Monospaced", Font.PLAIN, 12);
    }
    return font;
  }

  
  //get a Map of the Preferences
  static public Map<String, String> getMap() 
  {
  	Map globalpreferences = new LinkedHashMap();
    Enumeration e = table.keys();

    while (e.hasMoreElements()) 
    {
		String key = (String) e.nextElement();
		String value = (String) table.get(key);
        globalpreferences.put(key, value );              
    }

	return globalpreferences;	
  }
  
}
