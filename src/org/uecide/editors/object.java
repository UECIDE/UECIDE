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
import org.uecide.plugin.*;

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

import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.*;

public class object extends text {

    public object(Sketch s, File f, Editor e) throws IOException {
        super(s, f, e);
        textArea.setEditable(false);
    }

    @Override
    public boolean loadFile(File f) {
        file = f;

        Context ctx = sketch.getContext();

        ctx.startBuffer();
        String cmd = "${objdump}::-t::${object.filename}";
        ctx.set("object.filename", f.getAbsolutePath());
        String out = ctx.parseString(cmd);
        ctx.executeCommand(out, null);
        String output = ctx.endBuffer();

        textArea.setText(output);

        textArea.setCaretPosition(0);
        scrollTo(0);
        setModified(false);
        return true;
    }

    @Override
    public void reloadFile() {
        loadFile(file);
    }

    public void populateMenu(JPopupMenu menu, int flags) {
    }
    public boolean getUpdateFlag() { return false; }

    public static String emptyFile() { return ""; }


}
