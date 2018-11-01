package uk.co.majenko.bmpedit;

import java.awt.event.*;

public interface PixelClickListener {
    public void pixelClicked(PixelClickEvent e);
    public void pixelPressed(PixelClickEvent e);
    public void pixelReleased(PixelClickEvent e);
    public void pixelEntered(PixelClickEvent e);
    public void pixelExited(PixelClickEvent e);
}
