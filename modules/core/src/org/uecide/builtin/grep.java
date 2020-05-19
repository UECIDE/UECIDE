package org.uecide.builtin;

import org.uecide.*;
import java.io.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/* Search through a file looking for the first line that matches a regex capturing the
 * variables from it into keys
 *
 * Usage:
 *     __builtin_grep::file::regexp::key1.name[::key2.name...]
 */

public class grep extends BuiltinCommand {
    public grep(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {
        if (arg.length < 3) {
            throw new BuiltinCommandException("Syntax error");
        }

        try {
            File f = new File(arg[0]);
            String data = Utils.getFileAsString(f);
            String[] lines = data.split("\n");
            Pattern p = Pattern.compile(arg[1]);
            for (String line : lines) {
                Matcher m = p.matcher(line);
                if (m.find()) {
                    for (int i = 1; i <= m.groupCount(); i++) {
                        if (i + 1 < arg.length) {
                            ctx.set(arg[i + 1], m.group(i));
                        }
                    }
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            Debug.exception(ex);
        }
        return false;
    }

    public void kill() {
    }
}
