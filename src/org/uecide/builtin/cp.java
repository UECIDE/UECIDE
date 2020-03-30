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
import java.io.*;
import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.*;


public class cp extends BuiltinCommand {
    Context ctx;

    public boolean main(Context c, String[] arg) throws BuiltinCommandException {
        ctx = c;

        if (arg.length < 2) {
            throw new BuiltinCommandException("Syntax Error");
        }

        if (arg.length > 2) {
            return copy_many_to_one(arg);
        } else {
            return copy_one_to_one(arg);
        }
    }

    public boolean copy_one_to_one(String[] arg) throws BuiltinCommandException {
        File from = new File(arg[0]);
        File to = new File(arg[1]);
        if (to.exists() && to.isDirectory()) {
            to = new File(to, from.getName());
        }

        try {
            Files.copy(from.toPath(), to.toPath(), REPLACE_EXISTING);
        } catch (IOException e) {
            Debug.exception(e);
            throw new BuiltinCommandException(e.getMessage());
        }
        return true;
    }

    public boolean copy_many_to_one(String[] arg) throws BuiltinCommandException {
        File to = new File(arg[arg.length - 1]);
        if (!to.exists() || !to.isDirectory()) {
            return false;
        }
        for (int i = 0; i < arg.length - 2; i++) {
            String[] files = {
                arg[i],
                to.getAbsolutePath()
            };
            if (copy_one_to_one(files) == false) {
                return false;
            }
        }
        return true;
    }

    public void kill() {
    }
}
