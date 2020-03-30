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

package org.uecide.builtin;

import org.uecide.*;
import javax.script.*;
import java.io.*;

public class ecma implements BuiltinCommand {
    PipedReader stdo_pr;
    PipedWriter stdo_pw;
    PipedReader stde_pr;
    PipedWriter stde_pw;
    boolean running = true;
    Context ctx;
    public boolean main(Context c, String[] arg) {
        ctx = c;
        if (arg.length < 1) {
            ctx.error("Usage: __builtin_ecma::filename.js[::arg::arg...]");
            return false;
        }

        try {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("JavaScript");
            File f = new File(arg[0]);
            if (!f.exists()) {
                ctx.error(Base.i18n.string("err.notfound", arg[0]));
                return false;
            }
            StringBuilder sb = new StringBuilder();

            String[] args = new String[arg.length - 1];

            for (int i = 0; i < args.length; i++) {
                args[i] = arg[i+1];
            }

            FileInputStream fis = new FileInputStream(f);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));

            String line = null;
            while((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }

            br.close();
            
            engine.eval(sb.toString());

            engine.put("ctx", ctx);
            running = true;

            try {
                stdo_pr = new PipedReader();
                stdo_pw = new PipedWriter(stdo_pr);
                stde_pr = new PipedReader();
                stde_pw = new PipedWriter(stde_pr);
            } catch (Exception ex) {
                Base.exception(ex);
            }

            engine.getContext().setWriter(stdo_pw);
            engine.getContext().setErrorWriter(stde_pw);

            Thread stdout = new Thread() {
                public void run() {
                    while (running) {
                        char[] tmp = new char[30];
                        try {
                            while (stdo_pr.ready()) {
                                int i = stdo_pr.read(tmp, 0, 20);
                                ctx.messageStream(new String(tmp, 0, i));
                            }
                        } catch (Exception ex) {
                            Base.exception(ex);
                        }
                    }
                }
            };
            stdout.start();

            Thread stderr = new Thread() {
                public void run() {
                    while (running) {
                        char[] tmp = new char[30];
                        try {
                            while (stde_pr.ready()) {
                                int i = stde_pr.read(tmp, 0, 20);
                                ctx.messageStream("+" + new String(tmp, 0, i));
                            }
                        } catch (Exception ex) {
                            Base.exception(ex);
                        }
                    }
                }
            };
            stderr.start();


            Invocable inv = (Invocable)engine;
            Object o = inv.invokeFunction("run", (Object[])args);
            Boolean ret = false;
            if (o instanceof Boolean) {
                ret = (Boolean)o;
            } else {
                System.err.println(o);
            }
            stdo_pw.flush();
            stde_pw.flush();
            running = false;
            return ret;
        } catch (Exception e) {
            Base.exception(e);
            ctx.error("Exception...");
            ctx.error(e);
        }
        return false;
    }

    public void kill() {
        running = false;
    }

}
