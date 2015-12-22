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
import com.jcraft.jsch.*;
import java.awt.*;
import javax.swing.*;
import java.io.*;

public class scp implements BuiltinCommand {
    String host;
    String user;

    Context ctx;

    public boolean main(Context c, String[] arg) {
        ctx = c;
        if(arg.length != 2) {
            System.err.println("usage: __builtin_scp file1 user@remotehost:file2");
            return false;
        }

        Session session = null;

        FileInputStream fis = null;

        try {

            String lfile = arg[0];
            user = arg[1].substring(0, arg[1].indexOf('@'));
            arg[1] = arg[1].substring(arg[1].indexOf('@') + 1);
            host = arg[1].substring(0, arg[1].indexOf(':'));
            String rfile = arg[1].substring(arg[1].indexOf(':') + 1);

            JSch jsch = new JSch();
            session = jsch.getSession(user, host, 22);

            String password = Preferences.get("ssh." + host + "." + user);

            if(password == null) {
                password = Base.session.get("ssh." + host + "." + user);
            }

            if(password == null) {
                password = askPassword();

                if(password == null) {
                    ctx.error("Unable to log in without a password");
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
                    ctx.error("Authentication failed");
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


            boolean ptimestamp = true;

// exec 'scp -t rfile' remotely
            String command = "scp " + (ptimestamp ? "-p" : "") + " -t " + rfile;
            Channel channel = session.openChannel("exec");
            ((ChannelExec)channel).setCommand(command);

// get I/O streams for remote scp
            OutputStream out = channel.getOutputStream();
            InputStream in = channel.getInputStream();

            channel.connect();

            if(checkAck(in) != 0) {
                ctx.error("Channel open failed");
                session.disconnect();
                return false;
            }

            File _lfile = new File(lfile);

            if(ptimestamp) {
                command = "T" + (_lfile.lastModified() / 1000) + " 0";
// The access time should be sent here,
// but it is not accessible with JavaAPI ;-<
                command += (" " + (_lfile.lastModified() / 1000) + " 0\n");
                out.write(command.getBytes());
                out.flush();

                if(checkAck(in) != 0) {
                    ctx.error("Timestamp failed");
                    session.disconnect();
                    return false;
                }
            }

// send "C0644 filesize filename", where filename should not include '/'
            long filesize = _lfile.length();
            command = "C0644 " + filesize + " ";

            if(lfile.lastIndexOf('/') > 0) {
                command += lfile.substring(lfile.lastIndexOf('/') + 1);
            } else {
                command += lfile;
            }

            command += "\n";
            out.write(command.getBytes());
            out.flush();

            if(checkAck(in) != 0) {
                ctx.error("Remote open failed");
                session.disconnect();
                return false;
            }

// send a content of lfile
            byte[] buf = new byte[1024];
            fis = new FileInputStream(lfile);

            try {
                while(true) {
                    int len = fis.read(buf, 0, buf.length);

                    if(len <= 0) break;

                    out.write(buf, 0, len); //out.flush();
                }

                fis.close();
                fis = null;
            } catch(Exception e) {
                ctx.error("Copy failed: " + e.getMessage());
                session.disconnect();

                try {
                    if(fis != null)fis.close();
                } catch(Exception ee) {}

                return false;
            }

// send '\0'
            buf[0] = 0;
            out.write(buf, 0, 1);
            out.flush();

            if(checkAck(in) != 0) {
                ctx.error("Flush failed");
                session.disconnect();
                return false;
            }

            out.close();

            channel.disconnect();
            session.disconnect();

            return true;
        } catch(Exception e) {
            if(session != null) {
                session.disconnect();
            }

            ctx.error("Copy failed: " + e.getMessage());

            System.out.println(e);

            try {
                if(fis != null)fis.close();
            } catch(Exception ee) {}
        }

        return false;
    }

    int checkAck(InputStream in) throws IOException {
        int b = in.read();

// b may be 0 for success,
// 1 for error,
// 2 for fatal error,
// -1
        if(b == 0) return b;

        if(b == -1) return b;

        if(b == 1 || b == 2) {
            StringBuffer sb = new StringBuffer();
            int c;

            do {
                c = in.read();
                sb.append((char)c);
            } while(c != '\n');

            if(b == 1) { // error
                System.out.print(sb.toString());
            }

            if(b == 2) { // fatal error
                System.out.print(sb.toString());
            }
        }

        return b;
    }

    public String askPassword() {
        JTextField passwordField = (JTextField)new JPasswordField(20);
        JCheckBox save = new JCheckBox("Remember password");
        Object[] ob = {passwordField, save};
        int result = JOptionPane.showConfirmDialog(null, ob, "Enter password for " + user + "@" + host, JOptionPane.OK_CANCEL_OPTION);

        if(result == JOptionPane.CANCEL_OPTION) {
            return null;
        }

        if(save.isSelected()) {
            Preferences.set("ssh." + host + "." + user, passwordField.getText());
        }

        return passwordField.getText();
    }

}
