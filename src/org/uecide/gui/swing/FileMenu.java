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
    ExampleFileMenu examplesMenu;
    JMenuItem revertMenu;
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
        add(newMenu);

        openMenu = new JMenuItem("Open...");
        openMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, defaultModifiers));
        openMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                ctx.action("openSketch");
            }
        });
        add(openMenu);

        openGit = new JMenuItem("Open Git Repository...");
        add(openGit);

        recentMenu = new RecentFileMenu(ctx);
        add(recentMenu);

        examplesMenu = new ExampleFileMenu(ctx);
        add(examplesMenu);

        addSeparator();

        revertMenu = new JMenuItem("Revert File");
        add(revertMenu);

        closeMenu = new JMenuItem("Close");
        closeMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, defaultModifiers));
        closeMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                ctx.action("closeSession");
            }
        });
        add(closeMenu);

        saveMenu = new JMenuItem("Save");
        saveMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, defaultModifiers));
        saveMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                ctx.action("saveSketch");
            }
        });
        add(saveMenu);
    
        saveAsMenu = new JMenuItem("Save As...");
        saveAsMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, defaultModifiers | InputEvent.SHIFT_DOWN_MASK));
        saveAsMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                ctx.action("saveSketchAs");
            }
        });
        add(saveAsMenu);

        addSeparator();
    
        exportSarMenu = new JMenuItem("Export as SAR...");
        add(exportSarMenu);

        importSarMenu = new JMenuItem("Import SAR...");
        add(importSarMenu);

        addSeparator();

        preferencesMenu = new JMenuItem("Preferences...");
        add(preferencesMenu);

        quitMenu = new JMenuItem("Quit");
        quitMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                ctx.action("closeAllSessions");
            }
        });
        add(quitMenu);

    
    }
}
