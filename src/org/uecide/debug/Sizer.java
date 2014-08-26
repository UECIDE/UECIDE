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

package org.uecide.debug;

import org.uecide.*;

import java.io.*;
import java.util.*;
import java.text.MessageFormat;

public class Sizer implements MessageConsumer {
    private String buildPath, sketchName;
    private String firstLine;
    private long text;
    private long data;
    private long bss;
    private Sketch sketch;

    public Sizer(Sketch s) {
        sketch = s;
    }

    public void computeSize() {
        sketch.settings.put("filename", sketch.getName());
        PropertyFile props = sketch.mergeAllProperties();
        String commandSize[] = sketch.parseString(props.get("compile.size")).split("::");

        int r = 0;

        try {
            firstLine = null;
            Process process = Runtime.getRuntime().exec(commandSize);
            MessageSiphon in = new MessageSiphon(process.getInputStream(), this);
            in.setChannel(1);
            MessageSiphon err = new MessageSiphon(process.getErrorStream(), this);
            err.setChannel(2);

            boolean running = true;

            while(running) {
                try {
                    if(in.thread != null)
                        in.thread.join();

                    if(err.thread != null)
                        err.thread.join();

                    r = process.waitFor();
                    running = false;
                } catch(InterruptedException intExc) { }
            }
        } catch(Exception e) {
            // The default Throwable.toString() never returns null, but apparently
            // some sub-class has overridden it to do so, thus we need to check for
            // it.  See: http://www.arduino.cc/cgi-bin/yabb2/YaBB.pl?num=1166589459
            Base.error(e);
        }
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

    public void warning(String s) {
        sketch.warning(s);
    }
    public void error(String s) {
        sketch.error(s);
    }

    public void message(String s) {
        if(firstLine == null) {
            firstLine = s;
        } else {
            StringTokenizer st = new StringTokenizer(s, " ");

            try {
                text = (new Integer(st.nextToken().trim())).longValue();
                data = (new Integer(st.nextToken().trim())).longValue();
                bss += (new Integer(st.nextToken().trim())).longValue();
            } catch(Exception e) {
                Base.error(e);
            }
        }
    }
}
