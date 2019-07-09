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
import java.util.*;
import java.io.*;

public class stk500v1 implements BuiltinCommand, CommsListener {

    boolean replyIsAvailable = false;

    String portName = null;
    CommunicationPort port;
    int baudRate = 115200;
    Context ctx;
    boolean connected = false;
    int timeout = 1000;

    String deviceName = null;

    public static final int Resp_STK_OK                 = 0x10;
    public static final int Resp_STK_FAILED             = 0x11;
    public static final int Resp_STK_UNKNOWN            = 0x12;
    public static final int Resp_STK_NODEVICE           = 0x13;
    public static final int Resp_STK_INSYNC             = 0x14;
    public static final int Resp_STK_NOSYNC             = 0x15;

    public static final int Resp_ADC_CHANNEL_ERROR      = 0x16;
    public static final int Resp_ADC_MEASURE_OK         = 0x17;
    public static final int Resp_PWM_CHANNEL_ERROR      = 0x18;
    public static final int Resp_PWM_ADJUST_OK          = 0x19;

    public static final int Sync_CRC_EOP                = 0x20;

    public static final int Cmnd_GET_SYNC               = 0x30;
    public static final int Cmnd_GET_SIGN_ON            = 0x31;

    public static final int Cmnd_SET_PARAMETER          = 0x40;
    public static final int Cmnd_GET_PARAMETER          = 0x41;
    public static final int Cmnd_SET_DEVICE             = 0x42;
    public static final int Cmnd_SET_DEVICE_EXT         = 0x45;
    
    public static final int Cmnd_STK_ENTER_PROGMODE     = 0x50;
    public static final int Cmnd_STK_LEAVE_PROGMODE     = 0x51;
    public static final int Cmnd_STK_CHIP_ERASE         = 0x52;
    public static final int Cmnd_STK_CHECK_AUTOINC      = 0x53;
    public static final int Cmnd_STK_LOAD_ADDRESS       = 0x55;
    public static final int Cmnd_STK_UNIVERSAL          = 0x56;
    public static final int Cmnd_STK_UNIVERSAL_MULTI    = 0x57;

    public static final int Cmnd_STK_PROG_FLASH         = 0x60;
    public static final int Cmnd_STK_PROG_DATA          = 0x61;
    public static final int Cmnd_STK_PROG_FUSE          = 0x62;
    public static final int Cmnd_STK_PROG_LOCK          = 0x63;
    public static final int Cmnd_STK_PROG_PAGE          = 0x64;
    public static final int Cmnd_STK_PROG_FUSE_EXT      = 0x65;
    
    public static final int Cmnd_STK_READ_FLASH         = 0x70;
    public static final int Cmnd_STK_READ_DATA          = 0x71;
    public static final int Cmnd_STK_READ_FUSE          = 0x72;
    public static final int Cmnd_STK_READ_LOCK          = 0x73;
    public static final int Cmnd_STK_READ_PAGE          = 0x74;
    public static final int Cmnd_STK_READ_SIGN          = 0x75;
    public static final int Cmnd_STK_READ_OSCCAL        = 0x76;
    public static final int Cmnd_STK_READ_FUSE_EXT      = 0x77;
    public static final int Cmnd_STK_READ_OSCCAL_EXT    = 0x78;

    public static final int Parm_STK_HW_VER             = 0x80;
    public static final int Parm_STK_SW_MAJOR           = 0x81;
    public static final int Parm_STK_SW_MINOR           = 0x82;
    public static final int Parm_STK_LEDS               = 0x83;
    public static final int Parm_STK_VTARGET            = 0x84;
    public static final int Parm_STK_VADJUST            = 0x85;
    public static final int Parm_STK_OSC_PSCALE         = 0x86;
    public static final int Parm_STK_OSC_CMATCH         = 0x87;
    public static final int Parm_STK_RESET_DURATION     = 0x88;
    public static final int Parm_STK_SCK_DURATION       = 0x89;

    public static final int Parm_STK_BUFSIZEL           = 0x90;
    public static final int Parm_STK_BUFSIZEH           = 0x91;
    public static final int Parm_STK_DEVICE             = 0x92;
    public static final int Parm_STK_PROGMODE           = 0x93;
    public static final int Parm_STK_PARAMODE           = 0x94;
    public static final int Parm_STK_POLLING            = 0x95;
    public static final int Parm_STK_SELFTIMED          = 0x96;
    public static final int Param_STK500_TOPCARD_DETECT = 0x98;

    public static final int Stat_STK_INSYNC             = 0x01;
    public static final int Stat_STK_PROGMODE           = 0x02;
    public static final int Stat_STK_STANDALONE         = 0x04;
    public static final int Stat_STK_RESET              = 0x08;
    public static final int Stat_STK_PROGRAM            = 0x10;
    public static final int Stat_STK_LEDG               = 0x20;
    public static final int Stat_STK_LEDR               = 0x40;
    public static final int Stat_STK_LEDBLINK           = 0x80;

    TreeMap<Long, int[]>memChunks;

    public static long pageSize = 128;

    public boolean main(Context c, String[] args) {
        ctx = c;

        if (args.length != 3) {
            ctx.error("Usage: __builtin_stk500::port::baud::filename");
            return false;
        }
        portName = args[0];
        String brd = args[1];
        String fle = args[2];

        baudRate = 115200;

        try {
            baudRate = Integer.parseInt(brd);
        } catch (Exception e) {
            ctx.error(e);
        }

        if(!loadHexFile(new File(fle))) {
            ctx.error(Base.i18n.string("err.notfound", fle));
            return false;
        }

        if (!connect(1000)) {
            ctx.error(Base.i18n.string("err.noconnect"));
            return false;
        }

        String dn = getDeviceName();

        enterProgMode();
        uploadProgram();
        leaveProgMode();

        disconnect();
        if (ctx.getSketch() != null) {
            ctx.getSketch().setCompilingProgress(100);
        }
        return true;
    }

    public int[] newPage() {
        int[] page = new int[(int)pageSize];

        for(int i = 0; i < (int)pageSize; i++) {
            page[i] = 0xff;
        }

        return page;
    }

    public void disconnect() {
        if(!connected) {
            return;
        }

        port.pulseLine();

        connected = false;
        port.closePort();
    }

    public boolean connect(int to) {
        timeout = to;

        port = null;
        for (CommunicationPort p : Base.communicationPorts) {
            if (p.toString().equals(portName)) {
                port = p;
            }
        }
        if (port == null) {
            ctx.error("Unable to find port " + portName);
            return false;
        }

        port.openPort();
        port.addCommsListener(this);
        port.setSpeed(baudRate);

        port.pulseLine();

        int tries = 10;
        boolean rv = false;

        while(tries > 0 && !rv) {
            rv = sendCommand(new int[] {Cmnd_GET_SYNC});
            tries--;
        }

        if(tries == 0) {
            connected = false;

            ctx.error("Connection timed out");

            port.closePort();
            return false;
        }

        if(!rv) {
            connected = false;
            port.closePort();
            return false;
        }

        rv = sendCommand(new int[] {Cmnd_GET_SIGN_ON});
        if (!rv) {
            connected = false;
            port.closePort();
            return false;
        }

        connected = true;

        return true;

    }

    public String getDeviceName() {
        if(!connected) {
            return null;
        }

        return "Arduino";
    }

    public boolean sendCommand(int[] command) {
        replyIsAvailable = false;
        try {
            for(int i = 0; i < command.length; i++) {
                port.write((byte)(command[i] & 0xFF));
            }

            port.write((byte)(Sync_CRC_EOP & 0xFF));

            int to = 0;
            while (!replyIsAvailable) {
                Thread.sleep(10);
                to++;
                if (to > 100) {
                    return false;
                }
            }

            return true;

        } catch(Exception e) {
e.printStackTrace();
            ctx.error(e);
            return false;
        }
    }

    public boolean setParameter(int param, int val) {
        if(!connected) {
            return false;
        }

        if(!sendCommand(new int[] {Cmnd_SET_PARAMETER, param, val})) {
            return false;
        }

        return true;
    }

    public int getParameter(int param) {
        if(!connected) {
            return 0;
        }

        if (!sendCommand(new int[] {Cmnd_GET_PARAMETER, param})) {
            return 0;
        }

        return replyData.get(1);
    }

    public boolean uploadProgram() {
        if(!connected) {
            return false;
        }

        int numberOfChunks = memChunks.keySet().size();
        int currentChunk = 0;

        for(Long start : memChunks.keySet()) {

System.err.println("Programming chunk " + currentChunk + " at address " + start);

            int perc = currentChunk * 100 / numberOfChunks;
            if (ctx.getSketch() != null) {
                ctx.getSketch().setCompilingProgress(perc);
            }

            currentChunk ++;

            if(!loadAddress(start)) {

                return false;
            }

            int[] chunk = memChunks.get(start);

            if(!uploadPage(chunk)) {
                return false;
            }
        }

        return true;
    }

    public boolean uploadPage(int[] data) {
        int[] message = new int[data.length + 4];
        int len = data.length;
        message[0] = Cmnd_STK_PROG_PAGE;
        message[1] = ((len >> 8) & 0xFF);
        message[2] = (len & 0xFF);
        message[3] = 0x46;

        for(int i = 0; i < len; i++) {
            message[4 + i] = data[i];
        }

        if(!sendCommand(message)) {
            ctx.error(Base.i18n.string("err.upload"));
            return false;
        }

        return true;
    }

    public boolean loadAddress(long address) {
        if(!connected) {
            return false;
        }

        address = address >>> 1;


        int a0 = (int)(address & 0xFFL);
        int a1 = (int)((address >> 8) & 0xFFL);

        if (!sendCommand(new int[] {Cmnd_STK_LOAD_ADDRESS, a0, a1})) {
            return false;
        }

        return true;
    }

    public boolean enterProgMode() {
        if(!connected) {
            return false;
        }

        if (!sendCommand(new int[] { Cmnd_STK_ENTER_PROGMODE })) {
            return false;
        }

        return true;
    }

    public boolean leaveProgMode() {
        if(!connected) {
            return false;
        }

        if (!sendCommand(new int[] { Cmnd_STK_LEAVE_PROGMODE })) {
            return false;
        }

        return true;
    }

    static class HexRecord {
        int length = 0;
        long address = 0;
        int type = 0;
        int[] data = null;
        int checksum = 0;

        String lineBuffer = "";

        public final static int Data = 0x00;
        public final static int EoF = 0x01;
        public final static int ExtendedSegmentAddress = 0x02;
        public final static int StartSegmentAddress = 0x03;
        public final static int ExtendedLinearAddress = 0x04;
        public final static int StartLinearAddress = 0x05;

        public HexRecord(String line) {
            if(line.startsWith(":")) {
                lineBuffer = line.substring(1);

                length = shiftHex();
                int a0 = shiftHex();
                int a1 = shiftHex();
                address = ((a0  << 8) | a1) & 0xFFFF;
                type = shiftHex();
                data = new int[length];

                for(int i = 0; i < length; i++) {
                    data[i] = shiftHex();
                }

                checksum = shiftHex();
            }
        }

        public int getLength() {
            return (int)length;
        }
        public long getAddress() {
            return address;
        }
        public int[] getData() {
            return data;
        }
        public int getChecksum() {
            return checksum;
        }
        public int getType() {
            return type;
        }

        private int shiftHex() {
            if(lineBuffer.length() < 2) {
                return 0;
            }

            char c0 = lineBuffer.charAt(0);
            char c1 = lineBuffer.charAt(1);
            lineBuffer = lineBuffer.substring(2);

            int b = (h2d(c0) << 4) | h2d(c1);
            return b;

        }

        private int h2d(char c) {
            switch(c) {
            case '0':
                return 0;

            case '1':
                return 1;

            case '2':
                return 2;

            case '3':
                return 3;

            case '4':
                return 4;

            case '5':
                return 5;

            case '6':
                return 6;

            case '7':
                return 7;

            case '8':
                return 8;

            case '9':
                return 9;

            case 'a':
                return 10;

            case 'A':
                return 10;

            case 'b':
                return 11;

            case 'B':
                return 11;

            case 'c':
                return 12;

            case 'C':
                return 12;

            case 'd':
                return 13;

            case 'D':
                return 13;

            case 'e':
                return 14;

            case 'E':
                return 14;

            case 'f':
                return 15;

            case 'F':
                return 15;
            }

            return 0;
        }
    }

    public int unsigned_byte(byte b) {
        return (int)b & 0xFF;
    }

    public long unsigned_int(int b) {
        return (long)b & 0xFFFFFFFFL;
    }

    public boolean loadHexFile(File hexFile) {
        long baseAddress = 0;
        long fullAddress = 0;
        long segmentAddress = 0;
        long recordAddress = 0;
        long currentAddress = 0;

        memChunks = new TreeMap<Long, int[]>();
        int b0;
        int b1;
        int[] data;
        int tint;

        InputStream fis;
        BufferedReader br;
        String line;

        try {
            fis = new FileInputStream(hexFile);
            br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));

            while((line = br.readLine()) != null) {
                if(!line.startsWith(":")) {
                    continue;
                }

                HexRecord hr = new HexRecord(line);

                switch(hr.getType()) {
                case HexRecord.Data:
                    recordAddress = hr.getAddress();
                    fullAddress = baseAddress + (recordAddress + segmentAddress);
                    memChunks.put((Long)fullAddress, (int[])hr.getData());
                    break;

                case HexRecord.EoF:
                    break;

                case HexRecord.ExtendedSegmentAddress:
                    data = hr.getData();
                    segmentAddress = (data[0] << 8) | data[1];
                    segmentAddress *= 16;
                    break;

                case HexRecord.StartSegmentAddress:
                    // Not supported
                    break;

                case HexRecord.ExtendedLinearAddress:
                    data = hr.getData();
                    baseAddress = ((data[0] << 24) | (data[1] << 16));
                    break;

                case HexRecord.StartLinearAddress:
                    // Not supported
                    break;
                default:
                    break;
                }
            }

            br.close();
        } catch(Exception e) {
            ctx.error(e);
            return false;
        }

        // Now let's see about coalescing the chunks into big blocks, each of a page in size.

        TreeMap<Long, int[]> compressedChunks = new TreeMap<Long, int[]>();

        for(Long start : memChunks.keySet()) {
            int[] chunkData = memChunks.get(start);

            long pagestart = start & ~(pageSize - 1L);


            int[] pageData = compressedChunks.get(pagestart);

            if(pageData == null) {
                pageData = newPage();
            }

            long pageoffset = start - pagestart;

            //System.err.println(String.format("Coalesced chunk of length %d starting at 0x%x into page 0x%x",
            //    chunkData.length, start, pagestart));

            for(int i = 0; i < chunkData.length; i++) {
                if(pageoffset + i == pageSize) {
                    compressedChunks.put(pagestart, pageData);
                    pagestart += pageSize;
                    pageoffset = 0;
                    pageData = compressedChunks.get(pagestart);

                    if(pageData == null) {
                        pageData = newPage();
                    }
                }

                pageData[i + (int)pageoffset] = chunkData[i];
            }

            compressedChunks.put(pagestart, pageData);
        }

        memChunks = compressedChunks;


        return true;
    }

    public void commsEventReceived(CommsEvent e) {
    }

    ArrayList<Integer> replyData = new ArrayList<Integer>();
    int recPhase = 0;
    int msgLen = 0;

    public void commsDataReceived(byte[] data) {
        for (int i = 0; i < data.length; i++) {
            int recByte = (int)data[i];

            switch(recPhase) {
                case 0: // Message start
                    if (recByte == Resp_STK_INSYNC) {
                        recPhase = 1;
                    }
                    break;
                case 1:
                    if (recByte == Resp_STK_OK) {
                        replyIsAvailable=true;
                        recPhase = 0;
                    } else {
                        replyData.add(recByte);
                    }
                    break;
            }
        }
    }

    public void kill() {
    }

}
