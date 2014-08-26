
package org.uecide.editors;

import org.uecide.*;
import org.uecide.debug.*;

import java.io.*;
import javax.swing.*;

public class none { //implements EditorBase {

    public File file;
    public JPanel panel;

    public none(File f, JPanel p) {
        panel = p;
        file = f;

        JLabel l = new JLabel("The file " + f.getName() + " cannot be edited.");
        p.add(l);
    }
}
