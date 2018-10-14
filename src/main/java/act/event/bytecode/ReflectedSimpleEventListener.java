package act.event.bytecode;

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
import act.annotations.Order;
import act.app.App;
import act.event.EventBus;
import act.event.SimpleEventListener;
import act.inject.DependencyInjector;
import act.inject.param.ParamValueLoaderService;
import act.util.Ordered;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.util.*;

import java.lang.reflect.Method;
import java.util.List;

public class ReflectedSimpleEventListener implements SimpleEventListener, Ordered {

    private transient volatile Object host;

    private C.List<Class> paramTypes;
    private C.List<Class> providedParamTypes;
    private final Class<?> hostClass;
    private final Method method;
    private final int providedParamSize;
    private final boolean isStatic;
    private final boolean isAsync;
    private final int order;

    ReflectedSimpleEventListener(String className, String methodName, List<BeanSpec> paramTypes, boolean isStatic, boolean isAsync) {
        this.isStatic = isStatic;
        this.paramTypes = C.newList();
        this.providedParamTypes = C.newList();
        DependencyInjector injector = Act.injector();
        boolean cutOff = false;
        if (null == paramTypes) {
            paramTypes = C.list();
        }
        for (int i = paramTypes.size() - 1; i >= 0; --i) {
            BeanSpec spec = paramTypes.get(i);
            if (ParamValueLoaderService.provided(spec, injector)) {
                E.unexpectedIf(cutOff, "provided(injected) argument must be put at the end of passed in argument list");
                providedParamTypes.add(spec.rawType());
            } else {
                cutOff = true;
                this.paramTypes.add(spec.rawType());
            }
        }
        this.paramTypes = this.paramTypes.reverse();
        this.providedParamSize = this.providedParamTypes.size();
        if (providedParamSize > 0) {
            this.providedParamTypes = this.providedParamTypes.reverse();
        }
        Class[] argList = new Class[paramTypes.size()];
        for (int i = 0; i < argList.length; ++i) {
            argList[i] = paramTypes.get(i).rawType();
        }
        this.hostClass = Act.app().classForName(className);
        this.method = $.getMethod(hostClass, methodName, argList);
        this.isAsync = isAsync || EventBus.isAsync(hostClass) || EventBus.isAsync(method);
        Order order = this.hostClass.getAnnotation(Order.class);
        this.order = null == order ? Order.HIGHEST_PRECEDENCE : order.value();
    }

    @Override
    public boolean isAsync() {
        return isAsync;
    }

    @Override
    public int order() {
        return this.order;
    }

    @Override
    public void invoke(Object... args) {
        int paramNo = paramTypes.size();
        int argsNo = args.length;
        Object[] realArgs = args;
        if (paramNo != argsNo || providedParamSize > 0) {
            realArgs = new Object[paramNo + providedParamSize];
            System.arraycopy(args, 0, realArgs, 0, Math.min(paramNo, argsNo));
            App app = Act.app();
            for (int i = 0; i < providedParamSize; ++i) {
                realArgs[i + paramNo] = app.getInstance(providedParamTypes.get(i));
            }
        }
        Object host = host();
        if (null == host) {
            $.invokeStatic(method, realArgs);
        } else {
            $.invokeVirtual(host, method, realArgs);
        }
    }

    public List<Class> argumentTypes() {
        return paramTypes;
    }

    @Override
    public String toString() {
        return S.fmt("%s.%s(%s)", hostClass, method.getName(), S.strip(paramTypes, "[", "]"));
    }

    private Object host() {
        if (isStatic) {
            return null;
        } else {
            if (null == host) {
                synchronized (this) {
                    if (null == host) {
                        App app = App.instance();
                        host = app.getInstance(hostClass);
                    }
                }
            }
            return host;
        }
    }


}
