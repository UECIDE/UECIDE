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

public abstract class VariableCommand {
    public abstract String main(Context context, String args) throws VariableCommandException;

    public static String run(Context ctx, String command, String param) {
        try {
            VariableCommand vc = null;


            if (command.equals("arduino")) vc = new vc_arduino();
            else if (command.equals("basename")) vc = new vc_basename();
            else if (command.equals("board")) vc = new vc_board();
            else if (command.equals("char")) vc = new vc_char();
            else if (command.equals("compiler")) vc = new vc_compiler();
            else if (command.equals("core")) vc = new vc_core();
            else if (command.equals("env")) vc = new vc_env();
            else if (command.equals("exec")) vc = new vc_exec();
            else if (command.equals("files")) vc = new vc_files();
            else if (command.equals("find")) vc = new vc_find();
            else if (command.equals("foreach")) vc = new vc_foreach();
            else if (command.equals("git")) vc = new vc_git();
            else if (command.equals("if")) vc = new vc_if();
            else if (command.equals("java")) vc = new vc_java();
            else if (command.equals("join")) vc = new vc_join();
            else if (command.equals("math")) vc = new vc_math();
            else if (command.equals("onefile")) vc = new vc_onefile();
            else if (command.equals("option")) vc = new vc_option();
            else if (command.equals("os")) vc = new vc_os();
            else if (command.equals("port")) vc = new vc_port();
            else if (command.equals("prefs")) vc = new vc_prefs();
            else if (command.equals("preproc")) vc = new vc_preproc();
            else if (command.equals("programmer")) vc = new vc_programmer();
            else if (command.equals("random")) vc = new vc_random();
            else if (command.equals("replace")) vc = new vc_replace();
            else if (command.equals("select")) vc = new vc_select();
            else if (command.equals("sketch")) vc = new vc_sketch();
            else if (command.equals("system")) vc = new vc_system();
            else if (command.equals("tool")) vc = new vc_tool();
            else if (command.equals("uecide")) vc = new vc_uecide();
            else {
                return "BADVCMD";
            }

            String ret = vc.main(ctx, param);
            return ret;
        } catch (VariableCommandException ex) {
            Base.error(ex);
        } catch (Exception ex) {
            Base.error(ex);
        }

        return "";
    }

}
