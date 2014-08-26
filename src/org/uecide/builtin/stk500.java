package org.uecide.builtin;

import org.uecide.*;
import jssc.*;
import java.util.*;
import java.io.*;

public class stk500 implements BuiltinCommand {

    SerialPort port = null;
    String portName = null;
    int baudRate = 115200;
    Sketch sketch;
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

    public boolean main(Sketch sktch, String[] args) {
        sketch = sktch;
        if (args.length != 3) {
            sketch.error("Usage: __builtin_stk500::port::baud::filename");
            return false;
        }
        portName = args[0];
        String brd = args[1];
        String fle = args[2];

        baudRate = 115200;

        try {
            baudRate = Integer.parseInt(brd);
        } catch (Exception e) {
            Base.error(e);
        }

        if(loadHexFile(new File(fle))) {
            sketch.message("File loaded");
        } else {
            sketch.error("Unable to load file " + fle);
            return false;
        }

        if (!connect(1000)) {
            sketch.error("Unable to connect");
            return false;
        }

        String dn = getDeviceName();
        if(dn != null) {
            sketch.message("Connected to " + dn);
        }

        if(enterProgMode()) {
            sketch.message("Entered programming mode");
        }

        if(uploadProgram()) {
            sketch.message("Upload complete");
        }

        if(leaveProgMode()) {
            sketch.message("Left programming mode");
        }

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

        try {
            port.setDTR(false);
            port.setRTS(false);
        } catch(Exception e) {
            Base.error(e);
        }

        connected = false;
        Serial.closePort(port);
    }

    public boolean connect(int to) {
        timeout = to;
        port = Serial.requestPort(portName, baudRate);

        sequence = 0;

        if(port == null) {
            sketch.error("Unable to open port " + portName);
            return false;
        }

        try {
            Thread.sleep(100); // Initial short delay
        } catch(Exception e) {
        }

        try {
            port.setDTR(true);
            port.setRTS(true);
        } catch(Exception e) {
            Base.error(e);
        }

        int tries = 10;
        int[] rv = null;

        while(tries > 0 && rv == null) {
            rv = sendCommand(new int[] {CMD_SIGN_ON});
            tries--;
        }

        if(tries == 0) {
            connected = false;

            sketch.error("Connection timed out");

            Serial.closePort(port);
            return false;
        }

        if(rv == null) {
            connected = false;
            Serial.closePort(port);
            return false;
        }

        if(rv[0] != CMD_SIGN_ON) {
            connected = false;
            Serial.closePort(port);
            return false;
        }

        int status = rv[1];

        if(status != STATUS_CMD_OK) {
            connected = false;
            Serial.closePort(port);
            return false;
        }

        int rlen = rv[2];

        deviceName = "";

        for(int i = 0; i < rlen; i++) {
            char c = (char)rv[3 + i];
            deviceName += c;
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

    public int[] sendCommand(int[] command) {

//        System.out.print("STK500V2: stk500v2_command(");
//        for (int i : command) {
//            System.out.print(String.format("0x%02x ", i & 0xFF));
//        }
//        System.out.print(", ");
//        System.out.print(command.length);
//        System.out.println(")");

        try {
            int checksum = 0;
            port.writeByte((byte)0x1B);
            checksum ^= 0x1B;
            port.writeByte((byte)(sequence & 0xFF));
            checksum ^= sequence;
            port.writeByte((byte)((command.length >> 8) & 0xFF));
            checksum ^= ((command.length >> 8) & 0xFF);
            port.writeByte((byte)(command.length & 0xFF));
            checksum ^= (command.length & 0xFF);
            port.writeByte((byte)0x0E);
            checksum ^= 0x0E;

            for(int i = 0; i < command.length; i++) {
                port.writeByte((byte)(command[i] & 0xFF));
                checksum ^= command[i];
            }

            port.writeByte((byte)(checksum & 0xFF));

            sequence++;

            byte[] header = port.readBytes(5, timeout);

            while(header[0] != (byte)0x1B && header[4] != (byte)0x0E) {
                byte[] next = port.readBytes(1, timeout);
                header[0] = header[1];
                header[1] = header[2];
                header[2] = header[3];
                header[3] = header[4];
                header[4] = next[0];
            }

            int msglen = (unsigned_byte(header[2]) << 8) | unsigned_byte(header[3]);
            byte[] message = port.readBytes(msglen, timeout);
            byte[] cs = port.readBytes(1, timeout);

            int[] out = new int[msglen];

            for(int i = 0; i < msglen; i++) {
                out[i] = unsigned_byte(message[i]);
            }

            return out;

        } catch(Exception e) {
            Base.error(e);
            return null;
        }
    }

    public boolean setParameter(int param, int val) {
        if(!connected) {
            return false;
        }

        int[] rv = sendCommand(new int[] {CMD_SET_PARAMETER, param, val});

        if(rv == null) {
            return false;
        }

        if(rv[1] == STATUS_CMD_OK) {
            return true;
        }

        return false;
    }

    public int getParameter(int param) {
        if(!connected) {
            return 0;
        }

        int[] rv = sendCommand(new int[] {CMD_GET_PARAMETER, param});

        if(rv == null) {
            return 0;
        }

        if(rv[1] == STATUS_CMD_OK) {
            return rv[2];
        }

        return 0;
    }

    public boolean osccal() {
        if(!connected) {
            return false;
        }

        int[] rv = sendCommand(new int[] {CMD_OSCCAL});

        if(rv == null) {
            return false;
        }

        if(rv[1] == STATUS_CMD_OK) {
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
            sketch.setCompilingProgress(perc);

            currentChunk ++;

            
            if(firstrun) {
                currentAddress = start;
                offset = start;

                if(!loadAddress(currentAddress)) {
                    Base.error(String.format("Load Address failed at address 0x%08x", currentAddress));
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
/*
                } else {
                    int[] page = newPage();

                    while(currentAddress != start) {

                        if(!uploadPage(page)) {
                            return false;
                        }

                        currentAddress += page.length;
                    }
                }
            }
*/

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

        int[] rv = sendCommand(message);

        if(rv == null) {
            Base.error("Upload failed");
            return false;
        }

        if(rv[1] != STATUS_CMD_OK) {
            Base.error("Upload failed");
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

            int[] rv = sendCommand(new int[] {CMD_LOAD_ADDRESS, a3, a2, a1, a0});

            if(rv == null) {
                return false;
            }

            if(rv[1] == STATUS_CMD_OK) {
                return true;
            }

            return false;
        } else {
            //int[] rv = sendCommand(new int[] {CMD_LOAD_ADDRESS, 0x80, 0x00, 0x00, 0x00});
            int[] rv = sendCommand(new int[] {CMD_LOAD_ADDRESS, 0x00, 0x00, 0x00, 0x00});

            if(rv == null) {
                return false;
            }

            if(rv[1] != STATUS_CMD_OK) {
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

        int[] rv = sendCommand(new int[] {
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

        if(rv[1] == STATUS_CMD_OK) {
            return true;
        }

        return false;
    }

    public boolean leaveProgMode() {
        if(!connected) {
            return false;
        }

        int[] rv = sendCommand(new int[] { CMD_LEAVE_PROGMODE_ISP, 1, 1});

        if(rv == null) {
            Base.error("Timeout leaving programming mode!");
            return false;
        }

        if(rv[1] != STATUS_CMD_OK) {
            Base.error("Error leaving programming mode!");
            return false;
        }

        return true;
    }

    class HexRecord {
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
                    System.out.println(String.format("Loaded block at 0x%08x, size %d, made up of ba: 0x%08x, ra: 0x%08x, sa: 0x%08x",
                        fullAddress, hr.getLength(), baseAddress, recordAddress, segmentAddress));
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
                    System.out.println(String.format("New base address 0x%08x", baseAddress));
                    break;

                case HexRecord.StartLinearAddress:
                    // Not supported
                    break;
                default:
                    System.out.println(String.format("Unknown record type 0x%02X", hr.getType()));
                    break;
                }
            }

            br.close();
        } catch(Exception e) {
            Base.error(e);
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

            System.out.println(String.format("Coalesced block at 0x%08x into page 0x%08x offset 0x%04x",
                start, pagestart, pageoffset));

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
}
