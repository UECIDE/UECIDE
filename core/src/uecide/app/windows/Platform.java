package uecide.app.windows;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.*;

import com.sun.jna.Library;
import com.sun.jna.Native;

import uecide.app.Base;
import uecide.app.windows.Registry.REGISTRY_ROOT_KEY;

import javax.swing.UIManager;


// http://developer.apple.com/documentation/QuickTime/Conceptual/QT7Win_Update_Guide/Chapter03/chapter_3_section_1.html
// HKEY_LOCAL_MACHINE\SOFTWARE\Apple Computer, Inc.\QuickTime\QTSysDir

// HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Development Kit\CurrentVersion -> 1.6 (String)
// HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Development Kit\CurrentVersion\1.6\JavaHome -> c:\jdk-1.6.0_05

public class Platform extends uecide.app.Platform {

  static final String openCommand =
    System.getProperty("user.dir").replace('/', '\\') +
    "\\processing.exe \"%1\"";
  static final String DOC = "Processing.Document";

  public void setLookAndFeel() {
    try {
        // Use the Quaqua L & F on OS X to make JFileChooser less awful
        String laf = Base.theme.get("window.laf.windows");
        if ((laf != null) && (laf != "default")) {
           UIManager.setLookAndFeel(laf);
        } 
    } catch (Exception e) {
        e.printStackTrace();
    }
  }

  public void init(Base base) {
    super.init(base);

    checkAssociations();
    checkQuickTime();
    checkPath();
  }


  /**
   * Make sure that .pde files are associated with processing.exe.
   */
  protected void checkAssociations() {
    try {
      String knownCommand =
        Registry.getStringValue(REGISTRY_ROOT_KEY.CLASSES_ROOT,
                                DOC + "\\shell\\open\\command", "");
      if (knownCommand == null) {
        if (Base.preferences.getBoolean("platform.auto_file_type_associations")) {
          setAssociations();
        }

      } else if (!knownCommand.equals(openCommand)) {
        // If the value is set differently, just change the registry setting.
        if (Base.preferences.getBoolean("platform.auto_file_type_associations")) {
          setAssociations();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  /**
   * Associate .pde files with this version of Processing.
   */
  protected void setAssociations() {
    try {
        if (Registry.createKey(REGISTRY_ROOT_KEY.CLASSES_ROOT,
                               "", ".pde") &&
            Registry.setStringValue(REGISTRY_ROOT_KEY.CLASSES_ROOT,
                                    ".pde", "", DOC) &&

            Registry.createKey(REGISTRY_ROOT_KEY.CLASSES_ROOT, "", DOC) &&
            Registry.setStringValue(REGISTRY_ROOT_KEY.CLASSES_ROOT, DOC, "",
                                    "Processing Source Code") &&

            Registry.createKey(REGISTRY_ROOT_KEY.CLASSES_ROOT,
                               DOC, "shell") &&
            Registry.createKey(REGISTRY_ROOT_KEY.CLASSES_ROOT,
                               DOC + "\\shell", "open") &&
            Registry.createKey(REGISTRY_ROOT_KEY.CLASSES_ROOT,
                               DOC + "\\shell\\open", "command") &&
            Registry.setStringValue(REGISTRY_ROOT_KEY.CLASSES_ROOT,
                                    DOC + "\\shell\\open\\command", "",
                                    openCommand)) {
          // everything ok
          // hooray!

        } else {
          Base.preferences.setBoolean("platform.auto_file_type_associations", false);
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
  }


  /**
   * Find QuickTime for Java installation.
   */
  protected void checkQuickTime() {
    try {
      String qtsystemPath =
        Registry.getStringValue(REGISTRY_ROOT_KEY.LOCAL_MACHINE,
                                "Software\\Apple Computer, Inc.\\QuickTime",
                                "QTSysDir");
      // Could show a warning message here if QT not installed, but that
      // would annoy people who don't want anything to do with QuickTime.
      if (qtsystemPath != null) {
        File qtjavaZip = new File(qtsystemPath, "QTJava.zip");
        if (qtjavaZip.exists()) {
          String qtjavaZipPath = qtjavaZip.getAbsolutePath();
          String cp = System.getProperty("java.class.path");
          System.setProperty("java.class.path",
                             cp + File.pathSeparator + qtjavaZipPath);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  
  /**
   * Remove extra quotes, slashes, and garbage from the Windows PATH.
   */
  protected void checkPath() {
    ArrayList<String> legit = new ArrayList<String>();
    String path = System.getProperty("java.library.path");
    String[] pieces = path.split(File.pathSeparator);
    for (String item : pieces) {
      if (item.startsWith("\"")) {
        item = item.substring(1);
      }
      if (item.endsWith("\"")) {
        item = item.substring(0, item.length() - 1);
      }
      if (item.endsWith(File.separator)) {
        item = item.substring(0, item.length() - File.separator.length());
      }
      File directory = new File(item);
      if (!directory.exists()) {
        continue;
      }
      if (item.trim().length() == 0) {
        continue;
      }
      legit.add(item);
    }
    StringBuilder newPath = new StringBuilder();
    for (String s : legit) {
        newPath.append(s);
        newPath.append(File.separator);
    }
    String calcPath = newPath.toString();
    if (calcPath.endsWith(File.separator)) {
        calcPath = calcPath.substring(0, calcPath.length() - 1 - File.separator.length());
    }
    if (!calcPath.equals(path)) {
      System.setProperty("java.library.path", calcPath);
    }
  }


  // looking for Documents and Settings/blah/Application Data/Processing
  public File getSettingsFolder() {

    try {
        String keyPath =
          "Software\\Microsoft\\Windows\\CurrentVersion" +
          "\\Explorer\\Shell Folders";
        String appDataPath =
          Registry.getStringValue(REGISTRY_ROOT_KEY.CURRENT_USER, keyPath, "AppData");

        File dataFolder = new File(appDataPath, Base.theme.get("product"));
        return dataFolder;
    } catch (Exception e) {
        e.printStackTrace();
        return null;
    }
  }


  // looking for Documents and Settings/blah/My Documents/Processing
  // (though using a reg key since it's different on other platforms)
  public File getDefaultSketchbookFolder() {
    try {
        String keyPath =
          "Software\\Microsoft\\Windows\\CurrentVersion" +
          "\\Explorer\\Shell Folders";
        String personalPath =
          Registry.getStringValue(REGISTRY_ROOT_KEY.CURRENT_USER, keyPath, "Personal");

        return new File(personalPath, Base.theme.get("product"));
    } catch (Exception e) {
        e.printStackTrace();
        return null;
    }
  }


  public void openURL(String url) {
    try {
        if (url.startsWith("http://")) {
          Runtime.getRuntime().exec("cmd /c start " + url);
        } else {
          Runtime.getRuntime().exec("cmd /c \"" + url + "\"");
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
  }


  public boolean openFolderAvailable() {
    return true;
  }


  public void openFolder(File file) {
    try {
        String folder = file.getAbsolutePath();

        Runtime.getRuntime().exec("explorer \"" + folder + "\"");
    } catch (Exception e) {
        e.printStackTrace();
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  // Code partially thanks to Richard Quirk from:
  // http://quirkygba.blogspot.com/2009/11/setting-environment-variables-in-java.html

  static WinLibC clib = (WinLibC) Native.loadLibrary("msvcrt", WinLibC.class);

  public interface WinLibC extends Library {
    //WinLibC INSTANCE = (WinLibC) Native.loadLibrary("msvcrt", WinLibC.class);
    //libc = Native.loadLibrary("msvcrt", WinLibC.class);
    public int _putenv(String name);
}


  public void setenv(String variable, String value) {
    //WinLibC clib = WinLibC.INSTANCE;
    clib._putenv(variable + "=" + value);
  }


  public String getenv(String variable) {
    return System.getenv(variable);
  }


  public int unsetenv(String variable) {
    //WinLibC clib = WinLibC.INSTANCE;
    //clib._putenv(variable + "=");
    //return 0;
    return clib._putenv(variable + "=");
  }
}
