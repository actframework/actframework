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

import org.osgl.util.S;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a class or method to as "asynchronous"
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Async {
    /**
     * When a `public void` method has been annotated as `@Async`, ActFramework will
     * enhance the class by adding an new method as a copy of the original method, the name
     * of the new method will be the result of calling the original method.
     */
    class MethodNameTransformer {

        /**
         * Returns the new async method's name based on the give name
         * @param methodName the original method name
         * @return the name of the async method that pairs to the original method
         */
        public static String transform(String methodName) {
            return S.concat("__act_", methodName, "_async");
        }
    }
}
