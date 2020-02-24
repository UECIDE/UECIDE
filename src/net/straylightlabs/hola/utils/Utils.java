/*
 * The MIT License
 *
 * Copyright (c) 2015-2018 Todd Kulesza <todd@dropline.net>
 *
 * This file is part of Hola.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.straylightlabs.hola.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Utils {
    private static int nextDumpPathSuffix;
    private final static Logger logger = LoggerFactory.getLogger(Utils.class);

    static {
        nextDumpPathSuffix = 1;
    }

    /**
     * Save @packet to a new file beginning with @prefix. Append a sequential suffix to ensure we don't
     * overwrite existing files.
     * @param packet The data packet to dump to disk
     * @param prefix The start of the file name
     */
    @SuppressWarnings("unused")
    public static void dumpPacket(DatagramPacket packet, String prefix) {
        byte[] buffer = new byte[packet.getLength()];
        System.arraycopy(packet.getData(), packet.getOffset(), buffer, 0, packet.getLength());
        printBuffer(buffer, "Buffer to save");
        try {
            Path path = getNextPath(prefix);
            logger.info("Dumping buffer to {}", path);
            Files.write(path, buffer);
        } catch (IOException e) {
            logger.error("Error writing file: {}", e.getLocalizedMessage());
        }
    }

    /**
     * Get the next sequential Path for a binary dump file, ensuring we don't overwrite any existing files.
     * @param prefix The start of the file name
     * @return Next sequential Path
     */
    public static Path getNextPath(String prefix) {
        Path path;

        do {
            path = Paths.get(String.format("%s%s", prefix, Integer.toString(nextDumpPathSuffix)));
            nextDumpPathSuffix++;
        } while (Files.exists(path));

        return path;
    }

    /**
     * Print a formatted version of @buffer in hex
     * @param buffer the byte buffer to display
     */
    public static void printBuffer(byte[] buffer, String msg) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < buffer.length; i++) {
            if (i % 20 == 0) {
                sb.append("\n\t");
            }
            sb.append(String.format("%02x", buffer[i]));
        }

        logger.info("{}: {}", msg, sb.toString());
    }
}
