package act.i18n;

import act.Act;
import org.osgl.$;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.S;

import java.util.*;

public class I18n {

    private static final Logger logger = LogManager.get(I18n.class);

    public static final String DEF_RESOURCE_BUNDLE_NAME = "messages";

    public static String i18n(String msgId, Object ... args) {
        return i18n(Act.appConfig().locale(), msgId, args);
    }

    public static String i18n(Locale locale, String msgId, Object ... args) {
        return i18n(locale, DEF_RESOURCE_BUNDLE_NAME, msgId, args);
    }

    public static String i18n(Class<?> bundleSpec, String msgId, Object... args) {
        return i18n(Act.appConfig().locale(), bundleSpec.getName(), msgId, args);
    }

    public static String i18n(Locale locale, Class<?> bundleSpec, String msgId, Object... args) {
        return i18n(locale, bundleSpec.getName(), msgId, args);
    }

    public static String i18n(Locale locale, String bundleName, String msgId, Object... args) {
        return i18n(false, locale, bundleName, msgId, args);
    }

    public static String i18n(boolean ignoreError, Locale locale, String bundleName, String msgId, Object... args) {
        if (null == msgId) {
            return "";
        }
        ResourceBundle bundle = ResourceBundle.getBundle(bundleName, $.notNull(locale));
        String msg = msgId;
        if (ignoreError) {
            if (bundle.containsKey(msgId)) {
                msg = bundle.getString(msgId);
            }
        } else {
            try {
                msg = bundle.getString(msgId);
            } catch (MissingResourceException e) {
                logger.warn("Cannot find i18n message key: %s", msgId);
            }
        }
        int len = args.length;
        if (len > 0) {
            Object[] resolvedArgs = new Object[len];
            for (int i = 0; i < len; ++i) {
                Object arg = args[i];
                if (arg instanceof String) {
                    resolvedArgs[i] = i18n(true, locale, bundleName, (String) arg);
                } else {
                    resolvedArgs[i] = arg;
                }
            }
            msg = S.fmt(msg, resolvedArgs);
        }
        return msg;
    }

    public static String i18n(Enum<?> msgId) {
        return i18n(Act.appConfig().locale(), msgId);
    }

    public static String i18n(Locale locale, Enum<?> msgId) {
        return i18n(locale, DEF_RESOURCE_BUNDLE_NAME, msgId);
    }

    public static String i18n(Class<?> bundleSpec, Enum<?> msgId) {
        return i18n(Act.appConfig().locale(), bundleSpec, msgId);
    }

    public static String i18n(Locale locale, Class<?> bundleSpec, Enum<?> msgId) {
        return i18n(locale, bundleSpec.getName(), msgId);
    }

    public static String i18n(Locale locale, String bundleName, Enum<?> msgId) {
        String key = S.builder("enum.").append(msgId.getDeclaringClass().getSimpleName().toLowerCase()).append(".").append(msgId.name().toLowerCase()).toString();
        return i18n(locale, bundleName, key);
    }

    public static Map<String, String> i18n(Class<? extends Enum> enumClass) {
        return i18n(Act.appConfig().locale(), enumClass);
    }

    public static Map<String, String> i18n(Locale locale, Class<? extends Enum> enumClass) {
        return i18n(locale, DEF_RESOURCE_BUNDLE_NAME, enumClass);
    }

    public static Map<String, String> i18n(Class<?> bundleSpec, Class<? extends Enum> enumClass) {
        return i18n(Act.appConfig().locale(), bundleSpec, enumClass);
    }

    public static Map<String, String> i18n(Locale locale, Class<?> bundleSpec, Class<? extends Enum> enumClass) {
        return i18n(locale, bundleSpec.getName(), enumClass);
    }

    public static Map<String, String> i18n(Locale locale, String bundleName, Class<? extends Enum> enumClass) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        for (Enum<?> enumInstance : enumClass.getEnumConstants()) {
            String name = enumInstance.name();
            String val = i18n(locale, bundleName, enumInstance);
            map.put(name, val);
        }
        return map;
    }

}
