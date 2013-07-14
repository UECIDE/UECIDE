/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-10 Ben Fry and Casey Reas
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

import uecide.app.debug.RunnerException;
import uecide.app.preproc.*;
import processing.core.*;


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

import org.apache.log4j.BasicConfigurator;
//import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

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
}

