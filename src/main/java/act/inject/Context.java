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

import java.lang.annotation.*;

/**
 * This annotation is used to inject information into a class
 * field, bean property or method parameter.
 *
 * <p>
 *     Refer to <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/Context.html">javax.ws.rs.core.Context</a>
 * </p>
 *
 * @deprecated use `org.osgl.inject.annotation.Provided` instead
 * @see org.osgl.inject.annotation.Provided
 */
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Deprecated
public @interface Context {
}
