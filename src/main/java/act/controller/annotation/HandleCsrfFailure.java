package act.controller.annotation;

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
 * Specify how to handle the case when CSRF checking failed
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.TYPE)
public @interface HandleCsrfFailure {

    /**
     * Specify the option to handle CSRF checking failed
     * @return the way to deal with CSRF checking failed case
     */
    HandleMissingAuthentication.Option value();

    /**
     * Specify the custom implementation - only effective when
     * {@link #value()} is {@link HandleMissingAuthentication.Option#CUSTOM}
     *
     * @return the custom implementation
     */
    String custom() default "";
}
