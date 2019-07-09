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

import java.io.*;
import java.net.*;
import java.util.regex.*;
import java.util.*;
import com.jcraft.jsch.*;
import java.awt.*;
import javax.swing.*;
import org.usb4java.*;
import javax.usb.*;
import javax.usb.event.*;


public class UsbHidDevice implements CommunicationPort {

    Board board = null;
    UsbDevice dev = null;

    String name;

    public UsbHidDevice(Board b, UsbDevice d) {
        board = b;
        dev = d;
    }

    public String getConsoleAddress() {
        return null;
    }

    public String getConsolePort() {
        return null;
    }

    public String getProgrammingAddress() {
        UsbDeviceDescriptor desc = dev.getUsbDeviceDescriptor();
        short vid = desc.idVendor();
        short pid = desc.idProduct();
        return String.format("%04x:%04x", vid, pid);
    }

    public String getProgrammingPort() {
        UsbDeviceDescriptor desc = dev.getUsbDeviceDescriptor();
        short vid = desc.idVendor();
        short pid = desc.idProduct();
        return String.format("%04x:%04x", vid, pid);
    }

    public Board getBoard() {
        return board;
    }

    public String getName() {
        return board.getDescription() + " (" + getProgrammingPort() + ")";
    }

    public String toString() {
        return getName();
    }

    public void closePort() {
    }

    public boolean openPort() {
        return false;
    }

    public String getLastError() {
        return null;
    }

    public boolean setSpeed(int speed) {
        return true;
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
        UsbDeviceDescriptor desc = dev.getUsbDeviceDescriptor();
        short vid = desc.idVendor();
        short pid = desc.idProduct();
        return String.format("%04x:%04x", vid, pid);
    }

    public void removeCommsListener() { }
    public void addCommsListener(CommsListener l) { }
    public boolean print(String data) { return false; }
    public boolean println(String data) { return false; }
    public boolean write(byte[] data) { return false; }
    public boolean write(byte data) { return false; }

    public boolean exists() {   
        return true;
    }

}
