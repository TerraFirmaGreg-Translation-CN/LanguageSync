package io.github.tfgcn.langsync;

import java.util.Locale;
import java.util.ResourceBundle;

public final class I18n {
    private static ResourceBundle bundle;

    static {
        setLocale(Locale.getDefault());
    }

    private I18n() {
    }

    public static void setLocale(Locale locale) {
        try {
            bundle = ResourceBundle.getBundle("message", locale);
        } catch (Exception e) {
            bundle = ResourceBundle.getBundle("message", Locale.ROOT);
        }
    }

    public static String getString(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return "!" + key + "!";
        }
    }

    public static String getString(String key, Object... args) {
        try {
            String pattern = bundle.getString(key);
            return String.format(pattern, args);
        } catch (Exception e) {
            return "!" + key + "!";
        }
    }
}