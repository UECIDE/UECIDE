/*
 * Copyright (c) 2015, Majenko Technologies
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
    public void populateMenu(JPopupMenu menu, int flags);
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
    public void gotoLine(int line);
    public int getCursorPosition();
    public void setCursorPosition(int pos);
    public void removeAllFlags();
    public void flagLine(int line, Icon icon, int group);
    public void removeFlag(int line);
    public void removeFlagGroup(int group);
    public void clearKeywords();
    public void addKeyword(String name, Integer type);
    public void repaint();
    public Component getContentPane();
    public Rectangle getViewRect();
    public void setViewPosition(Point p);
}
