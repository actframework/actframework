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
 * The annotation specify a field or parameter should be a config file
 * that is loaded from path specified.
 *
 * The difference between `@LoadConfig` and `@LoadResource` is
 * specified at https://github.com/actframework/actframework/issues/420
 */
@LoadValue(ConfigResourceLoader.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@ParamBindingAnnotation
@InjectTag
public @interface LoadConfig {
    /**
     * Specify the config resource name.
     *
     * @return the config resource name
     */
    String value();
}
