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

import org.uecide.Context;
import org.uecide.Core;
import org.uecide.Base;

public class vc_core extends VariableCommand {
    public String main(Context sketch, String args) throws VariableCommandException {

        String[] alist = args.split(",");
        Core core = sketch.getCore();
        String prop = "";

        if (alist.length > 2) {
            throw new VariableCommandException("Syntax Error");
        }

        if (alist.length == 2) {
            core = Base.cores.get(alist[0]);
            if (core == null) {
                throw new VariableCommandException("Core not found: " + alist[0]);
            }
            prop = alist[1];
        } else {
            prop = alist[0];
        }

        if (prop.equals("root")) {
            return core.getFolder().getAbsolutePath();
        }
        return core.get(prop);
    }
}
