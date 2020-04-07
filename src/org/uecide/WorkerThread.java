package org.uecide;

import java.util.Queue;

public class WorkerThread extends Thread {
    private static int instance = 0;
    private final Queue<Runnable> queue;

    private boolean running = false;
    Context ctx;

    public WorkerThread(Queue<Runnable> queue, Context c) {
        this.ctx = c;
        this.queue = queue;
        setName("Worker Thread " + (instance++));
    }

    @Override
    public void run() {
        while ( true ) {
            try {
                Runnable work = null;

                synchronized ( queue ) {
                    while ( queue.isEmpty() ) {
                        queue.wait();
                    }

                    // Get the next work item off of the queue
                    work = queue.remove();
                }

                running = true;
                work.run();
                running = false;
            }
            catch ( InterruptedException ie ) {
                ie.printStackTrace();
                break;  // Terminate
            }
        }
    }

    public boolean isRunning() {
        return running;
    }
}

