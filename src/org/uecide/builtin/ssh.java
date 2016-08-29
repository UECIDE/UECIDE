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

/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/**
* This program will demonstrate remote exec.
* $ CLASSPATH=.:../build javac Exec.java
* $ CLASSPATH=.:../build java Exec
* You will be asked username, hostname, displayname, passwd and command.
* If everything works fine, given command will be invoked
* on the remote side and outputs will be printed out.
*
*/
package org.uecide.builtin;

import org.uecide.*;
import com.jcraft.jsch.*;
import java.awt.*;
import javax.swing.*;
import java.io.*;

public class ssh  implements BuiltinCommand {
    String host;
    String user;

    public boolean main(Context ctx, String[] arg) {
        try {
            JSch jsch = new JSch();

            if(arg.length != 2) {
                ctx.error("Usage: __builtin_ssh user@host command");
                return false;
            }

            host = arg[0];

            user = host.substring(0, host.indexOf('@'));
            host = host.substring(host.indexOf('@') + 1);

            Session session = jsch.getSession(user, host, 22);

            String password = Preferences.get("ssh." + host + "." + user);

            if(password == null) {
                password = Base.session.get("ssh." + host + "." + user);
            }

            if(password == null) {
                password = askPassword();

                if(password == null) {
                    return false;
                }
            }

            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");

            try {
                session.connect();
            } catch(JSchException e) {
                if(e.getMessage().equals("Auth fail")) {
                    password = null;
                    Preferences.unset("ssh." + host + "." + user);
                    Base.session.unset("ssh." + host + "." + user);
                    ctx.error(Base.i18n.string("err.ssh.auth"));
                    session.disconnect();
                    return false;
                } else {
                    ctx.error(e);
                    return false;
                }
            } catch(Exception e) {
                ctx.error(e);
                return false;
            }

            Base.session.set("ssh." + host + "." + user, password);


            String command = arg[1];

            Channel channel = session.openChannel("exec");
            ((ChannelExec)channel).setCommand(command);

            channel.setInputStream(null);

            InputStream in = channel.getInputStream();
            InputStream err = ((ChannelExec)channel).getErrStream();

            channel.connect();

            byte[] tmp = new byte[1024];

            while(true) {
                while(in.available() > 0) {
                    int i = in.read(tmp, 0, 20);

                    if(i < 0)break;

                    ctx.messageStream(new String(tmp, 0, i));
                }

                while(err.available() > 0) {
                    int i = err.read(tmp, 0, 20);

                    if(i < 0)break;

                    ctx.errorStream(new String(tmp, 0, i));
                }

                if(channel.isClosed()) {
                    if(in.available() > 0) continue;

                    break;
                }

                try {
                    Thread.sleep(1000);
                } catch(Exception ee) {}
            }

            channel.disconnect();
            session.disconnect();
        } catch(Exception e) {
            ctx.error(e);
        }

        return true;
    }

    public String askPassword() {
        JTextField passwordField = (JTextField)new JPasswordField(20);
        JCheckBox save = new JCheckBox(Base.i18n.string("form.ssh.rempass"));
        Object[] ob = {passwordField, save};
        int result = JOptionPane.showConfirmDialog(
            null, 
            ob, 
            Base.i18n.string("form.ssh.askpass", user, host),
            JOptionPane.OK_CANCEL_OPTION
        );

        if(result == JOptionPane.CANCEL_OPTION) {
            return null;
        }

        if(save.isSelected()) {
            Preferences.set("ssh." + host + "." + user, passwordField.getText());
        }

        return passwordField.getText();
    }

    public void kill() {
    }
}
