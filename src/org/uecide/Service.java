package org.uecide;

public abstract class Service implements Runnable {
    int interval;
    Thread myThread;
    String name;

    boolean running = false;
    boolean active = false;

    public Thread start() {
        running = true;
        myThread = new Thread(this);
        myThread.start();
        return myThread;
    }

    public void stop() {
        stop(false);
    }

    public void stop(boolean wait) {
        running = false;
        if (wait) {
            while (active) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                }
                continue;
            }
        }
    }

    public Thread restart() {
        stop(true);
        return start();
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int i) {
        interval = i;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isActive() {
        return active;
    }

    public void setName(String n) {
        name = n;
    }

    public String getName() {
        return name;
    }

    // Heh, let's model it after the Arduino structure.
    // I mean, why not, eh?

    public void run() {
        active = true;
        System.err.println("Service '" + getName() + "' started");
        setup();
        while (running) {
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                continue;
            }
            loop();
        }
        cleanup();
        System.err.println("Service '" + getName() + "' stopped");
        active = false;
    }

    public String getKey() {
        String className = null;
        Class<?> enclosingClass = getClass().getEnclosingClass();
        if (enclosingClass != null) {
            className = enclosingClass.getName();
        } else {
            className = getClass().getName();
        }

        className = className.substring(className.lastIndexOf(".") + 1);
        return className.toLowerCase();
    }

    public boolean isAutoStart() {
        return Base.preferences.getBoolean("service." + getKey() + ".autostart");
    }

    public void setAutoStart(boolean b) {
        Base.preferences.setBoolean("service." + getKey() + ".autostart", b);
        Base.preferences.saveDelay();
    }

    public abstract void setup();
    public abstract void loop();
    public abstract void cleanup();
}

