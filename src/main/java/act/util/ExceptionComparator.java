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

import java.util.Comparator;

/**
 * Used to sort Exception based on the inheritance hierarchy
 */
public class ExceptionComparator implements Comparator<Class<? extends Exception>> {
    @Override
    public int compare(Class<? extends Exception> o1, Class<? extends Exception> o2) {
        return hierarchicalLevel(o2) - hierarchicalLevel(o1);
    }

    private static int hierarchicalLevel(Class<? extends Exception> e) {
        int i = 0;
        Class<?> c = e;
        while (null != c) {
            i++;
            c = c.getSuperclass();
        }
        return i;
    }
}
