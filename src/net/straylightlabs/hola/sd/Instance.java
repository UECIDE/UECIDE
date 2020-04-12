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

import net.straylightlabs.hola.dns.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.*;
import java.util.stream.Collectors;

public class Instance {
    private final String name;
    private final Set<InetAddress> addresses;
    private final int port;
    private final Map<String, String> attributes;

    private final static Logger logger = LoggerFactory.getLogger(Instance.class);

    static Instance createFromRecords(PtrRecord ptr, Set<Record> records) {
        String name = ptr.getUserVisibleName();
        int port;
        List<InetAddress> addresses = new ArrayList<>();
        Map<String, String> attributes = Collections.emptyMap();

        Optional<SrvRecord> srv = records.stream()
                .filter(r -> r instanceof SrvRecord && r.getName().equals(ptr.getPtrName()))
                .map(r -> (SrvRecord) r).findFirst();
        if (srv.isPresent()) {
            logger.debug("Using SrvRecord {} to create instance for {}", srv, ptr);
            port = srv.get().getPort();
            addresses.addAll(records.stream().filter(r -> r instanceof ARecord)
                    .filter(r -> r.getName().equals(srv.get().getTarget())).map(r -> ((ARecord) r).getAddress())
                    .collect(Collectors.toList()));
            addresses.addAll(records.stream().filter(r -> r instanceof AaaaRecord)
                    .filter(r -> r.getName().equals(srv.get().getTarget())).map(r -> ((AaaaRecord) r).getAddress())
                    .collect(Collectors.toList()));
        } else {
            throw new IllegalStateException("Cannot create Instance when no SRV record is available");
        }
        Optional<TxtRecord> txt = records.stream()
                .filter(r -> r instanceof TxtRecord && r.getName().equals(ptr.getPtrName()))
                .map(r -> (TxtRecord) r).findFirst();
        if (txt.isPresent()) {
            logger.debug("Using TxtRecord {} to create attributes for {}", txt, ptr);
            attributes = txt.get().getAttributes();
        }
        return new Instance(name, addresses, port, attributes);
    }

    public Instance(String name, List<InetAddress> addresses, int port, Map<String, String> attributes) {
        this.name = name;
        this.addresses = new HashSet<>();
        this.addresses.addAll(addresses);
        this.port = port;
        this.attributes = attributes;
    }

    /**
     * Get the user-visible name associated with this instance.
     * <p>
     * This value comes from the instance's PTR record.
     *
     * @return name
     */
    @SuppressWarnings("unused")
    public String getName() {
        return name;
    }

    /**
     * Get the set of IP addresses associated with this instance.
     * <p>
     * These values come from the instance's A and AAAA records.
     *
     * @return set of addresses
     */
    @SuppressWarnings("unused")
    public Set<InetAddress> getAddresses() {
        return Collections.unmodifiableSet(addresses);
    }

    /**
     * Get the port number associated with this instance.
     * <p>
     * This value comes from the instance's SRV record.
     *
     * @return port number
     */
    @SuppressWarnings("unused")
    public int getPort() {
        return port;
    }

    /**
     * Check whether this instance has the specified attribute.
     * <p>
     * Attributes come from the instance's TXT records.
     *
     * @param attribute name of the attribute to search for
     * @return true if the instance has a value for attribute, false otherwise
     */
    @SuppressWarnings("unused")
    public boolean hasAttribute(String attribute) {
        return attributes.containsKey(attribute);
    }

    /**
     * Get the value of the specified attribute.
     * <p>
     * Attributes come from the instance's TXT records.
     *
     * @param attribute name of the attribute to search for
     * @return value of the given attribute, or null if the attribute doesn't exist in this Instance
     */
    @SuppressWarnings("unused")
    public String lookupAttribute(String attribute) {
        return attributes.get(attribute);
    }

    @SuppressWarnings("unused")
    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return "Instance{" +
                "name='" + name + '\'' +
                ", addresses=" + addresses +
                ", port=" + port +
                ", attributes=" + attributes +
                '}';
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + getName().hashCode();
        result = 31 * result + getPort();
        for (InetAddress address : getAddresses()) {
            result = 31 * result + address.hashCode();
        }
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            result = 31 * result + entry.hashCode();
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Instance)) {
            return false;
        }
        Instance other = (Instance) obj;
        if (!getName().equals(other.getName())) {
            return false;
        }
        if (getPort() != other.getPort()) {
            return false;
        }
        for (InetAddress address : getAddresses()) {
            if (!other.getAddresses().contains(address)) {
                return false;
            }
        }
        for (InetAddress address : other.getAddresses()) {
            if (!getAddresses().contains(address)) {
                return false;
            }
        }
        for (String key : attributes.keySet()) {
            if (!other.hasAttribute(key) || !other.lookupAttribute(key).equals(lookupAttribute(key))) {
                return false;
            }
        }
        for (String key : other.attributes.keySet()) {
            if (!hasAttribute(key) || !lookupAttribute(key).equals(other.lookupAttribute(key))) {
                return false;
            }
        }
        return true;
    }
}
