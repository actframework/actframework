package org.osgl.oms.controller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a class as Controller, which contains at least one of the following:
 * <ul>
 *     <li>Action handler method</li>
 *     <li>Any one of Before/After/Exception/Finally interceptor</li>
 * </ul>
 * <p>The framework will scan all Classes under the {@link org.osgl.oms.conf.AppConfigKey#CONTROLLER_PACKAGE}
 * or any class outside of the package but with this annotation marked</p>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Controller {
}
