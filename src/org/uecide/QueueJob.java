package org.uecide;

public abstract class QueueJob implements Runnable {

    protected Context ctx;
    protected int state;

    public static final int NEW = 0;
    public static final int RUNNING = 1;
    public static final int COMPLETED = 2;
    public static final int FAILED = 3;

    public QueueJob(Context c) {
        ctx = c;
        state = NEW;
    }

    public abstract void run();

    public int getState() {
        return state;
    }

    public abstract void kill();
}
