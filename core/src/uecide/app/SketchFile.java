package uecide.app;

import uecide.app.debug.RunnerException;
import uecide.app.preproc.*;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

public class SketchFile {
    public SketchEditor textArea;
    public File file;
    public boolean modified;
    public String[] includes;
    public String[] prototypes;
    public int headerLines;

    public void writeToFolder(File f) {
        File outputFile = new File(f, file.getName());
        textArea.writeFile(outputFile);
    }

    public void save() {
        textArea.writeFile(file);
        textArea.setModified(false);
    }

    public void nameCode(String name) {
        File oldFile = file;
        file = new File(oldFile.getParentFile(), name);
        save();
        oldFile.delete();
    }

    public void setText(String text) {
        textArea.setText(text);
    }

    public File getFile() {
        return file;
    }

    public void setFile(File f) {
        file = f;
        textArea.setFile(f);
    }
}

