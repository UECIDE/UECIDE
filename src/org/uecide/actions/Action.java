package org.uecide.actions;

import org.uecide.*;

import java.util.TreeMap;
import java.util.Set;
import org.reflections.Reflections;
import java.lang.reflect.Constructor;
import java.lang.NoSuchMethodException;
import java.lang.InstantiationException;
import java.lang.reflect.InvocationTargetException;

public abstract class Action {

    public Context ctx;

    static TreeMap<String, Class<? extends Action>> actionList = null;

    public Action(Context c) {
        ctx = c;
    }

    public static TreeMap<String, Class<? extends Action>> getActions() {
        return actionList;
    }

    public static void initActions() {
        actionList = new TreeMap<String, Class<? extends Action>>();
        Reflections ref = new Reflections("org.uecide.actions");
        Set<Class<? extends Action>> classes = ref.getSubTypesOf(Action.class);

        for (Class<? extends Action> cl : classes) {
            try {
                Action a = constructAction(cl, (Context)null);
                String cmd = a.getCommand();
                actionList.put(cmd, cl);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static Action constructAction(Class<? extends Action>cl, Context ctx) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Constructor<?> cons = cl.getConstructor(Context.class);
        Action a = (Action)cons.newInstance(ctx);
        return a;
    }

    public abstract boolean actionPerformed(Object[] args) throws ActionException;

    public static boolean run(Context c, String name, Object[] args) {
        try {
            Class<? extends Action> cl = actionList.get(name.toLowerCase());
            if (cl == null) {
                c.error("Unknown action " + name);
                return false;
            }
            Action action = constructAction(cl, c);
            return action.actionPerformed(args);
        } catch (ActionException ex) {
            c.error(ex);
        } catch (Exception ex) {
            c.error(ex);
        }
        return false;
    }

    public abstract String[] getUsage();
    public abstract String getCommand();

}
