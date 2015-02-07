package org.osgl.oms.plugin;

import org.osgl._;
import org.osgl.oms.util.ClassFilter;

/**
 * Tag a class that could be plug into OMS stack
 */
public interface Plugin {
    void plugin();
    public static final ClassFilter<Plugin> CLASS_FILTER = new ClassFilter<Plugin>() {
        @Override
        public void found(Class<? extends Plugin> clazz) {
            _.newInstance(clazz).plugin();
        }

        @Override
        public Class<Plugin> superType() {
            return Plugin.class;
        };
    };
}
