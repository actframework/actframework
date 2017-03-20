package act.data.util;

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

import act.app.App;
import org.osgl.util.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ActPropertyHandlerFactory extends ReflectionPropertyHandlerFactory {

    private ActObjectFactory objectFactory;
    private ActStringValueResolver stringValueResolver;

    public ActPropertyHandlerFactory(App app) {
        this.objectFactory = new ActObjectFactory(app);
        this.stringValueResolver = new ActStringValueResolver(app);
    }

    @Override
    protected PropertySetter newSetter(Class c, Method m, Field f) {
        return new ReflectionPropertySetter(objectFactory, stringValueResolver, c, m, f);
    }

    @Override
    protected PropertyGetter newGetter(Class c, Method m, Field f) {
        return new ReflectionPropertyGetter(objectFactory, stringValueResolver, c, m, f, this);
    }
}
