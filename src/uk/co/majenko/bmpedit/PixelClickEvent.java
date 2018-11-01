package uk.co.majenko.bmpedit;

import java.awt.*;

public class PixelClickEvent {

    Point location;
    Point previousLocation;
    Object source;
    int buttonNumber;
    int mods;

    PixelClickEvent(Point in, Point out, int button, Object image, int modifiers) {
        location = in;
        previousLocation = out;
        buttonNumber = button;
        source = image;
        mods = modifiers;
    }

    public Object getSource() {
        return source;
    }

    public Point getPixel() {
        return location;
    }

    public Point getPreviousPixel() {
        return previousLocation;
    }

    public int getButton() {
        return buttonNumber;
    }

    public String toString() {
        return String.format("[Pixel: %d,%d Button %d]", location.x, location.y, buttonNumber);
    }

    public int getModifiersEx() {
        return mods;
    }
}
