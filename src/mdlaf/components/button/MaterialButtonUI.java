package mdlaf.components.button;

import mdlaf.animation.MaterialUIMovement;
import mdlaf.utils.MaterialDrawingUtils;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.util.Map;

public class MaterialButtonUI extends BasicButtonUI {

    public static ComponentUI createUI(final JComponent c) {
        return new MaterialButtonUI();
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);

        AbstractButton button = (AbstractButton) c;
        button.setOpaque(UIManager.getBoolean("Button.opaque"));
        button.setBorder(UIManager.getBorder("Button.border"));
        button.setBackground(UIManager.getColor("Button.background"));
        button.setForeground(UIManager.getColor("Button.foreground"));
        button.setFont(UIManager.getFont("Button.font"));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(MaterialUIMovement.getMovement(button, UIManager.getColor("Button.mouseHoverColor")));
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        g = MaterialDrawingUtils.getAliasedGraphics(g);
        if (b.isContentAreaFilled()) {
            paintBackground(g, b);
        }
        super.paint(g, c);
    }

    private void paintBackground(Graphics g, JComponent c) {
        g.setColor(c.getBackground());
        int pad = UIManager.getInt("Button.backgroundPad");
        AbstractButton b = (AbstractButton)c;
        if ((b.getText() != null) && (!b.getText().equals(""))) {
            g.fillRoundRect(pad, pad, c.getWidth() - (pad * 2), c.getHeight() - (pad * 2), 7, 7);
        } else {
            if (UIManager.getBoolean("Button.backgroundCircle")) {
                g.fillOval(pad, pad, c.getWidth() - (pad * 2), c.getHeight() - (pad * 2));
            } else {
                g.fillRoundRect(pad, pad, c.getWidth() - (pad * 2), c.getHeight() - (pad * 2), 7, 7);
            }
        }
    }

    @Override
    protected void paintButtonPressed(Graphics g, AbstractButton b) {
        super.paintButtonPressed(g, b);
    }

    @Override
    public void update(Graphics g, JComponent c) {
        super.update(g, c);
        c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
