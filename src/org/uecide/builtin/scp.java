package org.uecide.builtin;

import org.uecide.*;
import com.jcraft.jsch.*;
import java.awt.*;
import javax.swing.*;
import java.io.*;

public class scp implements BuiltinCommand {
    String host;
    String user;

    public boolean main(Sketch sketch, String[] arg) {
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

            String password = Base.preferences.get("ssh." + host + "." + user);

            if(password == null) {
                password = Base.session.get("ssh." + host + "." + user);
            }

            if(password == null) {
                password = askPassword();

                if(password == null) {
                    sketch.error("Unable to log in without a password");
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
                    Base.preferences.unset("ssh." + host + "." + user);
                    Base.session.unset("ssh." + host + "." + user);
                    sketch.error("Authentication failed");
                    session.disconnect();
                    return false;
                } else {
                    Base.error(e);
                    return false;
                }
            } catch(Exception e) {
                Base.error(e);
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
                sketch.error("Channel open failed");
                session.disconnect();
                return false;
            }

            File _lfile = new File(lfile);

            if(ptimestamp) {
                command = "T " + (_lfile.lastModified() / 1000) + " 0";
// The access time should be sent here,
// but it is not accessible with JavaAPI ;-<
                command += (" " + (_lfile.lastModified() / 1000) + " 0\n");
                out.write(command.getBytes());
                out.flush();

                if(checkAck(in) != 0) {
                    sketch.error("Timestamp failed");
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
                sketch.error("Remote open failed");
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
                sketch.error("Copy failed: " + e.getMessage());
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
                sketch.error("Flush failed");
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

            sketch.error("Copy failed: " + e.getMessage());

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
            Base.preferences.set("ssh." + host + "." + user, passwordField.getText());
            Base.preferences.saveDelay();
        }

        return passwordField.getText();
    }


}
