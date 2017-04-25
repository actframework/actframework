package act.util;

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
 * Mark an action handler method result can be cached
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CacheFor {
    /**
     * Specify cache expiration time in seconds
     *
     * Default value: `60 * 60`, i.e. one hour
     *
     * @return the expiration time
     */
    int value() default 60 * 60;

    /**
     * Specify whether this cache is session based
     *
     * **Note** use session based cache with cautious because it might
     * result in very big memory consumption when you have a lot of
     * sessions
     *
     * @return `true` if this cache should be session based
     */
    boolean sessionBased() default false;

    /**
     * Specify the keys to extract parameter/post variables to build the final
     * cache key
     *
     * If not supplied then framework will try to iterate through all query
     * parameters to build the cache key
     *
     * @return the keys that should be used to build the final cache key
     */
    String[] keys() default {};

    /**
     * In some edge case (e.g. facebook post to app to get the landing page) POST
     * is treated as GET, we should allow cache the result in that case
     *
     * @return `true` if enable cache on POST request
     */
    boolean supportPost() default false;
}
