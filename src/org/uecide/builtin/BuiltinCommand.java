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

import org.uecide.UECIDE;
import org.uecide.Context;
import org.uecide.Debug;

public abstract class BuiltinCommand {
    public abstract boolean main(Context ctx, String[] arg) throws BuiltinCommandException ;
    public abstract void kill();

    public static boolean run(Context ctx, String cmdName, String[] arg) {
        BuiltinCommand cmd = null;

        if (cmdName.equals("append")) cmd = new append();
        else if (cmdName.equals("bullet")) cmd = new bullet();
        else if (cmdName.equals("bullet2")) cmd = new bullet2();
        else if (cmdName.equals("bullet3")) cmd = new bullet3();
        else if (cmdName.equals("cout")) cmd = new cout();
        else if (cmdName.equals("cp")) cmd = new cp();
        else if (cmdName.equals("delay")) cmd = new delay();
        else if (cmdName.equals("dot")) cmd = new dot();
        else if (cmdName.equals("echo")) cmd = new echo();
        else if (cmdName.equals("end_buffer")) cmd = new end_buffer();
        else if (cmdName.equals("error")) cmd = new error();
        else if (cmdName.equals("exec")) cmd = new exec();
        else if (cmdName.equals("foreach")) cmd = new foreach();
        else if (cmdName.equals("getauth")) cmd = new getauth();
        else if (cmdName.equals("gpio")) cmd = new gpio();
        else if (cmdName.equals("lock_port")) cmd = new lock_port();
        else if (cmdName.equals("merge_hex")) cmd = new merge_hex();
        else if (cmdName.equals("port")) cmd = new port();
        else if (cmdName.equals("push")) cmd = new push();
        else if (cmdName.equals("scp")) cmd = new scp();
        else if (cmdName.equals("set")) cmd = new set();
        else if (cmdName.equals("spin")) cmd = new spin();
        else if (cmdName.equals("ssh")) cmd = new ssh();
        else if (cmdName.equals("start_buffer")) cmd = new start_buffer();
        else if (cmdName.equals("stdin")) cmd = new stdin();
        else if (cmdName.equals("stk500v1")) cmd = new stk500v1();
        else if (cmdName.equals("stk500v2")) cmd = new stk500v2();
        else if (cmdName.equals("unlock_port")) cmd = new unlock_port();
        else if (cmdName.equals("warning")) cmd = new warning();
        else if (cmdName.equals("write")) cmd = new write();
        else if (cmdName.equals("askyesno")) cmd = new askyesno();
        else if (cmdName.equals("askyesnocancel")) cmd = new askyesno();
        else if (cmdName.equals("alert")) cmd = new alert();
        else if (cmdName.equals("input")) cmd = new input();
        else if (cmdName.equals("action")) cmd = new action();
        else if (cmdName.equals("exists")) cmd = new exists();
        else if (cmdName.equals("isset")) cmd = new isset();
        else {
            ctx.error("Unknown builtin command " + cmdName);
            return false;
        }

        try {
            return cmd.main(ctx, arg);
        } catch (BuiltinCommandException ex) {
            Debug.exception(ex);
            UECIDE.error(ex);
            return false;
        }

    }
}
