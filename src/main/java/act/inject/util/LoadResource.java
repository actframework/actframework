package act.inject.util;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.data.annotation.ParamBindingAnnotation;
import org.osgl.inject.annotation.InjectTag;
import org.osgl.inject.annotation.LoadValue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation specify a field or parameter should be a resource
 * that is loaded from path specified.
 *
 * @see act.util.HeaderMapping
 */
@LoadValue(ResourceLoader.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@ParamBindingAnnotation
@InjectTag
public @interface LoadResource {
    /**
     * Specify the resource path.
     *
     * The resource will be loaded by calling
     *
     * ```java
     * Act.app().classLoader().getResource(value);
     * ```
     *
     * By default the leading slash in the `value` will be removed, i.e.
     * `@LoadResource("/folder/file.txt")` is the same as
     * `@LoadResource("folder/file.txt")`. This behavior can be turned
     * off by specifying {@link #skipTrimLeadingSlash()} to `false`:
     *
     * ```java
     * {@literal @}LoadResource(value: "/folder/file.txt", skipTrimLeadingSlash: false);
     * ```
     *
     * @return the resource path
     */
    String value();

    /**
     * By default it will remove the leading `/` from the
     * {@link #value()}. Setting this option to `true` can
     * turn off the behavior.
     *
     * Default value is `false`
     *
     * @return `true` if it shall skip removing the leading
     * slash from {@link #value()}
     */
    boolean skipTrimLeadingSlash() default false;
}
