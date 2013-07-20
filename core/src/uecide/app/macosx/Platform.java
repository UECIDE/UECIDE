package uecide.app.macosx;

import java.awt.Insets;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.net.URI;

import javax.swing.UIManager;

import com.apple.eio.FileManager;

import uecide.app.Base;
import uecide.app.Theme;

/**
 * Platform handler for Mac OS X.
 */
public class Platform extends uecide.app.Platform {

  public void setLookAndFeel() throws Exception {
    // Use the Quaqua L & F on OS X to make JFileChooser less awful
    String laf = Theme.get("window.laf.macosx");
    if ((laf != null) && (laf != "default")) {
       UIManager.setLookAndFeel(laf);
    } else {
        UIManager.setLookAndFeel("ch.randelshofer.quaqua.QuaquaLookAndFeel");
    }

    // undo quaqua trying to fix the margins, since we've already
    // hacked that in, bit by bit, over the years
    UIManager.put("Component.visualMargin", new Insets(1, 1, 1, 1));
  }


  public void init(Base base) {
    System.setProperty("apple.laf.useScreenMenuBar", "true");
    ThinkDifferent.init(base);
    /*
    try {
      String name = "uecide.app.macosx.ThinkDifferent";
      Class osxAdapter = ClassLoader.getSystemClassLoader().loadClass(name);

      Class[] defArgs = { Base.class };
      Method registerMethod = osxAdapter.getDeclaredMethod("register", defArgs);
      if (registerMethod != null) {
        Object[] args = { this };
        registerMethod.invoke(osxAdapter, args);
      }
    } catch (NoClassDefFoundError e) {
      // This will be thrown first if the OSXAdapter is loaded on a system without the EAWT
      // because OSXAdapter extends ApplicationAdapter in its def
      System.err.println("This version of Mac OS X does not support the Apple EAWT." +
                         "Application Menu handling has been disabled (" + e + ")");

    } catch (ClassNotFoundException e) {
      // This shouldn't be reached; if there's a problem with the OSXAdapter
      // we should get the above NoClassDefFoundError first.
      System.err.println("This version of Mac OS X does not support the Apple EAWT. " +
                         "Application Menu handling has been disabled (" + e + ")");
    } catch (Exception e) {
      System.err.println("Exception while loading BaseOSX:");
      e.printStackTrace();
    }
    */
  }


  public File getSettingsFolder() throws Exception {
    return new File(getLibraryFolder(), Theme.get("product"));
  }


  public File getDefaultSketchbookFolder() throws Exception {
    return new File(getDocumentsFolder(), Theme.get("product"));
    /*
    // looking for /Users/blah/Documents/Processing
    try {
      Class clazz = Class.forName("uecide.app.BaseMacOS");
      Method m = clazz.getMethod("getDocumentsFolder", new Class[] { });
      String documentsPath = (String) m.invoke(null, new Object[] { });
      sketchbookFolder = new File(documentsPath, "Arduino");

    } catch (Exception e) {
      sketchbookFolder = promptSketchbookLocation();
    }
    */
  }


  public void openURL(String url) throws Exception {
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
          e.printStackTrace();
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
        e.printStackTrace();
        }
      }
    }


  public boolean openFolderAvailable() {
    return true;
  }


  public void openFolder(File file) throws Exception {
    //openURL(file.getAbsolutePath());  // handles char replacement, etc
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


  protected String getLibraryFolder() throws FileNotFoundException {
    return FileManager.findFolder(kUserDomain, kDomainLibraryFolderType);
  }


  protected String getDocumentsFolder() throws FileNotFoundException {
    return FileManager.findFolder(kUserDomain, kDocumentsFolderType);
  }
}
