package org.uecide;

import java.util.*;
import java.text.*;

public class I18N {
    public static Locale locale;
    public static ResourceBundle bundle;
    public static MessageFormat formatter;

    static String source = null;

    public I18N(String f) {
        source = f;
        locale = UECIDE.getLocale();
        bundle = ResourceBundle.getBundle("org/uecide/i18n/" + f + "/MessageBundle", locale);
        formatter = new MessageFormat("");
        formatter.setLocale(locale);
    }

    public static String string(String key, Object o1) {
        Object[] args = { o1 };
        return string(key, args);
    }

    public static String string(String key, Object o1, Object o2) {
        Object[] args = { o1, o2 };
        return string(key, args);
    }

    public static String string(String key, Object o1, Object o2, Object o3) {
        Object[] args = { o1, o2, o3 };
        return string(key, args);
    }

    public static String string(String key, Object o1, Object o2, Object o3, Object o4) {
        Object[] args = { o1, o2, o3, o4 };
        return string(key, args);
    }

    public static String string(String key, Object[] args) {
        formatter.applyPattern(bundle.getString(key));
        return formatter.format(args);
    }

    public static String string(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            Debug.exception(e);
            return "{" + source + "/" + key + "}";
        }
    }
}
