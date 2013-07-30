/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Sizer - computes the size of a .hex file
  Part of the Arduino project - http://www.arduino.cc/

  Copyright (c) 2006 David A. Mellis

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
  
  $Id$
*/

package uecide.app.debug;

import uecide.app.*;

import java.io.*;
import java.util.*;
import java.text.MessageFormat;

public class Sizer implements MessageConsumer {
    private String buildPath, sketchName;
    private String firstLine;
    private RunnerException exception;
    private long text;
    private long data;
    private long bss;

    private Editor editor;

    public Sizer(Editor editor) {
        this.editor = editor;
    }
  
    public void computeSize() throws RunnerException {

        editor.sketch.settings.put("filename", editor.sketch.name);
        HashMap<String, String> all = editor.sketch.mergeAllProperties();
        String commandSize[] = editor.sketch.parseString(all.get("compile.size")).split("::");

        int r = 0;
        try {
            exception = null;
            firstLine = null;
            Process process = Runtime.getRuntime().exec(commandSize);
            MessageSiphon in = new MessageSiphon(process.getInputStream(), this);
            in.setChannel(1);
            MessageSiphon err = new MessageSiphon(process.getErrorStream(), this);
            err.setChannel(2);

            boolean running = true;

            while(running) {
                try {
                    if (in.thread != null)
                        in.thread.join();
                    if (err.thread != null)
                        err.thread.join();
                    r = process.waitFor();
                    running = false;
                } catch (InterruptedException intExc) { }
            }
        } catch (Exception e) {
            // The default Throwable.toString() never returns null, but apparently
            // some sub-class has overridden it to do so, thus we need to check for
            // it.  See: http://www.arduino.cc/cgi-bin/yabb2/YaBB.pl?num=1166589459
            exception = new RunnerException(
                (e.toString() == null) ? e.getClass().getName() + r : e.toString() + r);
        }

        if (exception != null)
            throw exception;
    }

    public long progSize() {
        return (text + data);
    }

    public long ramSize() {
        return (data + bss);
    }

    public long textSize() {
        return text;
    }

    public long dataSize() {
        return data;
    }

    public long bssSize() {
        return bss;
    }

    public void message(String s, int c) {
        if (c == 2) {
            System.err.print(s);
        } else {
            message(s);
        }
    }
  
  public void message(String s) {
    if (firstLine == null)
      firstLine = s;
    else {
      StringTokenizer st = new StringTokenizer(s, " ");
      try {
        text = (new Integer(st.nextToken().trim())).longValue();
        data = (new Integer(st.nextToken().trim())).longValue();
        bss += (new Integer(st.nextToken().trim())).longValue();
      } catch (NoSuchElementException e) {
        exception = new RunnerException(e.toString());
      } catch (NumberFormatException e) {
        exception = new RunnerException(e.toString());
      }
    }
  }
}
