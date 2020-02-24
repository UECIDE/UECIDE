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

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Domain {
    private final String name;
    private final List<String> labels;

    public static final Domain LOCAL = new Domain("local.");

    private static final Pattern DOMAIN_PATTERN = Pattern.compile("((.*)_(tcp|udp)\\.)?(.*?)\\.?");

    public static Domain fromName(String name) {
        Matcher matcher = DOMAIN_PATTERN.matcher(name);
        if (matcher.matches()) {
            return new Domain(matcher.group(4));
        } else {
            throw new IllegalArgumentException("Name does not match domain syntax");
        }
    }

    private Domain(String name) {
        this.name = name;
        labels = Arrays.asList(name.split("\\."));
    }

    public String getName() {
        return name;
    }

    public List<String> getLabels() {
        return labels;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Domain domain = (Domain) o;

        return name.equals(domain.name);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "Domain{" +
                "name='" + name + '\'' +
                ", labels=" + labels +
                '}';
    }
}
