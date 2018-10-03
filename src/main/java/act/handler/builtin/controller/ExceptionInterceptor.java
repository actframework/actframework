package act.handler.builtin.controller;

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

import act.app.ActionContext;
import act.plugin.Plugin;
import act.security.CORS;
import org.osgl.mvc.result.Result;
import org.osgl.util.C;
import org.osgl.util.E;

import java.util.Comparator;
import java.util.List;

public abstract class ExceptionInterceptor
        extends Handler<ExceptionInterceptor>
        implements Plugin, ExceptionInterceptorInvoker, Comparable<ExceptionInterceptor> {

    private List<Class<? extends Exception>> exClasses;

    public ExceptionInterceptor() {
        this(0);
    }

    @SuppressWarnings("unchecked")
    public ExceptionInterceptor(Integer priority) {
        this(priority, new Class[]{});
    }

    public ExceptionInterceptor(Class<? extends Exception>... exClasses) {
        this(0, exClasses);
    }

    public ExceptionInterceptor(Integer priority, Class<? extends Exception>... exClasses) {
        super(priority);
        this.exClasses = C.listOf(exClasses).sorted(EXCEPTION_WEIGHT_COMPARATOR);
    }

    public ExceptionInterceptor(Integer priority, List<Class<? extends Exception>> exClasses) {
        super(priority);
        E.illegalArgumentIf(exClasses.isEmpty());
        this.exClasses = C.list(exClasses).sorted(EXCEPTION_WEIGHT_COMPARATOR);
    }

    @Override
    public Result handle(Exception e, ActionContext actionContext) throws Exception {
        if (exClasses.isEmpty()) {
            return internalHandle(e, actionContext);
        }
        for (Class<? extends Exception> c : exClasses) {
            if (c.isInstance(e)) {
                return internalHandle(e, actionContext);
            }
        }
        return null;
    }

    @Override
    public int compareTo(ExceptionInterceptor o) {
        if (o == this) return 0;
        boolean iAmEmpty = exClasses.isEmpty(), uAreEmpty = o.exClasses.isEmpty();
        if (iAmEmpty) {
            return uAreEmpty ? 0 : 1;
        } else if (uAreEmpty) {
            return -1;
        }
        return EXCEPTION_WEIGHT_COMPARATOR.compare(o.exClasses.get(0), exClasses.get(0));
    }

    @Override
    public CORS.Spec corsSpec() {
        return CORS.Spec.DUMB;
    }

    protected abstract Result internalHandle(Exception e, ActionContext actionContext) throws Exception;

    @Override
    public void register() {
        RequestHandlerProxy.registerGlobalInterceptor(this);
    }

    public static Comparator<Class<? extends Exception>> EXCEPTION_WEIGHT_COMPARATOR = new Comparator<Class<? extends Exception>>() {
        @Override
        public int compare(Class<? extends Exception> o1, Class<? extends Exception> o2) {
            return weight(o1) - weight(o2);
        }
    };

    public static int weight(Class c) {
        int i = 0;
        while (c != Object.class) {
            i++;
            c = c.getSuperclass();
        }
        return i;
    }
}
