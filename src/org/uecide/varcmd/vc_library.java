package org.uecide.varcmd;

import org.uecide.Context;
import org.uecide.Library;

public class vc_library extends VariableCommand {
    public String main(Context ctx, String args) throws VariableCommandException {

        if (args.equals("paths")) {
            return Library.getLibraryRootPaths(ctx.getCore().getName());
        }
        throw new VariableCommandException("Unknown Property: " + args);
    }
}
