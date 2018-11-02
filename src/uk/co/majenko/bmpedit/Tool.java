package uk.co.majenko.bmpedit;

import javax.swing.JPanel;

public interface Tool {
    public ToolButton getButton();
    public JPanel getOptionsPanel();
    public void press(ZoomableBitmap b, PixelClickEvent e);
    public void release(ZoomableBitmap b, PixelClickEvent e);
    public void drag(ZoomableBitmap b, PixelClickEvent e);
    public void select(ZoomableBitmap b);
    public void deselect(ZoomableBitmap b);
}
