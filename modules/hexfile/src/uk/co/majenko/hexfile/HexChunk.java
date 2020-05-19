package uk.co.majenko.hexfile;

import java.io.*;
import java.util.*;

public class HexChunk {
    // The starting address of this chunk.
    private int offset;
    // The lowest address within the chunk written.
    private int startAddress;
    // the highest address within the chunk written.
    private int endAddress;
    private int[] data;

    public HexChunk(int off) {
        offset = off;
        startAddress = 65535;
        endAddress = 0;
        data = new int[65536];
    }

    public int getStartAddress() {
        return startAddress;
    }

    public int getEndAddress() {
        return endAddress;
    }

    public void addByte(int addr, int b) {
        if (addr < startAddress) startAddress = addr;
        if (addr > endAddress) endAddress = addr;
        data[addr] = (b & 0xFF) | 0x100;
    }

    public int getByte(int address) {
        if (address < startAddress || address > endAddress) {
            return -1;
        }
        if ((data[address] & 0xFF00) == 0) {
            return -1;
        }
        return data[address] & 0xFF;
    }

    // Print a line of hex pairs followed by the checksum.
    private void printLine(PrintWriter pw, int[] vals) { 
        pw.print(":");
        int cs = 0;
        for (int val : vals) {
            cs += (val & 0xFF);
            pw.print(String.format("%02x", (val & 0xff)));
        }
        int ncs = 256 - (cs & 0xFF);
        pw.print(String.format("%02x%n", (ncs & 0xff)));
    }

    void outputChunk(PrintWriter pw, int off, ArrayList<Integer>chunk) {
        int[] d = new int[chunk.size() + 4];
        d[0] = (chunk.size() & 0xFF);
        d[1] = ((off >> 8) & 0xFF);
        d[2] = (off & 0xFF);
        d[3] = 0x00;
        for (int i = 0; i < chunk.size(); i++) {
            d[4+i] = (chunk.get(i) & 0xFF);
        }
        printLine(pw, d);
    }

    public void writeChunk(PrintWriter pw) {
    try {
        // Disable any previous offset. No idea. This seems to be a 
        // common thing, though I don't see the point. But I guess I'll
        // do it too. Maybe this is how these things start... "I don't
        // know why he did this, but he must have had a reason, so I'll
        // do the same." and so on, until "everyone does this, so I will too"...
//        printLine(pw, new int[] { 0x02, 0x00, 0x00, 0x04, 0x00, 0x00 });

        // Now put out our own offset (if it's 0 we won't bother, since we just did).
        if (offset > 0) {
            printLine(pw, new int[] { 0x02, 0x00, 0x00, 0x04, (offset >> 8) & 0xFF, offset & 0xFF });
        } 

        boolean inChunk = false;
        ArrayList<Integer> chunk = null;
        int lineOff = 0;
        int chunkCount = 0;
        for (int i = 0; i < 65536; i++) {
            int b = data[i];
                
            if ((b & 0x100) == 0x100) { // It's a valid byte
                if (!inChunk) {
                    inChunk = true;
                    chunkCount = 0;
                    lineOff = i;
                    chunk = new ArrayList<Integer>();
                }
            } else {
                if (inChunk) {
                    outputChunk(pw, lineOff, chunk);
                }
                inChunk = false;
            }

            if (inChunk) {
                chunk.add(b & 0xFF);
                chunkCount ++;
                if (chunkCount == 16) {
                    outputChunk(pw, lineOff, chunk);
                    inChunk = false;
                }
            }
        }

        if (inChunk) {
            outputChunk(pw, lineOff, chunk);
        }

    } catch (Exception ex) {
        ex.printStackTrace();
    }

    }

    public int bytesUsed() {
        if (startAddress > endAddress) return 0;
        return endAddress - startAddress;
    }

}
