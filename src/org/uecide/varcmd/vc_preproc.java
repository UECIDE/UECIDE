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

package org.uecide.varcmd;

import org.uecide.*;
import java.io.*;

public class vc_preproc implements VariableCommand {

    public String main(Context ctx, String args) {
        ctx.snapshot();
        String file = args;
        String[] bits = args.split(",");

        if (bits.length > 1) {
            file = bits[0];
            for (int i = 1; i < bits.length; i++) {
                String[] portion = bits[i].split("=");
                ctx.set(portion[0], portion[1]);
            }
        }

        File infile = new File(file);
        if (!infile.exists()) {
            ctx.rollback();
            return "FILE NOT FOUND";
        }

        String data = Base.getFileAsString(infile);
        data = ctx.parseString(data);

        String extension = null;
        String[] fbits = infile.getName().split(".");
        if (fbits.length > 1) {
            extension = "." + fbits[fbits.length-1];
        }

        File tempfile = null;

        try {
            tempfile = File.createTempFile("uecide-preproc-", extension);
            tempfile.deleteOnExit();

            PrintWriter pw = new PrintWriter(tempfile);
            pw.print(data);
            pw.close();
        } catch (Exception e) {
        }

        ctx.rollback();
        if (tempfile != null) {
            return tempfile.getAbsolutePath();
        } else {
            return "UNABLE TO CREATE TEMPFILE";
        }
    }
}
