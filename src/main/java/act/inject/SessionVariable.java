package act.inject;

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

import act.cli.CliSession;
import org.osgl.inject.annotation.InjectTag;
import org.osgl.inject.annotation.LoadValue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.inject.Qualifier;

/**
 * Indicate a binding from a {@link CliSession#attribute(String) CLI session variable} to
 * a commander field or parameter;
 *
 * Or binding from a {@link org.osgl.http.H.Session#get(String)} to a controller field or action handler
 * parameter
 */
@Qualifier
@InjectTag
@LoadValue(SessionValueLoader.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface SessionVariable {

    /**
     * Specifies the session attribute key.
     *
     * If not specified then use the parameter name or field name as session attribute key
     *
     * @return the session attribute key
     */
    String value() default "";
}
