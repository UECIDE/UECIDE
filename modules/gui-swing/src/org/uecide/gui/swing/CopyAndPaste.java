package org.uecide.gui.swing;

public interface CopyAndPaste {
    public void copy();
    public void cut();
    public void paste();
    public void selectAll();
    public void copyAll(String prefix, String suffix);
    public void undo();
    public void redo();
}
