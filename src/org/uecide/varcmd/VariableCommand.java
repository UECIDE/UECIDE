package org.uecide.varcmd;

import org.uecide.*;

public abstract class VariableCommand {
    public abstract String main(Context context, String args) throws VariableCommandException;

    public static String run(Context ctx, String command, String param) {
        VariableCommand vc = null;
        try {

            switch(command) {
                case "arduino": vc = new vc_arduino(); break;
                case "basename": vc = new vc_basename(); break;
                case "board": vc = new vc_board(); break;
                case "char": vc = new vc_char(); break;
                case "compiler": vc = new vc_compiler(); break;
                case "core": vc = new vc_core(); break;
                case "env": vc = new vc_env(); break;
                case "exec": vc = new vc_exec(); break;
                case "files": vc = new vc_files(); break;
                case "find": vc = new vc_find(); break;
                case "foreach": vc = new vc_foreach(); break;
                case "git": vc = new vc_git(); break;
                case "if": vc = new vc_if(); break;
                case "java": vc = new vc_java(); break;
                case "join": vc = new vc_join(); break;
                case "math": vc = new vc_math(); break;
                case "onefile": vc = new vc_onefile(); break;
                case "option": vc = new vc_option(); break;
                case "os": vc = new vc_os(); break;
                case "port": vc = new vc_port(); break;
                case "prefs": vc = new vc_prefs(); break;
                case "preproc": vc = new vc_preproc(); break;
                case "programmer": vc = new vc_programmer(); break;
                case "random": vc = new vc_random(); break;
                case "replace": vc = new vc_replace(); break;
                case "select": vc = new vc_select(); break;
                case "sketch": vc = new vc_sketch(); break;
                case "system": vc = new vc_system(); break;
                case "tool": vc = new vc_tool(); break;
                case "uecide": vc = new vc_uecide(); break;
                case "exists": vc = new vc_exists(); break;
                case "ucase": vc = new vc_ucase(); break;
                case "lcase": vc = new vc_lcase(); break;
                case "csv": vc = new vc_csv(); break;
                default: return "BADVCMD";
            }

            String ret = vc.main(ctx, param);
            return ret;
        } catch (VariableCommandException ex) {
            Debug.exception(ex);
            ctx.error(ex);
            ctx.error(vc.toString());
        } catch (Exception ex) {
            Debug.exception(ex);
            ctx.error(ex);
        }

        return "";
    }

}
