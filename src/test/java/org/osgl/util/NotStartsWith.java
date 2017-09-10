package org.osgl.util;

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

import org.hamcrest.Description;
import org.mockito.ArgumentMatcher;

import java.io.Serializable;

public class NotStartsWith extends ArgumentMatcher<String> implements Serializable {
    private static final long serialVersionUID = -5978092285707998431L;
    private final String prefix;

    public NotStartsWith(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public boolean matches(Object actual) {
        return actual != null && !((String)actual).startsWith(this.prefix);
    }

    public void describeTo(Description description) {
        description.appendText("notStartsWith(\"" + this.prefix + "\")");
    }
}
