/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-06 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package uecide.app;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.text.*;

import java.util.*;


/**
 * Message console that sits below the editing area.
 * <P>
 * Debugging this class is tricky... If it's throwing exceptions,
 * don't take over System.err, and debug while watching just System.out
 * or just write println() or whatever directly to systemOut or systemErr.
 */
public class EditorConsole extends JScrollPane {
  Editor editor;

  JTextPane consoleTextPane;
  BufferedStyledDocument consoleDoc;

  MutableAttributeSet stdStyle;
  MutableAttributeSet errStyle;
  MutableAttributeSet warnStyle;

  int maxLineCount;


  public EditorConsole(Editor editor) {
    this.editor = editor;

    maxLineCount = Base.preferences.getInteger("console.length");

    consoleDoc = new BufferedStyledDocument(10000, maxLineCount);
    consoleTextPane = new JTextPane(consoleDoc);
    consoleTextPane.setEditable(false);

    // necessary?
    MutableAttributeSet standard = new SimpleAttributeSet();
    StyleConstants.setAlignment(standard, StyleConstants.ALIGN_LEFT);
    consoleDoc.setParagraphAttributes(0, 0, standard, true);

    // build styles for different types of console output
    Color bgColor    = Base.theme.getColor("console.color");
    Color fgColorOut = Base.theme.getColor("console.output.color");
    Color fgColorErr = Base.theme.getColor("console.error.color");
    Color fgColorWarn = Base.theme.getColor("console.warning.color");
    Font font        = Base.preferences.getFont("console.font");

    stdStyle = new SimpleAttributeSet();
    StyleConstants.setForeground(stdStyle, fgColorOut);
    StyleConstants.setBackground(stdStyle, bgColor);
    StyleConstants.setFontSize(stdStyle, font.getSize());
    StyleConstants.setFontFamily(stdStyle, font.getFamily());
    StyleConstants.setBold(stdStyle, font.isBold());
    StyleConstants.setItalic(stdStyle, font.isItalic());

    errStyle = new SimpleAttributeSet();
    StyleConstants.setForeground(errStyle, fgColorErr);
    StyleConstants.setBackground(errStyle, bgColor);
    StyleConstants.setFontSize(errStyle, font.getSize());
    StyleConstants.setFontFamily(errStyle, font.getFamily());
    StyleConstants.setBold(errStyle, font.isBold());
    StyleConstants.setItalic(errStyle, font.isItalic());

    warnStyle = new SimpleAttributeSet();
    StyleConstants.setForeground(warnStyle, fgColorWarn);
    StyleConstants.setBackground(warnStyle, bgColor);
    StyleConstants.setFontSize(warnStyle, font.getSize());
    StyleConstants.setFontFamily(warnStyle, font.getFamily());
    StyleConstants.setBold(warnStyle, font.isBold());
    StyleConstants.setItalic(warnStyle, font.isItalic());

    consoleTextPane.setBackground(bgColor);

    // add the jtextpane to this scrollpane
    this.setViewportView(consoleTextPane);

    // calculate height of a line of text in pixels
    // and size window accordingly
    FontMetrics metrics = this.getFontMetrics(font);
    int height = metrics.getAscent() + metrics.getDescent();
    int lines = Base.preferences.getInteger("console.lines"); //, 4);
    int sizeFudge = 6; //10; // unclear why this is necessary, but it is
    setPreferredSize(new Dimension(1024, (height * lines) + sizeFudge));
    setMinimumSize(new Dimension(1024, (height * 4) + sizeFudge));

    if (Base.isMacOS()) {
      setBorder(null);
    }
  }

    synchronized public void setFont(Font font) {
        if (stdStyle == null || font == null || errStyle == null) {
            return;
        }
        StyleConstants.setFontSize(stdStyle, font.getSize());
        StyleConstants.setFontFamily(stdStyle, font.getFamily());
        StyleConstants.setBold(stdStyle, font.isBold());
        StyleConstants.setItalic(stdStyle, font.isItalic());

        StyleConstants.setFontSize(errStyle, font.getSize());
        StyleConstants.setFontFamily(errStyle, font.getFamily());
        StyleConstants.setBold(errStyle, font.isBold());
        StyleConstants.setItalic(errStyle, font.isItalic());
    }

  
  public void handleQuit() {
  }


  public void write(byte b[], int offset, int length, int chan) {
    // we could do some cross platform CR/LF mangling here before outputting
    // add text to output document
    message(new String(b, offset, length), chan, false);
  }


  // added sync for 0091.. not sure if it helps or hinders
  synchronized public void message(String what, int chan, boolean advance) {
    if (advance) {
      appendText("\n", chan);
    }
    appendText(what, chan);
  }

  synchronized private void appendText(String txt, int chan) {
    if (consoleDoc == null) {
        System.err.print(txt);
    }
    switch (chan) {
        case 0:
        default:
            consoleDoc.appendString(txt, stdStyle);
            break;
        case 1:
            consoleDoc.appendString(txt, warnStyle);
            break;
        case 2:
            consoleDoc.appendString(txt, errStyle);
            break;
    }
    consoleDoc.insertAll();
    consoleTextPane.setCaretPosition(consoleDoc.getLength());
  }


  public void clear() {
    try {
      consoleDoc.remove(0, consoleDoc.getLength());
    } catch (BadLocationException e) {
    }
  }
}


// . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


/**
 * Buffer updates to the console and output them in batches. For info, see:
 * http://java.sun.com/products/jfc/tsc/articles/text/element_buffer and
 * http://javatechniques.com/public/java/docs/gui/jtextpane-speed-part2.html
 * appendString() is called from multiple threads, and insertAll from the
 * swing event thread, so they need to be synchronized
 */
class BufferedStyledDocument extends DefaultStyledDocument {
  ArrayList<ElementSpec> elements = new ArrayList<ElementSpec>();
  int maxLineLength, maxLineCount;
  int currentLineLength = 0;
  boolean needLineBreak = false;
  boolean hasAppendage = false;

  public BufferedStyledDocument(int maxLineLength, int maxLineCount) {
    this.maxLineLength = maxLineLength;
    this.maxLineCount = maxLineCount;
  }

  /** buffer a string for insertion at the end of the DefaultStyledDocument */
  public synchronized void appendString(String str, AttributeSet a) {
    // do this so that it's only updated when needed (otherwise console
    // updates every 250 ms when an app isn't even running.. see bug 180)
    hasAppendage = true;

    // process each line of the string
    while (str.length() > 0) {
      // newlines within an element have (almost) no effect, so we need to
      // replace them with proper paragraph breaks (start and end tags)
      if (needLineBreak || currentLineLength > maxLineLength) {
        elements.add(new ElementSpec(a, ElementSpec.EndTagType));
        elements.add(new ElementSpec(a, ElementSpec.StartTagType));
        currentLineLength = 0;
      }

      if (str.indexOf('\n') == -1) {
        elements.add(new ElementSpec(a, ElementSpec.ContentType,
          str.toCharArray(), 0, str.length()));
        currentLineLength += str.length();
        needLineBreak = false;
        str = str.substring(str.length()); // eat the string
      } else {
        elements.add(new ElementSpec(a, ElementSpec.ContentType,
          str.toCharArray(), 0, str.indexOf('\n') + 1));
        needLineBreak = true;
        str = str.substring(str.indexOf('\n') + 1); // eat the line
      }
    }
  }

  /** insert the buffered strings */
  public synchronized void insertAll() {
    ElementSpec[] elementArray = new ElementSpec[elements.size()];
    elements.toArray(elementArray);

    try {
      // check how many lines have been used so far
      // if too many, shave off a few lines from the beginning
      Element element = super.getDefaultRootElement();
      int lineCount = element.getElementCount();
      int overage = lineCount - maxLineCount;
      if (overage > 0) {
        // if 1200 lines, and 1000 lines is max,
        // find the position of the end of the 200th line
        //systemOut.println("overage is " + overage);
        Element lineElement = element.getElement(overage);
        if (lineElement == null) return;  // do nuthin

        int endOffset = lineElement.getEndOffset();
        // remove to the end of the 200th line
        super.remove(0, endOffset);
      }
      super.insert(super.getLength(), elementArray);

    } catch (BadLocationException e) {
      // ignore the error otherwise this will cause an infinite loop
      // maybe not a good idea in the long run?
    }
    elements.clear();
    hasAppendage = false;
  }
}
