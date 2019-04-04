package act.apidoc;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
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

import org.osgl.$;

import java.lang.reflect.Method;

public class SimpleEndpointIdProvider implements EndpointIdProvider {

    private Class<?> controllerClass;
    private Method method;

    private EndpointIdProvider parent;

    public SimpleEndpointIdProvider(Class<?> controllerClass, Method method) {
        this.controllerClass = $.requireNotNull(controllerClass);
        this.method = $.requireNotNull(method);
        if (controllerClass != method.getDeclaringClass()) {
            parent = new SimpleEndpointIdProvider(method.getDeclaringClass(), method);
        }
    }

    @Override
    public String getId() {
        return id(controllerClass, method);
    }

    @Override
    public String getParentId() {
        return null == parent ? null : parent.getId();
    }

    static String id(Class<?> controllerClass, Method method) {
        return controllerClass.getName() + "." + method.getName();
    }

    public static String className(Class<?> clz) {
        Class<?> enclosing = clz.getEnclosingClass();
        if (null != enclosing) {
            return className(enclosing) + "." + clz.getSimpleName();
        }
        return clz.getSimpleName();
    }
}
