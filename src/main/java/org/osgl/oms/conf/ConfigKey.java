package org.osgl.oms.conf;

import java.util.List;
import java.util.Map;

/**
 * Defines ConfigKey properties
 */
public interface ConfigKey {
    String key();
    Object defVal();
    <T> T val(Map<String, ?> configuration);
    <T> List<T> implList(String key, Map<String, ?> configuration, Class<T> c);
}
