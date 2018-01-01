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

import act.Act;

import java.lang.annotation.*;

/**
 * Mark a request handler is subject to throttle control
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.METHOD)
public @interface Throttled {

    enum ExpireScale {
        /**
         * Delegate to the configuration setting of {@link act.conf.AppConfigKey#REQUEST_THROTTLE_EXPIRE_SCALE}
         */
        DEFAULT() {
            @Override
            public boolean enabled() {
                return Act.appConfig().requestThrottleExpireScale();
            }
        },
        /**
         * Enable request throttle reset timeout scale
         */
        ENABLED() {
            @Override
            public boolean enabled() {
                return true;
            }
        },
        /**
         * Disable request throttle reset timeout scale
         */
        DISABLED;

        public boolean enabled() {
            return false;
        }
    }

    /**
     * The maximum number of requests per second initiated from the same ip address.
     *
     * Default value:
     *
     * @return the request throttle number
     */
    int value() default -1;

    /**
     * Enable/disable throttle reset timeout scale.
     *
     * Default value: {@link ExpireScale#DEFAULT}
     * @return should we turn on request throttle reset timeout scale
     */
    ExpireScale expireScale() default ExpireScale.DEFAULT;
}
