/*
 * Copyright (c) 2016, Majenko Technologies
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

package org.uecide;

import java.io.*;
import java.util.*;

import org.uecide.plugin.*;

import java.util.regex.*;

public class Programmer extends UObject {
    public Programmer(File folder) {
        super(folder);
    }

    public Programmer() {
        super();
    }

    public boolean programFile(Context ctx, String file) {
        PropertyFile props = ctx.getMerged();
        if (!Base.isQuiet()) ctx.heading("Uploading firmware...");

        ctx.set("filename", file);

        if (props.get("programmer.message") != null) {
            ctx.message(ctx.parseString(props.get("programmer.message")));
        }

        String method = props.get("programmer.method");

        if (method == null) {
            ctx.error("Programmer has no programmer.method");
            return false;
        }

        /* --- SCRIPT based upload --- */
        if (method.equals("script")) {
            return (Boolean)ctx.executeKey("programmer.script");
        };


        /* --- SERIAL based upload --- */
        if (method.equals("serial")) {

            String resetMethod = ctx.parseString(props.get("programmer.reset"));

            int progbaud = ctx.getParsedInteger("upload.speed", 115200);
            int predelay = ctx.getParsedInteger("programmer.reset.predelay", 100);
            int delay = ctx.getParsedInteger("programmer.reset.delay", 100);
            int postdelay = ctx.getParsedInteger("programmer.reset.postdelay", 100);

            if (resetMethod.equals("baud")) {
                int baud = 1200;
                try {
                    String b = ctx.parseString(props.get("programmer.reset.baud"));
                    baud = Integer.parseInt(b);
                } catch (Exception e) {
                    Base.exception(e);
                }
                if (!performBaudBasedReset(ctx, baud, predelay, delay, postdelay)) {
                    return false;
                }
            } else if (resetMethod.equals("dtr")) {
                boolean dtr = props.getBoolean("programmer.reset.dtr");
                boolean rts = props.getBoolean("programmer.reset.rts");
                if (!performSerialReset(ctx, dtr, rts, progbaud, predelay, delay, postdelay)) {
                    return false;
                }
            }

            if (!Base.isQuiet()) ctx.bullet("Uploading...");

            if (!Preferences.getBoolean("compiler.verbose_upload")) {
                if (props.get("programmer.quiet") != null) {
                    ctx.set("verbose", ctx.parseString(props.get("programmer.quiet")));
                } else {
                    ctx.set("verbose", "");
                }
            } else {
                if (props.get("programmer.verbose") != null) {
                    ctx.set("verbose", ctx.parseString(props.get("programmer.verbose")));
                } else {
                    ctx.set("verbose", "");
                }
            }


            boolean res = (Boolean)ctx.executeKey("programmer.command");


            if(res) {
                if (!Base.isQuiet()) ctx.bullet("Upload Complete");
                return true;
            } else {
                ctx.error("Upload Failed");
                return false;
            }
        }
        return false;
    }

    public boolean performSerialReset(Context ctx, boolean dtr, boolean rts, int speed, int predelay, int delay, int postdelay) {
        if (!Base.isQuiet()) ctx.bullet("Resetting board.");
        try {
            CommunicationPort port = ctx.getDevice();
            if (port instanceof SerialCommunicationPort) {
                SerialCommunicationPort sport = (SerialCommunicationPort)port;
                if (!sport.openPort()) {
                    ctx.error("Error: " + sport.getLastError());
                    return false;
                }
                sport.setDTR(false);
                sport.setRTS(false);
                Thread.sleep(predelay);
                sport.setDTR(dtr);
                sport.setRTS(rts);
                Thread.sleep(delay);
                sport.setDTR(false);
                sport.setRTS(false);
                sport.closePort();
//                System.gc();
                Thread.sleep(postdelay);
            }
        } catch (Exception e) {
            Base.exception(e);
            ctx.error(e);
            return false;
        }
        return true;
    }

    public boolean performBaudBasedReset(Context ctx, int b, int predelay, int delay, int postdelay) {
        if (!Base.isQuiet()) ctx.bullet("Resetting board.");
        try {
            CommunicationPort port = ctx.getDevice();
            if (port instanceof SerialCommunicationPort) {
                SerialCommunicationPort sport = (SerialCommunicationPort)port;
                if (!sport.openPort()) {
                    ctx.error("Error: " + sport.getLastError());
                    return false;
                }
                sport.setDTR(false);
                sport.setRTS(false);
                Thread.sleep(predelay);
                sport.setDTR(true);
                sport.setRTS(true);
                if (!sport.setSpeed(b)) {
                    ctx.error("Error: " + sport.getLastError());
                }
                Thread.sleep(delay);
                sport.setDTR(false);
                sport.setRTS(false);
                sport.closePort();
                Thread.sleep(postdelay);
//                System.gc();
            }
        } catch (Exception e) {
            Base.exception(e);
            ctx.error(e);
            return false;
        }
        return true;
    }

    

}
