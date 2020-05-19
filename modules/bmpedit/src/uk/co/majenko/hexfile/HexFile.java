package uk.co.majenko.hexfile;

import java.util.*;
import java.io.*;

public class HexFile {
    private TreeMap<Integer, HexChunk> chunks;

    public HexFile() {
        chunks = new TreeMap<Integer, HexChunk>();
    }

    public HexFile(File f) throws IOException {
        chunks = new TreeMap<Integer, HexChunk>();
        loadFile(f);
    }

    public void loadFile(File f) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(f));

        String line;

        HexChunk currentChunk = null;
        int off = 0;

        while ((line = in.readLine()) != null) {
            if (!line.startsWith(":")) {
                continue;
            }
            
            int[] data = hexStringToByteArray(line.substring(1));

            int dlen = data[0];
            int lineoff = (data [1] << 8) | data[2];
            int type = data[3];

            switch (type) {
                case 0x00: // Data
                    if (currentChunk != null) {
                        for (int i = 0; i < dlen; i++) {
                            int b = data[4 + i];
                            int add = lineoff + i;
                            if (add > 0xFFFF) {
                                off++;
                                lineoff -= 0x10000;
                                add = lineoff + i;
                                currentChunk = chunks.get(off);
                                if (currentChunk == null) {
                                    currentChunk = new HexChunk(off);
                                    chunks.put(off, currentChunk);
                                }
                            }
                            currentChunk.addByte(add, b);
                       } 
                    }
                    break;

                case 0x01: // End of file
                    in.close();
                    return;
                case 0x02:
                    off = ((data[4] << 8) | data[5]) * 16;
                    currentChunk = chunks.get(off);
                    if (currentChunk == null) {
                        currentChunk = new HexChunk(off);
                        chunks.put(off, currentChunk);
                    }
                    break;
                case 0x03:
                    break;
                case 0x04:
                    off = ((data[4] << 8) | data[5]);
                    currentChunk = chunks.get(off);
                    if (currentChunk == null) {
                        currentChunk = new HexChunk(off);
                        chunks.put(off, currentChunk);
                    }
                    break;
                case 0x05:
                    break;
                
                
            }
        }

        in.close();
    }

    public static int[] hexStringToByteArray(String s) {
        int len = s.length();
        int[] data = new int[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (int) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public void saveFile(File f) throws IOException {
        PrintWriter pw = new PrintWriter(f);
        for (HexChunk chunk : chunks.values()) {
            if (chunk.bytesUsed() > 0) {
                chunk.writeChunk(pw);
            }
        }
        pw.println(":00000001FF");
        pw.close();
    }

}
