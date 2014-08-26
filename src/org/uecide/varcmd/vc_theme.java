package org.uecide.varcmd;

import org.uecide.*;
import java.io.File;

public class vc_theme implements VariableCommand {
    public String main(Sketch sketch, String args) {
        return Base.theme.get(args);
    }
}
