package org.uecide;

import java.io.*;
import java.util.*;

public class HexFile {
    TreeMap<Long, Long> memory;

    public HexFile() {
        memory = new TreeMap<Long, Long>();
    }

    public HexFile(File in) {
        memory = new TreeMap<Long, Long>();
        loadFile(in);
    }

    int h2d(char c) {
        switch(c) { 
            case '0': return 0;
            case '1': return 1;
            case '2': return 2;
            case '3': return 3;
            case '4': return 4;
            case '5': return 5;
            case '6': return 6;
            case '7': return 7;
            case '8': return 8;
            case '9': return 9;
            case 'a': return 10;
            case 'A': return 10;
            case 'b': return 11;
            case 'B': return 11;
            case 'c': return 12;
            case 'C': return 12;
            case 'd': return 13;
            case 'D': return 13;
            case 'e': return 14;
            case 'E': return 14;
            case 'f': return 15;
            case 'F': return 15;
        }
        return 0;
    }

    int h2d2(char a, char b) {
        return h2d(a) << 4 | h2d(b);
    }

    int h2d4(char a, char b, char c, char d) {
        return h2d(a) << 12 | h2d(b) << 8 | h2d(c) << 4 | h2d(d);
    }

    public boolean loadFile(File in) {
        if (!in.exists()) {
            return false;
        }

        Long baseAddress = 0L;
        Long currentAddress = 0L;
        try {
            FileReader fr = new FileReader(in);
            BufferedReader br = new BufferedReader(fr);

            String line = br.readLine();
            while (line != null) {
                line = line.trim();
                if (line.startsWith(":")) {
                    char[] chars = line.toCharArray();
                    int reclen = h2d2(chars[1], chars[2]);
                    int address = h2d4(chars[3], chars[4], chars[5], chars[6]);
                    int rectype = (int)h2d2(chars[7], chars[8]);
                    switch (rectype) {
                        case 0: // Data
                            currentAddress = baseAddress + address;

                            for (int i = 0; i < reclen * 2; i += 2) {
                                Long val = (long)h2d2(chars[9 + i], chars[10 + i]);
                                Long offsetAddress = currentAddress & 0xFFFFFFFCL;
                                Long offsetByte = currentAddress % 4;
                                Long temp = memory.get(offsetAddress);
                                if (temp == null) {
                                    temp = 0L;
                                }
                                temp |= (Long)val << ((3-offsetByte) * 8);
                                memory.put(offsetAddress, temp);
                                currentAddress++;
                            }
                            break;
                        case 1: // End of file
                            br.close();
                            return true;
                        case 4: // Extended Linear Address
                            baseAddress = (long)h2d4(chars[9], chars[10], chars[11], chars[12]) << 16;
                            break;
                    }
                }
                line = br.readLine();
            }
        } catch (Exception e) {
            return false;
        }
        return false;
        
    }

    int csum(int total) {
        total = total & 0xFF;
        total = 256 - total;
        total = total & 0xFF;
        return total;
    }

    String dataRecord(Long[] data, int len, Long lineAddress) {
        String o = ":";
        int total = 0;

        o += String.format("%02x", len * 4); 
        total += len*4;

        o += String.format("%02x", lineAddress >> 8 & 0xFF);
        total += (lineAddress >> 8 & 0xFF);
        
        o += String.format("%02x", lineAddress & 0xFF);
        total += (lineAddress & 0xFF);

        o += "00";
        total += 0;

        for (int i = 0; i < len; i++) {
            o += String.format("%02x", data[i] >> 24 & 0xFF);
            total += (data[i] >> 24 & 0xFF);

            o += String.format("%02x", data[i] >> 16 & 0xFF);
            total += (data[i] >> 16 & 0xFF);

            o += String.format("%02x", data[i] >> 8 & 0xFF);
            total += (data[i] >> 8 & 0xFF);

            o += String.format("%02x", data[i] & 0xFF);
            total += (data[i] & 0xFF);
        }
        
        o += String.format("%02x", csum(total));
        return o;
    }

    public boolean saveFile(File out) {
        try {
            PrintWriter pw = new PrintWriter(out);
            Long[] addressList = memory.keySet().toArray(new Long[0]);
            Long startAddress = -1L;
            Long startOffset = -1L;
            Long lastAddress = -1L;
            Long[] recordChunk = new Long[16];
            Long lineAddress = addressList[0];
            int reclen = 0;
            for (Long address : addressList) {
                Long addressOffset = address >> 16 & 0xFFFF;

                if (!addressOffset.equals(startOffset)) { // Starting a new address segment
                    startOffset = addressOffset;

                    if (reclen > 0) {
                        pw.println(dataRecord(recordChunk, reclen, lineAddress));
                        reclen = 0;
                        lineAddress = address;
                    }

                    pw.println(String.format(":02000004%02x%02x%02x",
                        startOffset >> 8 % 0xFF,
                        startOffset & 0xFF,
                        csum((int)(2+0+0+4+(startOffset >> 8 & 0xFF) + (startOffset & 0xFF)))
                    ));
                }

                if (!(address.equals(lastAddress + 4L))) { // Gap
                    if (reclen > 0) {
                        pw.println(dataRecord(recordChunk, reclen, lineAddress));
                        reclen = 0;
                        lineAddress = address;
                    }
                }

                if (reclen == 4) {
                    pw.println(dataRecord(recordChunk, reclen, lineAddress));
                    reclen = 0;
                    lineAddress = address;
                }
                recordChunk[reclen++] = memory.get(address);
                lastAddress = address;
            }

            if (reclen > 0) {
                pw.println(dataRecord(recordChunk, reclen, lineAddress));
            }

            pw.println(":00000001FF");

            pw.close();
        } catch (Exception e) {
            return false;
        }
        return false;
    }
}
