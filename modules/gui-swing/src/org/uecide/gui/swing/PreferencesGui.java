package org.uecide.gui.swing;

import org.uecide.UECIDE;
import org.uecide.Debug;

import java.io.IOException;

import java.util.Enumeration;
import java.util.TreeSet;
import java.util.HashMap;

import java.awt.Font;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import javax.swing.border.EmptyBorder;

import javax.swing.tree.TreePath;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;


public class PreferencesGui extends JDialog implements TreeSelectionListener {

    JSplitPane lrPanel;
    JScrollPane treeScroll;
    JScrollPane contentScroll;
    JPanel treePanel;
    JPanel contentPanel;
    JPanel buttonsPanel;
    JButton apply;
    JButton ok;
    JButton cancel;

    JTree prefTree;
    PreferencesTreeModel model;

    public PreferencesGui() {
//        setModalityType(ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        treePanel = new JPanel();
        treeScroll = new JScrollPane(treePanel);
        treePanel.setLayout(new BorderLayout());

        contentPanel = new JPanel();
        contentScroll = new JScrollPane(contentPanel);
        contentPanel.setLayout(new GridBagLayout());

        lrPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, contentScroll);

        add(lrPanel, BorderLayout.CENTER);


        model = new PreferencesTreeModel();

        prefTree = new JTree(model);
        treePanel.add(prefTree, BorderLayout.CENTER);

        prefTree.addTreeSelectionListener(this);


        buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        apply = new JButton("Apply");
        apply.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                saveAllSettings();
                try {
                    UECIDE.cleanAndScanAllSettings();
                } catch (IOException ex) {
                    Debug.exception(ex);
                }
                UECIDE.refreshAllGuis();
            }
        });
        buttonsPanel.add(apply);

        ok = new JButton("Ok");
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                saveAllSettings();
                try {
                    UECIDE.cleanAndScanAllSettings();
                } catch (IOException ex) {
                    Debug.exception(ex);
                }
                UECIDE.refreshAllGuis();
                dispose();
            }
        });
        buttonsPanel.add(ok);
        
        cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                dispose();
            }
        });
        buttonsPanel.add(cancel);

        add(buttonsPanel, BorderLayout.SOUTH);

        pack();


        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                lrPanel.setDividerLocation(0.3d);
            }
        });
        setVisible(true);
    }

    public Dimension getMinimumSize() {
        return new Dimension(700, 500);
    }

    public Dimension getPreferredSize() {
        return new Dimension(700, 500);
    }

    public void valueChanged(TreeSelectionEvent evt) {
        contentPanel.removeAll();

        TreePath[] paths = prefTree.getSelectionPaths();

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1d;
        c.weighty = 0d;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.PAGE_START;

        for (TreePath path : paths) {
            Object ob = path.getLastPathComponent();
            if (ob instanceof PreferencesTreeEntry) {
                PreferencesTreeEntry entry = (PreferencesTreeEntry)ob;
                HashMap<String, PreferencesSetting> settings = entry.getSettings();

                TreeSet<String> keys = new TreeSet<String>(settings.keySet());

                JLabel l = new JLabel(entry.toString());
                Font f = l.getFont();
                f = f.deriveFont(Font.BOLD, 24f);
                l.setFont(f);
                l.setBorder(new EmptyBorder(5, 5, 5, 5));
                contentPanel.add(l, c);
                c.gridy++;

                for (String key : keys) {
                    PreferencesSetting setting = settings.get(key);
                    setting.setAlignmentY(Component.TOP_ALIGNMENT);
                    setting.setAlignmentX(Component.LEFT_ALIGNMENT);
                    contentPanel.add(setting, c);
                    c.gridy++;
                }
            }
        }

        c.weighty = 1d;
        contentPanel.add(Box.createGlue(), c);

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    public void saveAllSettings() {
        model.saveAllSettings();
    }

    public void updateMyTheme() {
        SwingUtilities.updateComponentTreeUI(this);
        pack();
        revalidate();
        repaint();
    }
}
