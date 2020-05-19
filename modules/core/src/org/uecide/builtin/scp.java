package org.uecide.builtin;

import org.uecide.*;
import com.jcraft.jsch.*;
import java.awt.*;
import javax.swing.*;
import java.io.*;

/* Copy a file to a remote host with scp
 *
 * Usage:
 *     __builtin_scp::localfile::user@host:/path/to/remote/file
 */

public class scp extends BuiltinCommand {
    String host;
    String user;

    public scp(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {
        if(arg.length != 2) {
            throw new BuiltinCommandException("Syntax Error");
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
                password = UECIDE.session.get("ssh." + host + "." + user);
            }

            if(password == null) {
                password = askPassword();

                if(password == null) {
                    throw new BuiltinCommandException("No Password Given");
                }
            }

            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");

            try {
                session.connect();
            } catch(JSchException e) {
                Debug.exception(e);
                if(e.getMessage().equals("Auth fail")) {
                    password = null;
                    Preferences.unset("ssh." + host + "." + user);
                    UECIDE.session.unset("ssh." + host + "." + user);
                    session.disconnect();
                    throw new BuiltinCommandException("Authentication Failed");
                } else {
                    throw new BuiltinCommandException(e.getMessage());
                }
            } catch(Exception e) {
                Debug.exception(e);
                throw new BuiltinCommandException(e.getMessage());
            }

            UECIDE.session.set("ssh." + host + "." + user, password);


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
                session.disconnect();
                throw new BuiltinCommandException("Error Connecting SSH Channel");
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
                    session.disconnect();
                    throw new BuiltinCommandException("Timestamp Error");
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
                session.disconnect();
                throw new BuiltinCommandException("Error Connecting SSH Channel");
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
                Debug.exception(e);
                ctx.error(UECIDE.i18n.string("err.ssh.copy", e.getMessage()));
                session.disconnect();

                try {
                    if(fis != null)fis.close();
                } catch(Exception ee) {
                    Debug.exception(ee);
                }

                return false;
            }

// send '\0'
            buf[0] = 0;
            out.write(buf, 0, 1);
            out.flush();

            if(checkAck(in) != 0) {
                ctx.error(UECIDE.i18n.string("err.ssh.flush"));
                session.disconnect();
                return false;
            }

            out.close();

            channel.disconnect();
            session.disconnect();

            return true;
        } catch(Exception e) {
            Debug.exception(e);
            if(session != null) {
                session.disconnect();
            }

            ctx.error(UECIDE.i18n.string("err.ssh.copy", e.getMessage()));

            System.out.println(e);

            try {
                if(fis != null)fis.close();
            } catch(Exception ee) {
                Debug.exception(ee);
            }
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
            StringBuilder sb = new StringBuilder();
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
        JCheckBox save = new JCheckBox(UECIDE.i18n.string("form.ssh.rempass"));
        Object[] ob = {passwordField, save};
        int result = JOptionPane.showConfirmDialog(
            null, 
            ob, 
            UECIDE.i18n.string("form.ssh.askpass", user, host), 
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
