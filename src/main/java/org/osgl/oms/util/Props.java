package org.osgl.oms.util;

/**
 * provide utilties to access system properties
 */
public enum Props {
    ;
    public static String get(String key) {
        String val = System.getProperty(key);
        if (null == val) {
            val = System.getenv(key);
        }
        return val;
    }
}
