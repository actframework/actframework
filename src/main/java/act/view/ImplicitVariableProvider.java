package act.view;

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
import act.app.*;
import act.app.event.SysEventId;
import act.asm.*;
import act.inject.genie.GenieInjector;
import act.inject.param.ParamValueLoader;
import act.inject.param.ProvidedValueLoader;
import act.mail.MailerContext;
import act.plugin.Plugin;
import act.util.AsmTypes;
import act.util.ByteCodeVisitor;
import com.esotericsoftware.reflectasm.MethodAccess;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.util.E;
import org.osgl.util.S;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Plugin developer could extend this interface to inject
 * implicit variables to view template
 */
public abstract class ImplicitVariableProvider implements Plugin {

    /**
     * Returns a list of implicit variables the plugin needs to inject
     * into template render arguments for action view
     */
    public abstract List<ActionViewVarDef> implicitActionViewVariables();

    /**
     * Returns a list of implicit variables the plugin needs to inject
     * into template render arguments for mailer view
     */
    public abstract List<MailerViewVarDef> implicitMailerViewVariables();

    @Override
    public void register() {
        Act.viewManager().register(this);
    }

    public static class TemplateVariableScanner extends AppByteCodeScannerBase {

        private static class Meta {
            String className;
            String methodName;
            String varName;
            boolean isStatic;

            @Override
            public int hashCode() {
                return $.hc(className, methodName);
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj instanceof Meta) {
                    Meta that = $.cast(obj);
                    return $.eq(that.className, this.className) && $.eq(that.methodName, this.methodName);
                }
                return false;
            }

            String varName() {
                return (null == varName || S.eq(ProvidesImplicitTemplateVariable.DEFAULT, varName)) ? methodName : varName;
            }

            @Override
            public String toString() {
                return S.concat("Template variable provider: ", className, "::", methodName);
            }
        }

        private Set<Meta> providers = new HashSet<>();

        public TemplateVariableScanner(final App app) {
            app.jobManager().on(SysEventId.START, "TemplateVariableScanner:registerByteCodeScanner", new Runnable() {
                @Override
                public void run() {
                    register(app);
                }
            });
        }

        @Override
        protected boolean shouldScan(String className) {
            return true;
        }

        @Override
        public ByteCodeVisitor byteCodeVisitor() {
            return new ByteCodeVisitor() {
                private String className;

                @Override
                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                    if (isPublic(access)) {
                        className = Type.getObjectType(name).getClassName();
                    }
                    super.visit(version, access, name, signature, superName, interfaces);
                }

                @Override
                public MethodVisitor visitMethod(int access, final String methodName, String desc, String signature, String[] exceptions) {
                    MethodVisitor mv = super.visitMethod(access, methodName, desc, signature, exceptions);
                    if (null == className) {
                        return mv;
                    }

                    final boolean isStatic = isStatic(access);

                    return new MethodVisitor(ASM5, mv) {
                        @Override
                        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                            AnnotationVisitor av = super.visitAnnotation(desc, visible);
                            Type annoType = Type.getType(desc);
                            if (AsmTypes.TEMPLATE_VARIABLE.asmType().equals(annoType)) {
                                final Meta meta = new Meta();
                                meta.className = className;
                                meta.methodName = methodName;
                                meta.isStatic = isStatic;
                                if (providers.contains(meta)) {
                                    throw AsmException.of("@ProvidesImplicitTemplateVariable annotated method cannot be overloaded: %s", meta.toString());
                                }
                                providers.add(meta);
                                return new AnnotationVisitor(ASM5, av) {
                                    @Override
                                    public void visit(String name, Object value) {
                                        if ("value".equals(name)) {
                                            meta.varName = value.toString();
                                        }
                                        super.visit(name, value);
                                    }
                                };
                            }
                            return av;
                        }
                    };
                }
            };
        }

        @Override
        public void scanFinished(String className) {

        }

        private void register(App app) {
            for (Meta meta : providers) {
                register(meta, app);
            }
        }

        private void register(Meta meta, App app) {
            Class<?> cls = app.classForName(meta.className);
            Method method = findMethod(cls, meta.methodName);
            E.unexpectedIf(null == method, "Unable to find method %s", meta);
            register(meta, cls, method, app);
        }

        private Method findMethod(Class<?> cls, String methodName) {
            Method[] methods = cls.getDeclaredMethods();
            for (Method method : methods) {
                if (!method.getName().equals(methodName)) {
                    continue;
                }
                if (method.isAnnotationPresent(ProvidesImplicitTemplateVariable.class)) {
                    return method;
                }
            }
            return null;
        }

        private void register(Meta meta, Class<?> cls, Method method, App app) {
            final ReflectedActionViewVarDef def = new ReflectedActionViewVarDef(meta, cls, method, app);
            if (def.supportMailer) {
                MailerViewVarDef mailerVarDef = new MailerViewVarDef(meta.varName(), def.returnType(app)) {
                    @Override
                    public Object eval(MailerContext context) {
                        return def.getValue(context.app());
                    }
                };
                Act.viewManager().registerAppDefinedVar(mailerVarDef);
            }
            if (def.supportAction) {
                ActionViewVarDef actionVarDef = new ActionViewVarDef(meta.varName(), def.returnType(app)) {
                    @Override
                    public Object eval(ActionContext context) {
                        return def.getValue(context.app());
                    }
                };
                Act.viewManager().registerAppDefinedVar(actionVarDef);
            }
        }

        private static class ReflectedActionViewVarDef {

            private Class<?> cls;
            private Method method;
            private MethodAccess methodAccess;
            private int methodIndex;
            private ParamValueLoader[] loaders;
            private boolean supportAction;
            private boolean supportMailer;
            private int paramLen;

            protected ReflectedActionViewVarDef(Meta meta, Class<?> cls, Method method, App app) {
                this.cls = cls;
                this.method = method;
                if (!meta.isStatic) {
                    methodAccess = MethodAccess.get(cls);
                    methodIndex = methodAccess.getIndex(method.getName(), method.getParameterTypes());
                }
                initLoaders(app);
            }

            private void initLoaders(App app) {
                java.lang.reflect.Type[] types = method.getGenericParameterTypes();
                int len = types.length;
                ParamValueLoader[] loaders = new ParamValueLoader[len];
                if (0 < len) {
                    GenieInjector injector = app.injector();
                    Annotation[][] annos = method.getParameterAnnotations();
                    for (int i = 0; i < len; ++i) {
                        java.lang.reflect.Type type = types[i];
                        Annotation[] aa = annos[i];
                        BeanSpec spec = BeanSpec.of(type, aa, injector);
                        E.unexpectedIf(!injector.isProvided(spec), "");
                        loaders[i] = ProvidedValueLoader.get(spec, injector);
                        if (spec.isInstanceOf(ActionContext.class) || requireAction(type)) {
                            supportAction = true;
                        } else if (spec.isInstanceOf(MailerContext.class)) {
                            supportMailer = true;
                        }
                    }
                }
                if (!supportAction && !supportMailer) {
                    supportAction = true;
                    supportMailer = true;
                }
                this.loaders = loaders;
                this.paramLen = len;
            }

            private boolean requireAction(java.lang.reflect.Type type) {
                String name = type.toString();
                if (name.contains("org.osgl.http")) {
                    return true;
                }
                return false;
            }

            BeanSpec returnType(App app) {
                return BeanSpec.of(method.getGenericReturnType(), null, app.injector());
            }

            Object getValue(App app) {
                Object[] params = params();
                if (null != methodAccess) {
                    return methodAccess.invoke(app.getInstance(cls), methodIndex, params);
                } else {
                    return $.invokeStatic(method, params);
                }
            }

            private Object[] params() {
                Object[] params = new Object[paramLen];
                for (int i = 0; i < paramLen; ++i) {
                    ParamValueLoader loader = loaders[i];
                    params[i] = loader.load(null, null, false);
                }
                return params;
            }

        }

    }

}
