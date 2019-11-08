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

public class stk500v2 extends BuiltinCommand implements CommsListener {

    boolean replyIsAvailable = false;

    String portName = null;
    CommunicationPort port;
    int baudRate = 115200;
    Context ctx;
    int sequence = 0;
    boolean connected = false;
    int timeout = 1000;

    String deviceName = null;

    public static final int CMD_SIGN_ON                = 0x01;
    public static final int CMD_SET_PARAMETER          = 0x02;
    public static final int CMD_GET_PARAMETER          = 0x03;
    public static final int CMD_SET_DEVICE_PARAMETERS  = 0x04;
    public static final int CMD_OSCCAL                 = 0x05;
    public static final int CMD_LOAD_ADDRESS           = 0x06;
    public static final int CMD_FIRMWARE_UPGRADE       = 0x07;

    public static final int CMD_ENTER_PROGMODE_ISP     = 0x10;
    public static final int CMD_LEAVE_PROGMODE_ISP     = 0x11;
    public static final int CMD_CHIP_ERASE_ISP         = 0x12;
    public static final int CMD_PROGRAM_FLASH_ISP      = 0x13;
    public static final int CMD_READ_FLASH_ISP         = 0x14;
    public static final int CMD_PROGRAM_EEPROM_ISP     = 0x15;
    public static final int CMD_READ_EEPROM_ISP        = 0x16;
    public static final int CMD_PROGRAM_FUSE_ISP       = 0x17;
    public static final int CMD_READ_FUSE_ISP          = 0x18;
    public static final int CMD_PROGRAM_LOCK_ISP       = 0x19;
    public static final int CMD_READ_LOCK_ISP          = 0x1A;
    public static final int CMD_READ_SIGNATURE_ISP     = 0x1B;
    public static final int CMD_READ_OSCCAL_ISP        = 0x1C;
    public static final int CMD_SPI_MULTI              = 0x1D;

    public static final int STATUS_CMD_OK              = 0x00;

    TreeMap<Long, int[]>memChunks;

    public static long pageSize = 256;

    public boolean main(Context c, String[] args) throws BuiltinCommandException {
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

        if(loadHexFile(new File(fle))) {
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

        sequence = 0;

        try {
            Thread.sleep(100); // Initial short delay
        } catch(Exception e) {
        }

        port.pulseLine();

        int tries = 10;
        ArrayList<Integer> rv = null;

        while(tries > 0 && rv == null) {
            rv = sendCommand(new int[] {CMD_SIGN_ON});
            tries--;
        }

        if(tries == 0) {
            connected = false;

            ctx.error("Connection timed out");

            port.closePort();
            return false;
        }

        if(rv == null) {
            connected = false;
            port.closePort();
            return false;
        }

        if(rv.get(0) != CMD_SIGN_ON) {
            connected = false;
            port.closePort();
            return false;
        }

        int status = rv.get(1);

        if(status != STATUS_CMD_OK) {
            connected = false;
            port.closePort();
            return false;
        }

        int rlen = rv.get(2);

        deviceName = "";

        for(int i = 0; i < rlen; i++) {
            deviceName += Character.toString((char)((int)rv.get(3 + i)));
        }

        connected = true;

        return true;

    }

    public String getDeviceName() {
        if(!connected) {
            return null;
        }

        return deviceName;
    }

    public ArrayList<Integer> sendCommand(int[] command) {
        replyIsAvailable = false;
        try {
            int checksum = 0;
            port.write((byte)0x1B);
            checksum ^= 0x1B;
            port.write((byte)(sequence & 0xFF));
            checksum ^= sequence;
            port.write((byte)((command.length >> 8) & 0xFF));
            checksum ^= ((command.length >> 8) & 0xFF);
            port.write((byte)(command.length & 0xFF));
            checksum ^= (command.length & 0xFF);
            port.write((byte)0x0E);
            checksum ^= 0x0E;

            for(int i = 0; i < command.length; i++) {
                port.write((byte)(command[i] & 0xFF));
                checksum ^= command[i];
            }

            port.write((byte)(checksum & 0xFF));

            sequence++;

            int to = 0;
            while (!replyIsAvailable) {
                Thread.sleep(10);
                to++;
                if (to > 100) {
                    return null;
                }
            }

            return replyData;

        } catch(Exception e) {
e.printStackTrace();
            ctx.error(e);
            return null;
        }
    }

    public boolean setParameter(int param, int val) {
        if(!connected) {
            return false;
        }

        ArrayList<Integer> rv = sendCommand(new int[] {CMD_SET_PARAMETER, param, val});

        if(rv == null) {
            return false;
        }

        if(rv.get(1) == STATUS_CMD_OK) {
            return true;
        }

        return false;
    }

    public int getParameter(int param) {
        if(!connected) {
            return 0;
        }

        ArrayList<Integer> rv = sendCommand(new int[] {CMD_GET_PARAMETER, param});

        if(rv == null) {
            return 0;
        }

        if(rv.get(1) == STATUS_CMD_OK) {
            return rv.get(2);
        }

        return 0;
    }

    public boolean osccal() {
        if(!connected) {
            return false;
        }

        ArrayList<Integer> rv = sendCommand(new int[] {CMD_OSCCAL});

        if(rv == null) {
            return false;
        }

        if(rv.get(1) == STATUS_CMD_OK) {
            return true;
        }

        return false;
    }

    public boolean uploadProgram() {
        if(!connected) {
            return false;
        }

        boolean firstrun = true;

        long currentAddress = 0;
        long offset = 0;

        int numberOfChunks = memChunks.keySet().size();
        int currentChunk = 0;

        for(Long start : memChunks.keySet()) {

            int perc = currentChunk * 100 / numberOfChunks;

            currentChunk ++;

            
            if(firstrun) {
                currentAddress = start;
                offset = start;

                if(!loadAddress(currentAddress)) {
                    ctx.error(String.format("Load Address failed at address 0x%08x", currentAddress));
                    return false;
                }

                firstrun = false;
                currentAddress += pageSize;
                continue;
            }

            if (start != currentAddress) {
                loadAddress(start - offset);
                currentAddress = start;
            }

            int[] chunk = memChunks.get(start);

            if(!uploadPage(chunk))
                return false;

            currentAddress += chunk.length;
        }

        return true;
    }

    public boolean uploadPage(int[] data) {
        int[] message = new int[data.length + 10];
        int len = data.length;
        message[0] = CMD_PROGRAM_FLASH_ISP;
        message[1] = ((len >> 8) & 0xFF);
        message[2] = (len & 0xFF);
        message[3] = 0xc1;
        message[4] = 0x0a;
        message[5] = 0x40;
        message[6] = 0x4c;
        message[7] = 0x20;
        message[8] = 0xFF; // 0x00
        message[9] = 0xFF; // 0x00

        for(int i = 0; i < len; i++) {
            message[10 + i] = data[i];
        }

        ArrayList<Integer> rv = sendCommand(message);

        if(rv == null) {
            ctx.error("Upload failed");
            return false;
        }

        if(rv.get(1) != STATUS_CMD_OK) {
            ctx.error("Upload failed");
            return false;
        }

        return true;
    }

    public boolean loadAddress(long address) {
        if(!connected) {
            return false;
        }

        if(address <= 65535) {
            address = address >>> 1;


            int a0 = (int)(address & 0xFFL);
            int a1 = (int)((address >> 8) & 0xFFL);
            int a2 = (int)((address >> 16) & 0xFFL);
            int a3 = (int)((address >> 24) & 0xFFL);

            ArrayList<Integer> rv = sendCommand(new int[] {CMD_LOAD_ADDRESS, a3, a2, a1, a0});

            if(rv == null) {
                return false;
            }

            if(rv.get(1) == STATUS_CMD_OK) {
                return true;
            }

            return false;
        } else {
            //int[] rv = sendCommand(new int[] {CMD_LOAD_ADDRESS, 0x80, 0x00, 0x00, 0x00});
            ArrayList<Integer> rv = sendCommand(new int[] {CMD_LOAD_ADDRESS, 0x00, 0x00, 0x00, 0x00});

            if(rv == null) {
                return false;
            }

            if(rv.get(1) != STATUS_CMD_OK) {
                return false;
            }

            int[] page = newPage();

            int a0 = (int)(address & 0xFFL);
            int a1 = (int)((address >> 8) & 0xFFL);
            int a2 = (int)((address >> 16) & 0xFFL);
            int a3 = (int)((address >> 24) & 0xFFL);

            page[0xf8] = a0;
            page[0xf9] = a1;
            page[0xfa] = a2;
            page[0xfb] = a3;
            return uploadPage(page);
        }
    }

    public boolean enterProgMode() {
        if(!connected) {
            return false;
        }

        ArrayList<Integer> rv = sendCommand(new int[] {
                                   CMD_ENTER_PROGMODE_ISP,
                                   200,
                                   100,
                                   25,
                                   32,
                                   0,
                                   0x53,
                                   3,
                                   0xAC,
                                   0x53,
                                   0,
                                   0
                               });

        if(rv == null) {
            return false;
        }

        if(rv.get(1) == STATUS_CMD_OK) {
            return true;
        }

        return false;
    }

    public boolean leaveProgMode() {
        if(!connected) {
            return false;
        }

        ArrayList<Integer> rv = sendCommand(new int[] { CMD_LEAVE_PROGMODE_ISP, 1, 1});

        if(rv == null) {
            ctx.error("Timeout leaving programming mode!");
            return false;
        }

        if(rv.get(1) != STATUS_CMD_OK) {
            ctx.error("Error leaving programming mode!");
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
                    if (recByte == 0x1B) {
                        recPhase = 1;
                    }
                    break;
                case 1: // Seqno
                    // Ignore sequence numbers
                    recPhase = 2;
                    break;
                case 2: // Message size
                    msgLen = recByte;
                    recPhase = 3;
                    break;
                case 3: // Token
                    if (recByte == 0x0E) {
                        recPhase = 4;
                        replyData = new ArrayList<Integer>();
                    } else {
                        recPhase = 0;
                    }
                    break;
                case 4:
                    replyData.add(recByte);
                    if (replyData.size() == msgLen) {
                        recPhase = 5;
                    }
                    break;
                case 5:
                    replyIsAvailable=true;
                    recPhase = 0;
                    break;
            }
        }
    }


    public void kill() {
    }
}
