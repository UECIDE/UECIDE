package org.uecide;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class FilesystemSettingsDialog extends JDialog {

    JComboBox<String>   type;
    JLabel              typeLabel;

    JTextField          size;
    JLabel              sizeLabel;
    JButton             sizeSelect;

    JLabel              current;
    JLabel              currentLabel;

    JButton             okButton;
    JButton             cancelButton;

    Editor editor;

    public static final int OK = 1;
    public static final int CANCEL = 0;

    int result = CANCEL;

    public FilesystemSettingsDialog(Editor ed) throws IOException {
        result = CANCEL;
        editor = ed;

        setModalityType(JDialog.ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout());

        JPanel pan = new JPanel();
        add(pan, BorderLayout.CENTER);

        pan.setLayout(new GridBagLayout());

        GridBagConstraints con = new GridBagConstraints();

        con.gridx = 0;
        con.gridy = 0;
        con.fill = GridBagConstraints.HORIZONTAL;
        typeLabel = new JLabel("Filesystem Type: ");
        pan.add(typeLabel, con);
        type = new JComboBox<String>(new String[] {
            "None",
            "FAT12",
            "FAT16",
            "FAT32",
            "SPIFFS"
        });
        type.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateDisplayOptions((String)type.getSelectedItem());
            }
        });
        con.gridx = 1;
        con.gridwidth = 2;
        pan.add(type, con);

        con.gridx = 0;
        con.gridwidth = 1;
        con.gridy++;

        sizeLabel = new JLabel("Filesystem Size (KiB): ");
        pan.add(sizeLabel, con);
        con.gridx = 1;
        size = new JTextField("512", 10);
        pan.add(size, con);
        con.gridx = 2;
        sizeSelect = new JButton(IconManager.getIcon(16, "misc.downarrow"));

        sizeSelect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                popupSizeMenu(sizeSelect, 0, 0);
            }
        });


        pan.add(sizeSelect, con);

        con.gridx = 0;
        con.gridy++;

        currentLabel = new JLabel("Current Used Size: ");
        pan.add(currentLabel, con);

        con.gridx = 1;
        File root = editor.loadedSketch.getFilesystemFolder();
        if (root.exists()) {
            long used = Utils.getFolderSize(root);
            used /= 1024;
            current = new JLabel(used + " kiB");
        } else {
            current = new JLabel("No files found");
        }
        pan.add(current, con);
    
        con.gridx = 0;
        con.gridy++;


        JPanel buttonBar = new JPanel();
        add(buttonBar, BorderLayout.SOUTH);

        okButton = new JButton("OK");
        cancelButton = new JButton("Cancel");
        buttonBar.add(cancelButton);
        buttonBar.add(okButton);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                result = OK;
                setVisible(false);
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                result = CANCEL;
                setVisible(false);
            }
        });

        setResizable(false);

        setLocationRelativeTo(editor);
        setSize(400, 300);
    }

    void updateDisplayOptions(String selectedType) {
        if (selectedType.equals("None")) {
            size.setVisible(false);
            sizeLabel.setVisible(false);
            sizeSelect.setVisible(false);
            current.setVisible(false);
            currentLabel.setVisible(false);
        } else if (selectedType.equals("FAT12")) {
            size.setVisible(true);
            sizeLabel.setVisible(true);
            sizeSelect.setVisible(true);
            current.setVisible(true);
            currentLabel.setVisible(true);
        } else if (selectedType.equals("FAT16")) {
            size.setVisible(true);
            sizeLabel.setVisible(true);
            sizeSelect.setVisible(true);
            current.setVisible(true);
            currentLabel.setVisible(true);
        } else if (selectedType.equals("FAT32")) {
            size.setVisible(true);
            sizeLabel.setVisible(true);
            sizeSelect.setVisible(true);
            current.setVisible(true);
            currentLabel.setVisible(true);
        } else if (selectedType.equals("SPIFFS")) {
            size.setVisible(true);
            sizeLabel.setVisible(true);
            sizeSelect.setVisible(true);
            current.setVisible(true);
            currentLabel.setVisible(true);
        }
    }

    void popupSizeMenu(JComponent c, int x, int y) {
        JPopupMenu menu = new JPopupMenu();

        ActionListener setValue = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JMenuItem item = (JMenuItem)e.getSource();
                size.setText(item.getActionCommand());
            }
        };

        JMenuItem i = new JMenuItem("Auto (Experimental)");
        i.setActionCommand("0");
        i.addActionListener(setValue);
        menu.add(i);

        i = new JMenuItem("512kiB");
        i.setActionCommand("512");
        i.addActionListener(setValue);
        menu.add(i);

        i = new JMenuItem("1MiB");
        i.setActionCommand("1024");
        i.addActionListener(setValue);
        menu.add(i);

        i = new JMenuItem("2MiB");
        i.setActionCommand("2048");
        i.addActionListener(setValue);
        menu.add(i);

        i = new JMenuItem("3MiB");
        i.setActionCommand("3072");
        i.addActionListener(setValue);
        menu.add(i);

        i = new JMenuItem("4MiB");
        i.setActionCommand("4096");
        i.addActionListener(setValue);
        menu.add(i);

        i = new JMenuItem("5MiB");
        i.setActionCommand("5120");
        i.addActionListener(setValue);
        menu.add(i);

        i = new JMenuItem("6MiB");
        i.setActionCommand("6144");
        i.addActionListener(setValue);
        menu.add(i);

        i = new JMenuItem("7MiB");
        i.setActionCommand("7168");
        i.addActionListener(setValue);
        menu.add(i);


        i = new JMenuItem("8MiB");
        i.setActionCommand("8192");
        i.addActionListener(setValue);
        menu.add(i);


        menu.show(c, x, y);
    }

    public void setFilesystemType(String t) {
        if (t == null) t = "None";
        type.setSelectedItem(t);
        updateDisplayOptions(t);
    }

    public String getFilesystemType() {
        return (String)type.getSelectedItem();
    }

    public long getFilesystemSize() {
        return Utils.s2l(size.getText());
    }

    public void setFilesystemSize(long s) {
        size.setText(Long.toString(s));
    }

    public int getResult() {    
        return result;
    }
}
