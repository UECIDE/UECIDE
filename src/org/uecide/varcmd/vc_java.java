package org.uecide.varcmd;

import org.uecide.*;
import java.io.File;

public class vc_java implements VariableCommand {
    public String main(Sketch sketch, String args) {
        return System.getProperty(args);
    }
}
