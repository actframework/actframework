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

import java.lang.annotation.Annotation;

public abstract class DescendantClassFilter<SUPER_TYPE> extends ClassFilter<SUPER_TYPE, Annotation> {

    public DescendantClassFilter(Class<SUPER_TYPE> superType) {
        super(superType, null);
    }

    public DescendantClassFilter(boolean publicOnly, boolean noAbstract, Class<SUPER_TYPE> superType) {
        super(publicOnly, noAbstract, superType, null);
    }
}
