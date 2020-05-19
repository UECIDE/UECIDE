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

public class PtrRecord extends Record {
    private final String userVisibleName;
    private final String ptrName;

    public final static String UNTITLED_NAME = "Untitled";

    public PtrRecord(ByteBuffer buffer, String name, Class recordClass, long ttl, int rdLength) {
        super(name, recordClass, ttl);
        if (rdLength > 0) {
            ptrName = readNameFromBuffer(buffer);
        } else {
            ptrName = "";
        }
        userVisibleName = buildUserVisibleName();
    }

    public String getPtrName() {
        return ptrName;
    }

    public String getUserVisibleName() {
        return userVisibleName;
    }

    private String buildUserVisibleName() {
        String[] parts = ptrName.split("\\.");
        if (parts[0].length() > 0) {
            return parts[0];
        } else {
            return UNTITLED_NAME;
        }
    }

    @Override
    public String toString() {
        return "PtrRecord{" +
                "name='" + name + '\'' +
                ", recordClass=" + recordClass +
                ", ttl=" + ttl +
                ", ptrName='" + ptrName + '\'' +
                '}';
    }
}
