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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class Record {
    protected final String name;
    protected final long ttl;

    protected final Class recordClass;

    private final static Logger logger = LoggerFactory.getLogger(Record.class);

    public final static int USHORT_MASK = 0xFFFF;
    public final static long UINT_MASK = 0xFFFFFFFFL;
    public final static String NAME_CHARSET = "UTF-8";

    public static Record fromBuffer(ByteBuffer buffer) {
        String name = readNameFromBuffer(buffer);
        Type type = Type.fromInt(buffer.getShort() & USHORT_MASK);
//        int rrClassByte = buffer.getShort() & 0x7FFF;
        int tmp = buffer.getShort() & 0xFFFF;
        // FIXME allow the user to see that cache's should be flushed?
        boolean flushCache = (tmp & 0x8000) == 0x8000;
        int rrClassByte = tmp & 0x7FFF;
        Class recordClass = Class.fromInt(rrClassByte);
        long ttl = buffer.getInt() & UINT_MASK;
        int rdLength = buffer.getShort() & USHORT_MASK;

        switch (type) {
            case A:
                try {
                    return new ARecord(buffer, name, recordClass, ttl);
                } catch (UnknownHostException e) {
                    throw new IllegalArgumentException("Buffer does not represent a valid A record");
                }
            case AAAA:
                try {
                    return new AaaaRecord(buffer, name, recordClass, ttl);
                } catch (UnknownHostException e) {
                    throw new IllegalArgumentException("Buffer does not represent a valid AAAA record");
                }
            case PTR:
                return new PtrRecord(buffer, name, recordClass, ttl, rdLength);
            case SRV:
                return new SrvRecord(buffer, name, recordClass, ttl);
            case TXT:
                return new TxtRecord(buffer, name, recordClass, ttl, rdLength);
            default:
                logger.debug("Buffer represents an unsupported record type, skipping ahead {} bytes", rdLength);
                return new UnknownRecord(buffer, name, recordClass, ttl, rdLength);
        }
    }

    protected Record(String name, Class recordClass, long ttl) {
        this.name = name;
        this.recordClass = recordClass;
        this.ttl = ttl;
    }

    public static String readNameFromBuffer(ByteBuffer buffer) {
        List<String> labels = new ArrayList<>();
        int labelLength;
        int continueFrom = -1;
        do {
            buffer.mark();
            labelLength = buffer.get() & 0xFF;
            if (isPointer(labelLength)) {
                buffer.reset();
                int offset = buffer.getShort() & 0x3FFF;
                if (continueFrom < 0) {
                    continueFrom = buffer.position();
                }
                buffer.position(offset);
            } else {
                String label = readLabel(buffer, labelLength);
                labels.add(label);
            }
        } while (labelLength != 0);

        if (continueFrom >= 0) {
            buffer.position(continueFrom);
        }

        return labels.stream().collect(Collectors.joining("."));
    }

    private static boolean isPointer(int octet) {
        return (octet & 0xC0) == 0xC0;
    }

    private static String readLabel(ByteBuffer buffer, int length) {
        String label = "";
        if (length > 0) {
            byte[] labelBuffer = new byte[length];
            buffer.get(labelBuffer);
            try {
                label = new String(labelBuffer, NAME_CHARSET);
            } catch (UnsupportedEncodingException e) {
                System.err.println("UnsupportedEncoding: " + e);
            }
        }
        return label;
    }

    public static List<String> readStringsFromBuffer(ByteBuffer buffer, int length) {
        List<String> strings = new ArrayList<>();
        int bytesRead = 0;
        do {
            int stringLength = buffer.get() & 0xFF;
            String label = readLabel(buffer, stringLength);
            bytesRead += label.length() + 1;
            strings.add(label);
        } while (bytesRead < length);
        return strings;
    }

    public String getName() {
        return name;
    }

    public long getTTL() {
        return ttl;
    }

    @Override
    public String toString() {
        return "Record{" +
                "name='" + name + '\'' +
                ", recordClass=" + recordClass +
                ", ttl=" + ttl +
                '}';
    }

    enum Type {
        UNSUPPORTED(0),
        A(1),
        NS(2),
        CNAME(5),
        SOA(6),
        NULL(10),
        WKS(11),
        PTR(12),
        HINFO(13),
        MINFO(14),
        MX(15),
        TXT(16),
        AAAA(28),
        SRV(33);

        private final int value;

        public static Type fromInt(int val) {
            for (Type type : values()) {
                if (type.value == val) {
                    return type;
                }
            }
            return UNSUPPORTED;
        }

        Type(int value) {
            this.value = value;
        }

        public int asUnsignedShort() {
            return value & USHORT_MASK;
        }
    }

    enum Class {
        IN(1);

        private final int value;

        public static Class fromInt(int val) {
            for (Class c : values()) {
                if (c.value == val) {
                    return c;
                }
            }
            throw new IllegalArgumentException(String.format("Can't convert 0x%04x to a Class", val));
        }

        Class(int value) {
            this.value = value;
        }

        public int asUnsignedShort() {
            return value & USHORT_MASK;
        }
    }
}
