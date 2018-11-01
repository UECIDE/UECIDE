package org.uecide;

public class KeyValuePair {
    Object key;
    Object value;

    public KeyValuePair(Object k, Object v) {
        key = k;
        value = v;
    }

    public Object getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    public String toString() {
        return value.toString();
    }
}
