package act.db;

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
import act.db.di.FindBy;
import org.osgl.inject.annotation.InjectTag;
import org.osgl.inject.annotation.LoadValue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation specify a field or parameter should be retrieved from
 * database through certain binding key with value get from the current
 * {@link act.util.ActContext}
 */
@LoadValue(FindBy.class)
@InjectTag
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@ParamBindingAnnotation
public @interface DbBind {
    /**
     * Specifies the request parameter name. Default value is an
     * empty string meaning the bind name coming from
     * {@literal @}Named annotation, or field name or param name) will
     * be used as the request parameter name.
     *
     * @return the parameter binding name
     */
    String value() default "";

    /**
     * Specify the mapping property name used to query database.
     * Default value is an empty string meaning use the {@link #value() binding name}
     * as the property name.
     *
     * @return the property name used to query the database
     */
    String field() default "";

    /**
     * Indicate if it shall use the resolved value to search for ID or normal field
     *
     * **Note** this setting has no effect when any one of the following follow case is `true`
     *
     * 1. the parameter type or field type is collection or iterable
     * 2. {@link #field()} setting is not blank
     *
     * @return `true` if it shall bind by ID field, `false` otherwise
     */
    boolean byId() default true;

    /**
     * If {@link #byId()} is `false` then developer can use this field
     * to specify the type of the search field
     * @return the type of the search field
     */
    Class fieldType() default String.class;
}
