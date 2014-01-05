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

public class Theme extends PropertyFile {

    public static HashMap<String, PropertyFile>themeList = new HashMap<String, PropertyFile>();

    public Theme(File u) {
        super(u);
    }

    public Theme(File u, File d) {
        super(u, d);
    }

    public void loadNewTheme(File f) {
        loadNewUserFile(f);
        for (Editor e : Base.editors) {
            e.applyPreferences();
        }
    }

    public static void loadThemeList() {
        File sysThemes = Base.getSystemThemesFolder();
        File userThemes = Base.getUserThemesFolder();
        if (sysThemes.exists() && sysThemes.isDirectory()) {    
            String fl[] = sysThemes.list();
            for (String f : fl) {
                PropertyFile nf = new PropertyFile(new File(f));
                themeList.put(nf.get("name"), nf);
            }
        }
        if (userThemes.exists() && userThemes.isDirectory()) {    
            String fl[] = userThemes.list();
            for (String f : fl) {
                PropertyFile nf = new PropertyFile(new File(f));
                themeList.put(nf.get("name"), nf);
            }
        }
    }
}
