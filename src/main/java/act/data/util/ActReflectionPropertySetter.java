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

import act.app.data.StringValueResolverManager;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.ReflectionPropertySetter;
import org.osgl.util.S;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ActReflectionPropertySetter extends ReflectionPropertySetter {

    private static Logger logger = LogManager.get(ActReflectionPropertySetter.class);

    private StringValueResolverManager resolverManager;

    public ActReflectionPropertySetter(Class c, Method m, Field f, StringValueResolverManager resolverManager) {
        super(c, m, f);
        this.resolverManager = resolverManager;
    }

    @Override
    protected Object convertValue(Class requiredClass, Object value) {
        if (null == value) {
            return null;
        }
        if (requiredClass.isAssignableFrom(value.getClass())) {
            return value;
        }
        Object retVal = resolverManager.resolve(S.string(value), requiredClass);
        if (null == retVal) {
            logger.warn("Cannot resolve value %s for class %s", value, requiredClass);
        }
        return retVal;
    }
}
