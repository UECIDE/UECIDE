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

package net.straylightlabs.hola.sd;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Service {
    private final String name;
    private final List<String> labels;

    private static final Pattern SERVICE_PATTERN = Pattern.compile("^((_[a-zA-Z0-9_\\-]+\\.)?_(tcp|udp))\\.?|$");

    public static Service fromName(String name) {
        Matcher matcher = SERVICE_PATTERN.matcher(name);
        if (matcher.find()) {
            return new Service(matcher.group(1));
        } else {
            throw new IllegalArgumentException("Name does not match service syntax");
        }
    }

    private Service(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("A Service's name can't be null or empty");
        }

        this.name = name;
        labels = Arrays.asList(name.split("\\."));
    }

    public String getName() {
        return name;
    }

    public List<String> getLabels() {
        return Collections.unmodifiableList(labels);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Service service = (Service) o;

        return name.equals(service.name);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "Service{" +
                "name='" + name + '\'' +
                ", labels=" + labels +
                '}';
    }
}
