package org.uecide;

import java.util.*;
import java.text.*;
import org.apache.commons.text.WordUtils;

public class I18N {
    public static Locale locale;
    public static ResourceBundle bundle;
    public static MessageFormat formatter;

    static String source = null;

    public I18N(String f) {
        source = f;
        locale = Base.getLocale();
        bundle = ResourceBundle.getBundle("org/uecide/i18n/" + f + "/MessageBundle", locale);
        formatter = new MessageFormat("");
        formatter.setLocale(locale);
    }

    public static String string(String key, Object o1) {
        Object[] args = { o1 };
        return WordUtils.wrap(string(key, args), 50);
    }

    public static String string(String key, Object o1, Object o2) {
        Object[] args = { o1, o2 };
        return WordUtils.wrap(string(key, args), 50);
    }

    public static String string(String key, Object o1, Object o2, Object o3) {
        Object[] args = { o1, o2, o3 };
        return WordUtils.wrap(string(key, args), 50);
    }

    public static String string(String key, Object o1, Object o2, Object o3, Object o4) {
        Object[] args = { o1, o2, o3, o4 };
        return WordUtils.wrap(string(key, args), 50);
    }

    public static String string(String key, Object[] args) {
        formatter.applyPattern(string(key));
        return WordUtils.wrap(formatter.format(args), 50);
    }

    public static String string(String key) {
        try {
            return WordUtils.wrap(bundle.getString(key), 50);
        } catch (Exception e) {
            return "{" + source + "/" + key + "}";
        }
    }
}
