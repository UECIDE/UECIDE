package org.uecide;

public class JSAction {
    public String icon;
    public String function;
    public String tooltip;
    public JSPlugin plugin;

    public JSAction(String i, String f, String t, JSPlugin p) {
        icon = i;
        function = f;
        tooltip = t;
        plugin = p;
    }

    public Object activate(Editor ed) {
        return plugin.call(function, null, ed, null);
    }
}
