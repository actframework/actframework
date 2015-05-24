package act.util;

/**
 * provide utilities to access system properties
 */
public enum SysProps {
    ;

    public static String get(String key) {
        String val = System.getProperty(key);
        if (null == val) {
            val = System.getenv(key);
        }
        return val;
    }
}
