package org.osgl.oms.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark the extended class. This is used by plugin class which does not
 * extends a certain class directly.
 * <p>For example, suppose a plugin developer created a class
 * {@code MyProjLayoutProbe} extends {@link org.osgl.oms.app.ProjectLayoutProbe},
 * the class could be sensed by OMS directly; however if the developer decide
 * extend {@code MyProjLayoutProbe} and create another class
 * {@code MySubProjLayoutProbe} then this {@code Extends @Extends}
 * annotation needs to be used to mark on the new class. Otherwise, the plugin detector
 * will not be able to detect the second class that does not extends the
 * {@link org.osgl.oms.app.ProjectLayoutProbe} class directly
 * </p>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Extends {
    /**
     * Mark the class that the underline type will extends directly or
     * indirectly
     */
    Class<?> value();
}
