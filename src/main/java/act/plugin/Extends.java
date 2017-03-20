package act.plugin;

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

import act.app.ProjectLayoutProbe;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark the extended class. This is used by plugin class which does not
 * extends a certain class directly.
 * <p>For example, suppose a plugin developer created a class
 * {@code MyProjLayoutProbe} extends {@link ProjectLayoutProbe},
 * the class could be sensed by Act directly; however if the developer decide
 * extend {@code MyProjLayoutProbe} and create another class
 * {@code MySubProjLayoutProbe} then this {@code Extends @Extends}
 * annotation needs to be used to mark on the new class. Otherwise, the plugin detector
 * will not be able to detect the second class that does not extends the
 * {@link ProjectLayoutProbe} class directly
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
