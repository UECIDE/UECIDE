package uecide.app.editors;

import uecide.app.*;
import uecide.app.debug.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;

public interface EditorBase {
    public boolean isModified();
    public String getText();
    public void setText(String text);
    public void setModified(boolean m);
    public File getFile();
    public void populateMenu(JMenu menu, int flags);
    public boolean save();
    public boolean saveTo(File file);
    public void reloadFile();
    public void requestFocus();
    public void insertAtCursor(String text);
    public void insertAtStart(String text);
    public void insertAtEnd(String text);
    public void refreshSettings();
    public String getSelectedText();
    public void setSelectedText(String text);
    public int getSelectionStart();
    public int getSelectionEnd();
    public void setSelection(int start, int end);
    public void highlightLine(int line, Color color);
    public void clearHighlights();
}
