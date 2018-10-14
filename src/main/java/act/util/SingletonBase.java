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

import org.osgl.util.E;

/**
 * Application could extend this class to create singleton classes.
 * <p>
 *     Note, the sub type must NOT be abstract and has public constructor
 * </p>
 */
public abstract class SingletonBase extends LogSupportedDestroyableBase {

    /**
     * Returns the singleton instance of the sub type
     * @param <T> the sub type of {@code SingletonBase}
     * @return the instance
     */
    public static <T> T instance() {
        throw E.tbd("This method will be enhanced on sub type of SingletonBase class");
    }

}
