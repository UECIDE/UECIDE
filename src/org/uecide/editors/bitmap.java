/*
 * Copyright (c) 2014, Majenko Technologies
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 *
 * * Neither the name of Majenko Technologies nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.uecide.editors;

import org.uecide.*;
import org.uecide.plugin.*;
import org.uecide.debug.*;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.print.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.text.*;
import javax.swing.JToolBar;

import javax.imageio.*;


public class bitmap extends JPanel implements EditorBase {
    File file = null;
    boolean modified = false;
    JScrollPane scrollPane;
    Sketch sketch;
    Editor editor;
    ImageIcon image;
    JLabel label;
    BufferedImage bImage;
    double zoom = 1.0;

    public bitmap(Sketch s, File f, Editor e) {
        editor = e;
        sketch = s;
        this.setLayout(new BorderLayout());
        scrollPane = new JScrollPane();
        this.add(scrollPane, BorderLayout.CENTER);
        scrollPane.addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                int notches = e.getWheelRotation();

                if(notches < 0) {
                    zoomIn(1.1 * (double)(-notches));
                } else {
                    zoomOut(1.1 * (double)notches);
                }
            }
        });
        loadFile(f);
    }

    public void zoomIn(double factor) {
        zoom *= factor;
        int newImageWidth = (int)((double)bImage.getWidth() * zoom);
        int newImageHeight = (int)((double)bImage.getHeight() * zoom);
        System.err.println(newImageWidth + " x " + newImageHeight);
        BufferedImage resizedImage = new BufferedImage(newImageWidth, newImageHeight, bImage.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(bImage, 0, 0, newImageWidth, newImageHeight, null);
        g.dispose();
        image = new ImageIcon(resizedImage);
        label.setIcon(image);
    }

    public void zoomOut(double factor) {
        zoom /= factor;
        int newImageWidth = (int)((double)bImage.getWidth() * zoom);
        int newImageHeight = (int)((double)bImage.getHeight() * zoom);
        BufferedImage resizedImage = new BufferedImage(newImageWidth, newImageHeight, bImage.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(bImage, 0, 0, newImageWidth, newImageHeight, null);
        g.dispose();
        image = new ImageIcon(resizedImage);
        label.setIcon(image);
    }

    public boolean loadFile(File f) {
        try {
            file = f;
            bImage = ImageIO.read(f);
            image = new ImageIcon(bImage);
            label = new JLabel(image);
            scrollPane.setViewportView(label);
            zoom = 1.0;
        } catch(Exception e) {
            sketch.error(e);
        }

        return true;
    }

    public boolean isModified() {
        return false;
    }

    public String getText() {
        return "";
    }

    public String getText(int s, int e) {
        return "";
    }

    public void setText(String text) {
    }

    public void setModified(boolean m) {
    }

    public void scrollTo(final int pos) {
    }

    public File getFile() {
        return file;
    }

    public void revertFile() {
        reloadFile();
    }

    public void populateMenu(JMenu menu, int flags) {
    }

    // Save the file contents to disk
    public boolean save() {
        return true;
    }

    public void reloadFile() {
        loadFile(file);
    }

    public void insertAtCursor(String text) {
    }

    public void insertAtStart(String text) {
    }

    public void insertAtEnd(String text) {
    }

    public boolean saveTo(File f) {
        return true;
    }

    public void refreshSettings() {
        // Nothing to do
    }

    public void setSelection(int start, int end) {
    }

    public int getSelectionStart() {
        return 0;
    }

    public int getSelectionEnd() {
        return 0;
    }

    public void setSelectedText(String text) {
    }

    public String getSelectedText() {
        return "";
    }

    public void highlightLine(int line, Color color) {
    }

    public void clearHighlights() {
    }

    public void gotoLine(int line) {
    }

    public void requestFocus() {
    }

    public void setCursorPosition(int pos) {
    }

    public int getCursorPosition() {
        return 0;
    }

    public void removeAllFlags() {
    }

    public void flagLine(int line, Icon icon, int group) {
    }

    public void removeFlag(int line) {
    }

    public void removeFlagGroup(int group) {
    }

    public void clearKeywords() {
    }

    public void addKeyword(String name, Integer type) {
    }

    public void repaint() {
    }
}
