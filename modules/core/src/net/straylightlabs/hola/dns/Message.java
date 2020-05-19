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

package net.straylightlabs.hola.dns;

import java.nio.ByteBuffer;

public abstract class Message {
    protected final ByteBuffer buffer;

    public final static int MAX_LENGTH = 9000; // max size of mDNS packets, in bytes

    private final static int USHORT_MASK = 0xFFFF;

    protected Message() {
        buffer = ByteBuffer.allocate(MAX_LENGTH);
    }

    protected int readUnsignedShort() {
        return buffer.getShort() & USHORT_MASK;
    }

    public String dumpBuffer() {
        StringBuilder sb = new StringBuilder();
        int length = buffer.position();
        if (length == 0) {
            length = buffer.limit();
        }
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02x", buffer.get(i)));
            if ((i + 1) % 8 == 0) {
                sb.append('\n');
            } else if ((i + 1) % 2 == 0) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }
}
