package org.uecide;

import java.util.Queue;

public class WorkerThread extends Thread {
    private static int instance = 0;
    private final Queue<QueueJob> queue;

    private boolean running = false;
    Context ctx;
    QueueJob work = null;

    public WorkerThread(Queue<QueueJob> queue, Context c) {
        this.ctx = c;
        this.queue = queue;
        setName("Worker Thread " + (instance++));
    }

    @Override
    public void run() {
        while ( true ) {
            try {
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

    public void kill() {
        if (running) {
            if (work != null) {
                work.kill();
            }
        }
    }
}

