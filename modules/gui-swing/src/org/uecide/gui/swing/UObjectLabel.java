package org.uecide.gui.swing;

import org.uecide.Debug;
import org.uecide.UObject;

import javax.swing.JLabel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import java.awt.Font;

public class UObjectLabel extends JLabel {
    UObject object;
    
    public UObjectLabel(UObject ob) {
        super();
        object = ob;
        Font f = new JLabel().getFont();
        f = f.deriveFont(Font.PLAIN, f.getSize() * 0.8f);
        setFont(f);
        setBorder(new CompoundBorder(
            new EmptyBorder(1, 1, 1, 1),
            new CompoundBorder(
                new EtchedBorder(EtchedBorder.LOWERED),
                new EmptyBorder(3, 3, 3, 3)
            )
        ));

        update();
    }

    public void setObject(UObject ob) {
        object = ob;
        update();
    }

    void update() {
        if (object == null) {
            setText("Nothing Selected");
            return;
        }

        try {
            setIcon(new CleverIcon(16, object.getIcon()));
        } catch (Exception ex) {
            Debug.exception(ex);
        }
        if (object == null) return;
        setText(object.getDescription());
        setToolTipText(object.getName());
    }
}
