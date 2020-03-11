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

package org.uecide;

//import java.io.*;
//import java.net.*;
//import java.util.regex.*;
//import java.util.*;
//import com.jcraft.jsch.*;
//import java.awt.*;
//import javax.swing.*;

import java.util.HashMap;
import java.util.Arrays;
import java.net.InetAddress;
import java.io.InputStream;
import java.io.OutputStream;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

// These are for the password entry and must be moved to the Gui.
import javax.swing.JOptionPane;
import javax.swing.JCheckBox;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class SSHCommunicationPort implements CommunicationPort {

    InetAddress address = null;
    int port = 22;
    Board board = null;
    String lastError = "No error";
    CommsListener listener = null;
    String consoleCommand = null;
    String password = null;

    Session session = null;
    Channel channel = null;

    OutputStream stdin = null;
    InputStream stdout = null;
    InputStream stderr = null;

    ReadThread readThread = null;

    String name;

    public SSHCommunicationPort(String n, Board b, InetAddress ip, int portnum) {
        board = b;
        name = n;
        address = ip;
        port = portnum;
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
        return "" + port;
    }

    public Board getBoard() {
        return board;
    }

    public String getName() {
        return name + " (" + getProgrammingAddress() + ")";
    }

    public String toString() {
        return getName();
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
            JSch jsch = new JSch();
            String hostname = getProgrammingAddress();
            session = jsch.getSession(get("user"), hostname, port);
            String password = Preferences.get("ssh." + hostname + "." + get("user"));
            if(password == null) {
                password = UECIDE.session.get("ssh." + hostname + "." + get("user"));
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
                    Preferences.unset("ssh." + hostname + "." + get("user"));
                    UECIDE.session.unset("ssh." + hostname + "." + get("user"));
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

            UECIDE.session.set("ssh." + hostname + "." + get("user"), password);

            String command = board.get("ssh.console.command");
            channel = session.openChannel("exec");
            ((ChannelExec)channel).setCommand(command);

            stdout = channel.getInputStream();
            stderr = ((ChannelExec)channel).getErrStream();
            stdin = channel.getOutputStream();

            readThread = new ReadThread();

            readThread.start();

            channel.connect();
        } catch (Exception ex) {
            lastError = ex.getMessage();
            ex.printStackTrace();
            System.err.println(lastError);
            return false;
        }
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
        String hostname = getProgrammingAddress();
        int result = JOptionPane.showConfirmDialog(null, ob, "Enter password for " + get("user") + "@" + hostname, JOptionPane.OK_CANCEL_OPTION);

        if(result == JOptionPane.CANCEL_OPTION) {
            return null;
        }

        if(save.isSelected()) {
            Preferences.set("ssh." + hostname + "." + get("user"), passwordField.getText());
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

    HashMap<String, String> data = new HashMap<String, String>();
    public void set(String key, String value) {
        data.put(key, value);
    }
    public String get(String key) {
        return data.get(key);
    }

    public String getKey() {
        return name;
    }

    public boolean exists() { 
        return true;
    }

}
