package org.uecide.gui.swing;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.uecide.Context;

public class FileMenu extends JMenu {
    
    Context ctx;

    JMenuItem newMenu;
    JMenuItem openMenu;
    JMenuItem openGit;
    RecentFileMenu recentMenu;
    FrequentFileMenu frequentMenu;
    ExampleFileMenu examplesMenu;
    JMenuItem closeMenu;
    JMenuItem saveMenu;
    JMenuItem saveAsMenu;
    JMenuItem exportSarMenu;
    JMenuItem importSarMenu;
    JMenuItem preferencesMenu;
    JMenuItem quitMenu;

    public FileMenu(Context c) {
        super("File");
        ctx = c;

        int defaultModifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        newMenu = new JMenuItem("New");
        newMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, defaultModifiers));
        newMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                ctx.action("newSketch");
            }
        });

        openMenu = new JMenuItem("Open...");
        openMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, defaultModifiers));
        openMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                ctx.action("openSketch");
            }
        });

        openGit = new JMenuItem("Open Git Repository...");

        recentMenu = new RecentFileMenu(ctx);
        frequentMenu = new FrequentFileMenu(ctx);
        examplesMenu = new ExampleFileMenu(ctx);

        closeMenu = new JMenuItem("Close");
        closeMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, defaultModifiers));
        closeMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                ctx.action("closeSession");
            }
        });

        saveMenu = new JMenuItem("Save");
        saveMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, defaultModifiers));
        saveMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                ctx.action("saveSketch");
            }
        });
    
        saveAsMenu = new JMenuItem("Save As...");
        saveAsMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, defaultModifiers | InputEvent.SHIFT_DOWN_MASK));
        saveAsMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                ctx.action("saveSketchAs");
            }
        });
    
        exportSarMenu = new JMenuItem("Export as SAR... (TODO)");
        importSarMenu = new JMenuItem("Import SAR... (TODO)");
        preferencesMenu = new JMenuItem("Preferences... (TODO)");
        preferencesMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                new PreferencesGui();
            }
        });

        quitMenu = new JMenuItem("Quit");
        quitMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                ctx.action("closeAllSessions");
            }
        });

        add(newMenu);
        add(openMenu);
        add(openGit);
        add(recentMenu);
        add(frequentMenu);
        add(examplesMenu);

        addSeparator();

        add(saveMenu);
        add(saveAsMenu);

        addSeparator();

        add(exportSarMenu);
        add(importSarMenu);

        addSeparator();

        add(preferencesMenu);

        addSeparator();

        add(closeMenu);
        add(quitMenu);
    }
}
