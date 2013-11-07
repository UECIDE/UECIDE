package uecide.app.macosx;

import java.awt.Insets;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.net.URI;

import javax.swing.UIManager;

import com.apple.eio.FileManager;

import uecide.app.Base;

/**
 * Platform handler for Mac OS X.
 */
public class Platform extends uecide.app.Platform {

  public void setLookAndFeel() {
    try {
        // Use the Quaqua L & F on OS X to make JFileChooser less awful
        String laf = Base.theme.get("window.laf.macosx");
        if ((laf != null) && (laf != "default")) {
           UIManager.setLookAndFeel(laf);
        } else {
            UIManager.setLookAndFeel("ch.randelshofer.quaqua.QuaquaLookAndFeel");
        }

        // undo quaqua trying to fix the margins, since we've already
        // hacked that in, bit by bit, over the years
        UIManager.put("Component.visualMargin", new Insets(1, 1, 1, 1));
    } catch (Exception e) {
        Base.error(e);
    }
  }


  public void init(Base base) {
    System.setProperty("apple.laf.useScreenMenuBar", "true");
    ThinkDifferent.init(base);
  }


  public File getSettingsFolder() {
    return new File(getLibraryFolder(), Base.theme.get("product"));
  }


  public File getDefaultSketchbookFolder() {
    return new File(getDocumentsFolder(), Base.theme.get("product"));
  }


  public void openURL(String url) {
    try {
        Float javaVersion = new Float(System.getProperty("java.version").substring(0, 3)).floatValue();

        if (javaVersion < 1.6f) {
          if (url.startsWith("http://")) {
            // formerly com.apple.eio.FileManager.openURL(url);
            // but due to deprecation, instead loading dynamically
            try {
              Class<?> eieio = Class.forName("com.apple.eio.FileManager");
              Method openMethod =
                eieio.getMethod("openURL", new Class[] { String.class });
              openMethod.invoke(null, new Object[] { url });
            } catch (Exception e) {
              Base.error(e);
            }
          } else {
          // Assume this is a file instead, and just open it.
          // Extension of http://dev.processing.org/bugs/show_bug.cgi?id=1010
              Base.open(url);
          }
        } else {
          try {
            Class<?> desktopClass = Class.forName("java.awt.Desktop");
            Method getMethod = desktopClass.getMethod("getDesktop");
            Object desktop = getMethod.invoke(null, new Object[] { });

            // for Java 1.6, replacing with java.awt.Desktop.browse() 
            // and java.awt.Desktop.open()
            if (url.startsWith("http://")) {  // browse to a location
              Method browseMethod =
                desktopClass.getMethod("browse", new Class[] { URI.class });
              browseMethod.invoke(desktop, new Object[] { new URI(url) });
            } else {  // open a file
              Method openMethod =
                desktopClass.getMethod("open", new Class[] { File.class });
              openMethod.invoke(desktop, new Object[] { new File(url) });
              }
          } catch (Exception e) {
            Base.error(e);
            }
          }
        } catch (Exception ex) {
            Base.error(ex);
        }
    }


  public boolean openFolderAvailable() {
    return true;
  }


  public void openFolder(File file) {
    Base.open(file.getAbsolutePath());
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  // Some of these are supposedly constants in com.apple.eio.FileManager,
  // however they don't seem to link properly from Eclipse.

  static final int kDocumentsFolderType =
    ('d' << 24) | ('o' << 16) | ('c' << 8) | 's';
  //static final int kPreferencesFolderType =
  //  ('p' << 24) | ('r' << 16) | ('e' << 8) | 'f';
  static final int kDomainLibraryFolderType =
    ('d' << 24) | ('l' << 16) | ('i' << 8) | 'b';
  static final short kUserDomain = -32763;


  // apple java extensions documentation
  // http://developer.apple.com/documentation/Java/Reference/1.5.0
  //   /appledoc/api/com/apple/eio/FileManager.html

  // carbon folder constants
  // http://developer.apple.com/documentation/Carbon/Reference
  //   /Folder_Manager/folder_manager_ref/constant_6.html#/
  //   /apple_ref/doc/uid/TP30000238/C006889

  // additional information found int the local file:
  // /System/Library/Frameworks/CoreServices.framework
  //   /Versions/Current/Frameworks/CarbonCore.framework/Headers/


  protected String getLibraryFolder() {
    try {
        return FileManager.findFolder(kUserDomain, kDomainLibraryFolderType);
    } catch (Exception e) {
        Base.error(e);
        return null;
    }
  }


  protected String getDocumentsFolder() {
    try {
        return FileManager.findFolder(kUserDomain, kDocumentsFolderType);
    } catch (Exception e) {
        Base.error(e);
        return null;
    }
  }
}
