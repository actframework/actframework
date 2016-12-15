package act.i18n;

import act.Act;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.S;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class I18n {

    private static final Logger logger = LogManager.get(I18n.class);

    public static final String DEF_RESOURCE_BUNDLE_NAME = "messages";

    public static String i18n(String msgId, Object ... args) {
        return i18n(Act.appConfig().locale(), DEF_RESOURCE_BUNDLE_NAME, msgId, args);
    }

    public static String i18n(Class<?> bundleSpec, String msgId, Object... args) {
        return i18n(Act.appConfig().locale(), bundleSpec.getName(), bundleSpec.getName(), msgId, args);
    }

    public static String i18n(Locale locale, String bundleName, String msgId, Object... args) {
        ResourceBundle bundle = ResourceBundle.getBundle(bundleName, locale);
        String msg = msgId;
        try {
            msg = bundle.getString(msgId);
        } catch (MissingResourceException e) {
            logger.warn("Cannot find i18n message key: %s", msgId);
        }
        if (args.length > 0) {
            msg = S.fmt(msg, args);
        }
        return msg;
    }

    public static String i18n(Enum<?> msgId, Object ... args) {
        return i18n(Act.appConfig().locale(), DEF_RESOURCE_BUNDLE_NAME, msgId, args);
    }

    public static String i18n(Class<?> bundleSpec, Enum<?> msgId, Object... args) {
        return i18n(Act.appConfig().locale(), bundleSpec.getName(), bundleSpec.getName(), msgId, args);
    }

    public static String i18n(Locale locale, String bundleName, Enum<?> msgId, Object... args) {
        String key = S.builder("enum.").append(msgId.getClass().getSimpleName().toLowerCase()).append(".").append(msgId.name().toLowerCase()).toString();
        return i18n(locale, bundleName, key, args);
    }

}
