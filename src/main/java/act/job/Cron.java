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
 * Mark a method to be a cron job. The method must be public and has no parameters. If
 * the method is virtual then the class declared the method must have a public constructor
 * without parameter
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Cron {
    /**
     * Specifies cron expression or a configuration key starts with {@code "cron."}
     */
    String value();

    /**
     * Specify the ID of the scheduled job. Default value: empty string
     * @return the job id
     */
    String id() default "";

    String CRON_12AM = "0 0 0 * * *";
    String CRON_MIDNIGHT = CRON_12AM;
    String CRON_2AM = "0 0 2 * * *";
    String CRON_8AM = "0 0 8 * * *";
    String CRON_MORNING = CRON_8AM;
    String CRON_12PM = "0 0 12 * * *";
    String CRON_NOON = CRON_12PM;
    String CRON_6PM = "0 0 18 * * *";
    String CRON_10PM = "0 0 22 * * *";
}
