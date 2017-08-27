package act.apidoc;

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
import act.app.DevModeClassLoader;
import act.app.Source;
import act.handler.RequestHandler;
import act.handler.builtin.controller.RequestHandlerProxy;
import act.handler.builtin.controller.impl.ReflectedHandlerInvoker;
import act.inject.DefaultValue;
import act.inject.DependencyInjector;
import act.inject.param.ParamValueLoaderService;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.inject.BeanSpec;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * An `Endpoint` represents an API that provides specific service
 */
public class Endpoint implements Comparable<Endpoint> {

    private static BeanSpecInterpreter beanSpecInterpretor = new BeanSpecInterpreter();

    public static class ParamInfo {
        private String bindName;
        private BeanSpec beanSpec;
        private String description;
        private String defaultValue;

        private ParamInfo(String bindName, BeanSpec beanSpec, String description) {
            this.bindName = bindName;
            this.beanSpec = beanSpec;
            this.description = description;
            this.defaultValue = checkDefaultValue(beanSpec);
        }

        public String getName() {
            return bindName;
        }

        public String getType() {
            return beanSpecInterpretor.inteprete(beanSpec);
        }

        public String getDescription() {
            return description;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        private String checkDefaultValue(BeanSpec spec) {
            DefaultValue def = spec.getAnnotation(DefaultValue.class);
            return null != def ? def.value() : null;
        }
    }

    /**
     * The scheme defines the protocol used to access the endpoint
     *
     * At the moment we support HTTP only
     */
    public enum Scheme {
        HTTP
    }

    /**
     * The scheme used to access the endpoint
     */
    private Scheme scheme = Scheme.HTTP;

    private int port;

    /**
     * The HTTP method
     */
    private H.Method method;

    /**
     * The URL path
     */
    private String path;

    /**
     * The handler.
     *
     * In most case should be `pkg.Class.method`
     */
    private String handler;

    /**
     * The description
     */
    private String description;

    /**
     * Param list.
     *
     * Only available when handler is driven by
     * {@link act.handler.builtin.controller.impl.ReflectedHandlerInvoker}
     */
    public List<ParamInfo> params = new ArrayList<>();

    Endpoint(int port, H.Method method, String path, RequestHandler handler) {
        this.method = $.notNull(method);
        this.path = $.notNull(path);
        this.handler = handler.toString();
        this.port = port;
        explore(handler);
    }

    @Override
    public int compareTo(Endpoint o) {
        int n = path.compareTo(o.path);
        if (0 != n) {
            return n;
        }
        return method.ordinal() - o.method.ordinal();
    }

    public Scheme getScheme() {
        return scheme;
    }

    public int getPort() {
        return port;
    }

    public H.Method getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getHandler() {
        return handler;
    }

    public String getDescription() {
        return description;
    }

    public List<ParamInfo> getParams() {
        return params;
    }

    private void explore(RequestHandler handler) {
        if (!(handler instanceof RequestHandlerProxy)) {
            return;
        }
        RequestHandlerProxy proxy = $.cast(handler);
        ReflectedHandlerInvoker invoker = $.cast(proxy.actionHandler().invoker());
        Class<?> controllerClass = invoker.controllerClass();
        Method method = invoker.method();
        Description descAnno = method.getAnnotation(Description.class);
        this.description = null == descAnno ? methodDescription(method) : descAnno.value();
        exploreParamInfo(method);
        if (!Modifier.isStatic(method.getModifiers())) {
            exploreParamInfo(controllerClass);
        }
    }

    private String methodDescription(Method method) {
        if (Act.isDev()) {
            DevModeClassLoader cl = $.cast(Act.app().classLoader());
            Source source = cl.source(method.getDeclaringClass());
            if (null != source) {
                // TODO find method description from comments in source file
            }
        }
        return method.toGenericString();
    }

    private void exploreParamInfo(Method method) {
        Type[] paramTypes = method.getGenericParameterTypes();
        int paramCount = paramTypes.length;
        if (0 == paramCount) {
            return;
        }
        DependencyInjector injector = Act.injector();
        Annotation[][] allAnnos = method.getParameterAnnotations();
        for (int i = 0; i < paramCount; ++i) {
            Type type = paramTypes[i];
            Annotation[] annos = allAnnos[i];
            ParamInfo info = paramInfo(type, annos, injector, null);
            if (null != info) {
                params.add(info);
            }
        }
    }

    private void exploreParamInfo(Class<?> controller) {
        DependencyInjector injector = Act.injector();
        List<Field> fields = $.fieldsOf(controller);
        for (Field field : fields) {
            if (ParamValueLoaderService.shouldWaive(field)) {
                continue;
            }
            Type type = field.getGenericType();
            Annotation[] annos = field.getAnnotations();
            ParamInfo info = paramInfo(type, annos, injector, field.getName());
            if (null != info) {
                params.add(info);
            }
        }
    }

    private ParamInfo paramInfo(Type type, Annotation[] annos, DependencyInjector injector, String name) {
        if (isLoginUser(annos)) {
            return null;
        }
        BeanSpec spec = BeanSpec.of(type, annos, name, injector);
        if (ParamValueLoaderService.providedButNotDbBind(spec, injector)) {
            return null;
        }
        String description = spec.toString();
        Description descAnno = spec.getAnnotation(Description.class);
        if (null != descAnno) {
            description = descAnno.value();
        }
        return new ParamInfo(spec.name(), spec, description);
    }

    private boolean isLoginUser(Annotation[] annos) {
        for (Annotation a : annos) {
            if ("LoginUser".equals(a.annotationType().getSimpleName())) {
                return true;
            }
        }
        return false;
    }

}
