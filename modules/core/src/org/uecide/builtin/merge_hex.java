package org.uecide.builtin;

import org.uecide.*;
import java.io.*;

import uk.co.majenko.hexfile.*;

/* Merge multiple hex files into one
 *
 * Usage:
 *     __builtin_merge_hex::output.hex::file1.hex[::file2.hex...]
 */

public class merge_hex extends BuiltinCommand {
    public merge_hex(Context c) { super(c); }

    public boolean main(String[] arg) throws BuiltinCommandException {
        try {
            if (arg.length < 2) {
                throw new BuiltinCommandException("Syntax Error");
            }

            HexFile hex = new HexFile();
            for (int i = 1; i < arg.length; i++) {
                hex.loadFile(new File(arg[i]));
            }
            hex.saveFile(new File(arg[0]));

            return true;
        } catch (Exception ex) { 
            Debug.exception(ex);
            throw new BuiltinCommandException(ex.getMessage());
        }
    }

    public void kill() {
    }
}
