package org.uecide.varcmd;

import org.uecide.*;

public abstract interface VariableCommand {
    public abstract String main(Context context, String args);
}
