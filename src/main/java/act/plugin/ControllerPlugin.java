package act.plugin;

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

import act.app.ActionContext;
import act.controller.meta.ControllerClassMetaInfo;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;
import org.osgl.http.H;
import org.osgl.mvc.result.Result;
import org.osgl.util.C;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * allows plugin different controller annotations framework, e.g. JAX-RS
 */
public abstract class ControllerPlugin implements Plugin {

    public static final class PathAnnotationSpec {
        private final Class<? extends Annotation> annoType;
        private final boolean supportAbsolutePath;
        private final boolean supportInheritance;

        public PathAnnotationSpec(Class<? extends Annotation> annoType, boolean supportAbsolutePath, boolean supportInheritance) {
            this.annoType = annoType;
            this.supportAbsolutePath = supportAbsolutePath;
            this.supportInheritance = supportInheritance;
        }

        public Class<? extends Annotation> annoType() {
            return annoType;
        }

        public boolean supportAbsolutePath() {
            return supportAbsolutePath;
        }

        public boolean supportInheritance() {
            return supportInheritance;
        }
    }

    protected abstract Map<Class<? extends Annotation>, H.Method> annotationMethodLookup();

    protected abstract PathAnnotationSpec urlContextAnnotation();

    protected abstract boolean noDefaultPath();

    /**
     * Returns function that takes {@link ActionContext} as parameter and returns a {@link Result}. If
     * a non-null result is returned, then it will by pass the ActFramework handling process and return
     * directly, in which case the plugin literally hijack the ActFramework handling process.
     * <p>
     * The plugin might also do some changes to `ActionContext` and return `null` value in which case
     * ActFramework normal handling process still happens.
     *
     * @param controllerClass the controller class
     * @param actionMethod    the action method
     * @return a logic to be injected before ActFramework handling request
     */
    public $.Function<ActionContext, Result> beforeHandler(Class<?> controllerClass, Method actionMethod) {
        return $.f1();
    }

    private static final $.Func2<Result, ActionContext, Result> DUMB_AFTER_HANDLER = new $.F2<Result, ActionContext, Result>() {
        @Override
        public Result apply(Result result, ActionContext context) throws NotAppliedException, $.Break {
            return result;
        }
    };

    /**
     * Returns function that takes {@link Result} and {@link ActionContext} as parameter
     * and returns a {@link Result}. This function is called after ActFramework's processing
     * has finished.
     * <p>
     * The afterHandler allows plugin to inject logic to further process the returned result
     * or do some updates to `ActionContext` for example to change the response content type
     * etc.
     * <p>
     * The afterHandler shall always returns a result even if there is nothing to do with
     * it, it must return the result passed in.
     *
     * @param controllerClass the controller class
     * @param actionMethod    the action method
     * @return a logic to be injected before ActFramework handling request
     */
    public $.Func2<Result, ActionContext, Result> afterHandler(Class<?> controllerClass, Method actionMethod) {
        return DUMB_AFTER_HANDLER;
    }

    @Override
    public void register() {
        ControllerClassMetaInfo.registerMethodLookups(annotationMethodLookup(), noDefaultPath());
        ControllerClassMetaInfo.registerUrlContextAnnotation(urlContextAnnotation());
        Manager.INST.controllerPlugins.add(this);
    }

    public static class Manager extends ControllerPlugin {

        public static final Manager INST = new Manager();

        private List<ControllerPlugin> controllerPlugins = new ArrayList<>();

        @Override
        public $.Function<ActionContext, Result> beforeHandler(final Class<?> controllerClass, final Method actionMethod) {
            int size = controllerPlugins.size();
            switch (size) {
                case 0:
                    return super.beforeHandler(controllerClass, actionMethod);
                case 1:
                    return controllerPlugins.get(0).beforeHandler(controllerClass, actionMethod);
                default:
                    return new $.Function<ActionContext, Result>() {
                        @Override
                        public Result apply(ActionContext context) throws NotAppliedException, $.Break {
                            for (ControllerPlugin plugin: controllerPlugins) {
                                Result result = plugin.beforeHandler(controllerClass, actionMethod).apply(context);
                                if (null != result) {
                                    return result;
                                }
                            }
                            return null;
                        }
                    };
            }
        }

        @Override
        public $.Func2<Result, ActionContext, Result> afterHandler(final Class<?> controllerClass, final Method actionMethod) {
            int size = controllerPlugins.size();
            switch (size) {
                case 0:
                    return super.afterHandler(controllerClass, actionMethod);
                case 1:
                    return controllerPlugins.get(0).afterHandler(controllerClass, actionMethod);
                default:
                    return new $.Func2<Result, ActionContext, Result>() {
                        @Override
                        public Result apply(Result result, ActionContext context) throws NotAppliedException, $.Break {
                            for (ControllerPlugin plugin: controllerPlugins) {
                                result = plugin.afterHandler(controllerClass, actionMethod).apply(result, context);
                            }
                            return result;
                        }
                    };
            }
        }

        @Override
        protected Map<Class<? extends Annotation>, H.Method> annotationMethodLookup() {
            return C.Map();
        }

        @Override
        protected PathAnnotationSpec urlContextAnnotation() {
            return null;
        }

        @Override
        protected boolean noDefaultPath() {
            return false;
        }

        @Override
        public void register() {
        }
    }

}
