package org.uecide;

import java.awt.event.*;

public class JSActionListener implements ActionListener {
    public JSAction action;
    public Editor editor;

    public JSActionListener(Editor ed, JSAction a) {
        editor = ed;
        action = a;
    }

    public void actionPerformed(ActionEvent e) {
        action.activate(editor);
    }
}
