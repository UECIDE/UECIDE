package org.uecide.plugin;

import java.util.Stack;

public class RStack<E> extends Stack<E> {
    int maxSize = 100;

    public void setMaxSize(int s) {
        maxSize = s;
    }
    public E get(int i) {
        if (size() == 0) {
            return null;
        }
        return super.get(size() - i - 1);
    }

    public E push(E ob) {
        super.push(ob);
        while (size() > maxSize) {
            remove(0);
        }
        return ob;
    }
}

