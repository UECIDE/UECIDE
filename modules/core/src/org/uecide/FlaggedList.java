package org.uecide;

public class FlaggedList {
    String name;
    int color;

    public static final int Red = 1;
    public static final int Green = 2;
    public static final int Yellow = 3;
    public static final int Blue = 4;

    public FlaggedList(int c, String n) {
        color = c;
        name = n;
    }

    public String toString() {
        return name;
    }

    public int getColor() {
        return color;
    }
}

