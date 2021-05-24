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

public abstract class BuiltinCommand {
        public abstract boolean main(Context ctx, String[] arg) throws BuiltinCommandException;
        public abstract void kill();

        public static boolean run(Context c, String cmdName, String[] arg) {
                BuiltinCommand cmd = null;

                switch(cmdName) {
                        case "append":          cmd = new append(); break;
                        case "append_var":      cmd = new append_var(); break;
                        case "bullet2":         cmd = new bullet2(); break;
                        case "bullet3":         cmd = new bullet3(); break;
                        case "bullet":          cmd = new bullet(); break;
                        case "cout":            cmd = new cout(); break;
                        case "cp":              cmd = new cp(); break;
                        case "delay":           cmd = new delay(); break;
                        case "deltree":         cmd = new deltree(); break;
                        case "dot":             cmd = new dot(); break;
                        case "echo":            cmd = new echo(); break;
                        case "ecma":            cmd = new ecma(); break;
                        case "end_buffer":      cmd = new end_buffer(); break;
                        case "error":           cmd = new error(); break;
                        case "exec":            cmd = new exec(); break;
                        case "export":          cmd = new export(); break;
                        case "fetch":           cmd = new fetch(); break;
                        case "foreach":         cmd = new foreach(); break;
                        case "getauth":         cmd = new getauth(); break;
                        case "gpio":            cmd = new gpio(); break;
                        case "loadjar":         cmd = new loadjar(); break;
                        case "lock_port":       cmd = new lock_port(); break;
                        case "merge_hex":       cmd = new merge_hex(); break;
                        case "port":            cmd = new port(); break;
                        case "push":            cmd = new push(); break;
                        case "read_var":        cmd = new read_var(); break;
                        case "refresh":         cmd = new refresh(); break;
                        case "scp":             cmd = new scp(); break;
                        case "set":             cmd = new set(); break;
                        case "spin":            cmd = new spin(); break;
                        case "ssh":             cmd = new ssh(); break;
                        case "start_buffer":    cmd = new start_buffer(); break;
                        case "stdin":           cmd = new stdin(); break;
                        case "stk500v1":        cmd = new stk500v1(); break;
                        case "stk500v2":        cmd = new stk500v2(); break;
                        case "tmpfile":         cmd = new tmpfile(); break;
                        case "unfetch":         cmd = new unfetch(); break;
                        case "unlock_port":     cmd = new unlock_port(); break;
                        case "warning":         cmd = new warning(); break;
                        case "write":           cmd = new write(); break;
                        case "write_var":       cmd = new write_var(); break;
                        default:
                                c.error("Unknown builtin command: " + cmdName);
                                return false;
                }

                try {
                        return cmd.main(c, arg);
                } catch (BuiltinCommandException ex) {
                        c.error(ex.getMessage());
                        return false;
                } catch (Exception ex) {
                        c.error(ex);
                        return false;
                }
        }
}
