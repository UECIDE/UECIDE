package org.uecide.builtin;

import org.uecide.*;
import java.io.*;

public class gpio extends BuiltinCommand {
    public gpio(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {
        try {
            int pin = Utils.s2i(arg[0]);
            String command = arg[1];

            if (command.equals("high")) {
                exportPin(pin);
                setMode(pin, "out");
                setValue(pin, 1);
            } else if (command.equals("low")) {
                exportPin(pin);
                setMode(pin, "out");
                setValue(pin, 0);
            } else if (command.equals("hiz")) {
                exportPin(pin);
                setMode(pin, "in");
            }
        } catch (Exception e) {
            Debug.exception(e);
            throw new BuiltinCommandException(e.getMessage());
        }
        return true;
    }

    void exportPin(int pin) {
        try {
            File export = new File("/sys/class/gpio/export");
            if (!export.exists()) {
                return;
            }
            PrintWriter pw = new PrintWriter(export);
            pw.println(pin);
            pw.close();
        } catch (Exception e) {
            Debug.exception(e);
        }
    }

    void setMode(int pin, String dir) {
        try {
            String path = "/sys/class/gpio/gpio" + pin;
            File direction = new File(path, "direction");
            if (!direction.exists()) {
                return;
            }

            PrintWriter pw = new PrintWriter(direction);
            pw.println(dir);
            pw.close();
        } catch (Exception e) {
            Debug.exception(e);
        }
    }

    void setValue(int pin, int val) {
        try {
            String path = "/sys/class/gpio/gpio" + pin;
            File value = new File(path, "value");
            if (!value.exists()) {
                return;
            }

            PrintWriter pw = new PrintWriter(value);
            pw.println(value);
            pw.close();
        } catch (Exception e) {
            Debug.exception(e);
        }
    }

    public void kill() {
    }
}
