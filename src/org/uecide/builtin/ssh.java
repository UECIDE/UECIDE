package org.uecide.builtin;

import org.uecide.*;
import com.jcraft.jsch.*;
import java.awt.*;
import javax.swing.*;
import java.io.*;

/* Execute a command on a remote host through SSH
 *
 * Usage:
 *     __builtin_ssh::user@host::command
 */

public class ssh  extends BuiltinCommand {
    String host;
    String user;

    public ssh(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {
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
                password = UECIDE.session.get("ssh." + host + "." + user);
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
                Debug.exception(e);
                if(e.getMessage().equals("Auth fail")) {
                    password = null;
                    Preferences.unset("ssh." + host + "." + user);
                    UECIDE.session.unset("ssh." + host + "." + user);
                    ctx.error(UECIDE.i18n.string("err.ssh.auth"));
                    session.disconnect();
                    return false;
                } else {
                    ctx.error(e);
                    return false;
                }
            } catch(Exception e) {
                Debug.exception(e);
                ctx.error(e);
                return false;
            }

            UECIDE.session.set("ssh." + host + "." + user, password);


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
                } catch(Exception ee) {
                    Debug.exception(ee);
                }
            }

            channel.disconnect();
            session.disconnect();
        } catch(Exception e) {
            Debug.exception(e);
            ctx.error(e);
        }

        return true;
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
