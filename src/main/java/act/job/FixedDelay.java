package act.job;

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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a method to be a job called at fixed delay time
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface FixedDelay {
    /**
     * Specifies the delayed time to execute the method. Time could be specified in
     * days, hours, minutes and seconds. For example:
     * <ul>
     *     <li>2d - 2 days</li>
     *     <li>3h - 3 hours</li>
     *     <li>5mn - 5 minutes</li>
     *     <li>1s - 1 second</li>
     * </ul>
     * <p>Note, combination time specification is not supported. For example
     * {@code "1d 5h 30mn"} will be considered to be illegal argument</p>
     * <p>If this option is miss specified, then the default value will be
     * set to {@code 1s} one second</p>
     * <p>The value shall not be zero or negative, otherwise IllegalArgumentException
     * will be thrown out</p>
     * <p>Default value: {@code 60s}</p>
     */
    String value() default "60s";

    /**
     * Specify the ID of the scheduled job. Default value: empty string
     * @return the job id
     */
    String id() default "";

    /**
     * Specify the job shall start immediately after app started.
     *
     * Default value is `true`.
     *
     * @return whether the job shall start immediately after the app started
     */
    boolean startImmediately() default true;
}
