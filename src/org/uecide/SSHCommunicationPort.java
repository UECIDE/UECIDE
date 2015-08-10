package org.uecide;

import jssc.*;
import java.io.*;
import java.net.*;
import java.util.regex.*;
import java.util.*;
import com.jcraft.jsch.*;
import java.awt.*;
import javax.swing.*;

public class SSHCommunicationPort implements CommunicationPort {

    InetAddress address = null;
    int port = 22;
    Board board = null;
    String lastError = "No error";
    CommsListener listener = null;
    String consoleCommand = null;
    String username = null;
    String password = null;
    String hostname = null;
    String portname = null;

    Session session = null;
    Channel channel = null;

    OutputStream stdin = null;
    InputStream stdout = null;
    InputStream stderr = null;

    ReadThread readThread = null;

    public SSHCommunicationPort(String n, Board b) {
        board = b;
        String unPart = "";
        String hostPart = "";

        if (n.startsWith("ssh://")) {
            n = n.substring(6);
        };
        if (n.indexOf("@") > -1) {
            String[] parts = n.split("@");
            unPart = parts[0];
            hostPart = parts[1];
        } else {
            hostPart = n;
        }

        String[] upbits = unPart.split(":");
        String[] hostbits = hostPart.split(":");

        username = upbits[0];
        if (upbits.length == 2) { password = upbits[1]; }
        hostname = hostbits[0];
        if (hostbits.length == 2) { portname = hostbits[1]; }
        if (portname == null) {
            portname = "22";
        }

        if (username == null || username.equals("")) {
            username = board.get("ssh.console.user");
        }

        try {
            address = InetAddress.getByName(hostname);
        } catch (Exception e) {
        }
    }

    public String getConsoleAddress() {
        return null;
    }

    public String getConsolePort() {
        return null;
    }

    public String getProgrammingAddress() {
        byte[] ip = address.getAddress();
        return String.format("%d.%d.%d.%d",
            (int)ip[0] & 0xFF,
            (int)ip[1] & 0xFF,
            (int)ip[2] & 0xFF,
            (int)ip[3] & 0xFF
        );
    }

    public String getProgrammingPort() {
        return portname;
    }

    public Board getBoard() {
        return board;
    }

    public String getName() {
        String out = board.getDescription() + " (ssh://";
        if (hostname != null) {
            out += hostname;
        }
        if (portname != null) {
            out += ":" + portname;
        }
        out += ")";
        return out;
    }

    public String toString() {
        String out = "ssh://";
        if (hostname != null) {
            out += hostname;
        }
        if (portname != null) {
            out += ":" + portname;
        }
        return out;
    }

    public void addCommsListener(CommsListener l) {
        listener = l;
    }

    public void removeCommsListener() {
        listener = null;
    }

    public boolean print(String s) {
        try {
            byte[] b = s.getBytes("UTF-8");
            return write(b);
        } catch (Exception e) {
            lastError = e.getMessage();
            return false;
        }
    }

    public boolean println(String s) {
        s += "\r\n";
        try {
            byte[] b = s.getBytes("UTF-8");
            return write(b);
        } catch (Exception e) {
            lastError = e.getMessage();
            return false;
        }
    }

    public boolean write(byte[] b) {
        try {
            stdin.write(b);
        } catch (Exception e) {
            lastError = e.getMessage();
            return false;
        }
        return true;
    }

    public boolean write(byte b) {
        try {
            stdin.write(b);
        } catch (Exception e) {
            lastError = e.getMessage();
            return false;
        }
        return true;
    }

    public void closePort() {
        try {
            readThread.finish();
            channel.disconnect();
            session.disconnect();
        } catch (Exception e) {
            lastError = e.getMessage();
        }
    }

    public boolean openPort() {
        try {
            System.err.println("Port: " + portname);
            port = Integer.parseInt(portname);
            System.err.println("Opening SSH connection to " + username + "@" + hostname + ":" + port);
            JSch jsch = new JSch();
            session = jsch.getSession(username, hostname, port);
            String password = Preferences.get("ssh." + hostname + "." + username);
            if(password == null) {
                password = Base.session.get("ssh." + hostname + "." + username);
            }

            if(password == null) {
                password = askPassword();

                if(password == null) {
                    return false;
                }
            }

            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");

            System.err.println("Trying to connect...");
            try {
                session.connect();
            } catch(JSchException e) {
                if(e.getMessage().equals("Auth fail")) {
                    password = null;
                    Preferences.unset("ssh." + hostname + "." + username);
                    Base.session.unset("ssh." + hostname + "." + username);
                    lastError = "Authentication failed";
                    System.err.println(lastError);
                    session.disconnect();
                    return false;
                } else {
                    lastError = e.getMessage();
                    System.err.println(lastError);
                    return false;
                }
            } catch(Exception e) {
                lastError = e.getMessage();
            e.printStackTrace();
                    System.err.println(lastError);
                return false;
            }

            System.err.println("Connected");
            Base.session.set("ssh." + hostname + "." + username, password);

            String command = board.get("ssh.console.command");
            Channel channel = session.openChannel("exec");
            ((ChannelExec)channel).setCommand(command);

            stdout = channel.getInputStream();
            stderr = ((ChannelExec)channel).getErrStream();
            stdin = channel.getOutputStream();

            readThread = new ReadThread();

            readThread.start();

            System.err.println("Connecting channel...");
            channel.connect();
        } catch (Exception ex) {
            lastError = ex.getMessage();
            ex.printStackTrace();
            System.err.println(lastError);
            return false;
        }
        System.err.println("All done");
        return true;
    }

    public String getLastError() {
        return lastError;
    }

    public boolean setSpeed(int speed) {
        return true;
    }

    public String askPassword() {
        JTextField passwordField = (JTextField)new JPasswordField(20);
        JCheckBox save = new JCheckBox("Remember password");
        Object[] ob = {passwordField, save};
        int result = JOptionPane.showConfirmDialog(null, ob, "Enter password for " + username + "@" + hostname, JOptionPane.OK_CANCEL_OPTION);

        if(result == JOptionPane.CANCEL_OPTION) {
            return null;
        }

        if(save.isSelected()) {
            Preferences.set("ssh." + hostname + "." + username, passwordField.getText());
        }

        return passwordField.getText();
    }

    class ReadThread extends Thread {
        boolean running = false;
        byte[] buffer = new byte[1024];
        public void run() {
            running = true;
            while (running) {
                try {
                    int nread = stdout.read(buffer);
                    if (listener != null) {
                        byte[] b = Arrays.copyOfRange(buffer, 0, nread);
                        listener.commsDataReceived(b);
                    }
                } catch (Exception e) {
                    lastError = e.getMessage();
                }
            }
        }

        public void finish() {
            running = false;
        }
    }


    public CommsSpeed[] getSpeeds() {
        CommsSpeed[] s = new CommsSpeed[1];
        s[0] = new CommsSpeed(0, "N/A");
        return s;
    }

    // How do you pulse an SSH line?
    public void pulseLine() {
    }

    public String getBaseName() {
        return toString();
    }
}
