package org.uecide.plugin;

import org.uecide.*;
import org.uecide.debug.*;
import org.uecide.editors.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.tree.*;
import javax.swing.filechooser.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.imageio.*;

import say.swing.*;

import org.uecide.Console;

import java.util.Timer;


public class Navigator extends Plugin implements MouseListener {
    public static HashMap<String, String> pluginInfo = null;
    public static void setInfo(HashMap<String, String>info) { pluginInfo = info; }
    public static String getInfo(String item) { return pluginInfo.get(item); }

    Context ctx;

    public Navigator(Editor e) { editor = e; }
    public Navigator(EditorBase e) { editorTab = e; }


    public void shootConsole()
    {
        shootImage(editor.getConsole());
    }

    public void shootImage(Component c) {
        BufferedImage im = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_ARGB);
        c.paint(im.getGraphics());
        JFileChooser fc = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG Images", "png");
        fc.setFileFilter(filter);
        int n = fc.showSaveDialog(editor);
        if (n == JFileChooser.APPROVE_OPTION) {
            try {
                ImageIO.write(im, "PNG", fc.getSelectedFile());
            } catch (Exception e) {
            }
        }
    }

    public void shootEditor() {
        EditorBase sel = editor.getSelectedEditor();
        if (sel == null) {
            return;
        }
        shootImage(sel.getContentPane());
    }

    public void addToolbarButtons(JToolBar toolbar, int flags) {
    }

    public static PropertyFile getPreferencesTree() {
        return null;
    }

    public void populateMenu(JMenu menu, int flags) {
    }

    public void populateMenu(JPopupMenu menu, int flags) {
        if (flags == (Plugin.MENU_POPUP_CONSOLE | Plugin.MENU_MID)) {
            JMenuItem item = new JMenuItem("Screenshot");
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    shootConsole();
                }
            });
            menu.add(item);
        }

        if (flags == (Plugin.MENU_POPUP_EDITOR | Plugin.MENU_MID)) {
            JMenuItem item = new JMenuItem("Screenshot");
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    shootEditor();
                }
            });
            menu.add(item);
        }
    }

    public static String getPreferencesTitle() {
        return null;
    }

    public void populateContextMenu(JPopupMenu menu, int flags, DefaultMutableTreeNode node) {
    }

    public ImageIcon getFileIconOverlay(File f) { return null; }

    JPanel previewPanel = new JPreviewPanel();

    class JPreviewPanel extends JPanel {

        int skipped = 0;
        String currentText = "";
        Rectangle currentViewRect = null;

        BufferedImage renderedImage = null;
        BufferedImage imageData = null;
        Component target = null;
        Timer updateTicker = new Timer();

        public JPreviewPanel() {
            super();
            updateTicker.scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    updateImage();
                }
            }, 100, 100);
        }

        @Override 
        public Dimension getMinimumSize() {
            return new Dimension(100, 100);
        }

        EditorBase sel = null;

        public void updateImage() {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }


            if (editor == null) {
                return;
            }

            sel = editor.getSelectedEditor();
            if (sel == null) {
                return;
            }

            boolean changed = false;
            String newText = sel.getText();
            if (!newText.equals(currentText)) {
                changed = true;
                currentText = newText;
            }

            Rectangle bounds = sel.getViewRect();
            if (currentViewRect != null && 
                currentViewRect.y == bounds.y && 
                currentViewRect.height == bounds.height && !changed && (skipped < 10)) {
                skipped++;
                return;
            }
            currentViewRect = bounds;
            skipped = 0;

            target = sel.getContentPane();
            if (target == null) {
                return;
            }
            int tw = target.getWidth();
            int th = target.getHeight();

            if (tw <= 0 || th <= 0) {
                return;
            }

            if (renderedImage == null || renderedImage.getWidth() != w || renderedImage.getHeight() != h) {
                renderedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            }

            if (imageData == null || imageData.getWidth() != tw || imageData.getHeight() != h) {
                imageData = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
            }


            try {
                // Paint the target into a temporary work buffer image
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        Graphics2D workBuffer = (Graphics2D)imageData.getGraphics();
                        target.paint(workBuffer);
                    }
                });
            } catch (Exception e) {
                return;
            }

            Graphics2D mainImage = (Graphics2D)renderedImage.getGraphics();

            mainImage.drawImage(imageData, 0, 0, w, h, null);

            float toppct = (float)bounds.y / (float)th;
            float botpct = ((float)bounds.y + (float)bounds.height) / (float)th;

            int toppos = (int)((float)h * toppct);
            int botpos = (int)((float)h * botpct);

            mainImage.setPaint(new Color(0, 0, 0, 0.1f));
            mainImage.fillRect(0, 0, w, toppos);
            mainImage.fillRect(0, botpos, w, h - botpos);

            mainImage.setPaint(new Color(1, 1, 1, 0.2f));
            mainImage.fillRect(0, toppos, w, botpos - toppos);

            repaint();
        }

        @Override
        public void paintComponent(Graphics g) {
            if (renderedImage == null) {
                super.paintComponent(g);
                return;
            }
            Graphics2D g2d = (Graphics2D) g;
            g2d.drawImage(renderedImage, 0, 0, null);
        }
    };

    public void addPanelsToTabs(JTabbedPane tabs, int flags) {
        if (flags == Plugin.TABS_SIDEBAR) {
            tabs.add(previewPanel, "Navigator");
            previewPanel.addMouseListener(this);
        }
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        EditorBase sel = editor.getSelectedEditor();
        if (sel == null) {
            return;
        }
        String text = sel.getText();
        if (text == null || text.equals("")) {
            return;
        }


        Component c = sel.getContentPane();
        int mainHeight = c.getHeight();
        Rectangle viewRect = sel.getViewRect();

        float pct = (float)e.getY() / (float)previewPanel.getHeight();

        int clickPos = (int)((float)mainHeight * pct);

        int showPos = clickPos - (viewRect.height / 2);
        if (showPos < 0) {
            showPos = 0;
        }

        if (showPos + viewRect.height > mainHeight) {
            showPos = mainHeight - viewRect.height;
        }

        sel.setViewPosition(new Point(0, showPos));
        previewPanel.repaint();
    }
}

