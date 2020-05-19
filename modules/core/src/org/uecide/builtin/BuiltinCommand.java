package org.uecide.builtin;

import org.uecide.UECIDE;
import org.uecide.Context;
import org.uecide.Debug;

import java.util.TreeMap;
import java.util.Set;
import org.reflections.Reflections;
import java.lang.reflect.Constructor;
import java.lang.NoSuchMethodException;
import java.lang.InstantiationException;
import java.lang.reflect.InvocationTargetException;


public abstract class BuiltinCommand {
    public abstract boolean main(String[] arg) throws BuiltinCommandException ;
    public abstract void kill();

    static TreeMap<String, Class<? extends BuiltinCommand>> commandList = null;

    public Context ctx;
    public BuiltinCommand(Context ctx) {
        this.ctx = ctx;
    }

    public static void initBuiltinCommands() {
        commandList = new TreeMap<String, Class<? extends BuiltinCommand>>();
        Reflections ref = new Reflections("org.uecide.builtin");
        Set<Class<? extends BuiltinCommand>> classes = ref.getSubTypesOf(BuiltinCommand.class);

        for (Class<? extends BuiltinCommand> cl : classes) {
            String className = cl.getName();
            String[] parts = className.split("\\.");
            String commandName = parts[parts.length-1];
            commandList.put(commandName, cl);
        }
    }

    public static BuiltinCommand constructCommand(Class<? extends BuiltinCommand>cl, Context ctx) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Constructor<?> cons = cl.getConstructor(Context.class);
        BuiltinCommand a = (BuiltinCommand)cons.newInstance(ctx);
        return a;
    }

    public static boolean run(Context c, String cmdName, String[] arg) {
        try {
            Class<? extends BuiltinCommand> cl = commandList.get(cmdName.toLowerCase());
            if (cl == null) {
                c.error("Unknown command " + cmdName);
                return false;
            }
            BuiltinCommand command = constructCommand(cl, c);
            return command.main(arg);
        } catch (BuiltinCommandException ex) {
            c.error(ex);
            c.error("Command name: " + cmdName);
            c.error("Command parameters: ");
            for (Object o : arg) {
                c.error("    " + o.toString());
            }
        } catch (Exception ex) {
            c.error(ex);
        }
        return false;
    }

    public static boolean commandExists(String cmdName) {
        Class<? extends BuiltinCommand> cl = commandList.get(cmdName.toLowerCase());
        if (cl == null) {
            return false;
        }
        return true;
    }
}
