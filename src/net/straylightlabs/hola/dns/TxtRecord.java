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

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TxtRecord extends Record {
    private Map<String, String> attributes;

    @SuppressWarnings("unused")
    private final static Logger logger = LoggerFactory.getLogger(TxtRecord.class);

    public TxtRecord(ByteBuffer buffer, String name, Record.Class recordClass, long ttl, int length) {
        super(name, recordClass, ttl);
        List<String> strings = readStringsFromBuffer(buffer, length);
        attributes = parseDataStrings(strings);
    }

    private Map<String, String> parseDataStrings(List<String> strings) {
        Map<String, String> pairs = new HashMap<>();
        strings.stream().forEach(s -> {
            String[] parts = s.split("=");
            if (parts.length > 1) {
                pairs.put(parts[0], parts[1]);
            }
        });
        return pairs;
    }

    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    @Override
    public String toString() {
        return "TxtRecord{" +
                "name='" + name + '\'' +
                ", recordClass=" + recordClass +
                ", ttl=" + ttl +
                ", attributes=" + attributes +
                '}';
    }
}
