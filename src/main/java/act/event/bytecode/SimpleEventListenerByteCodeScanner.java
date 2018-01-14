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

import act.app.AppByteCodeScannerBase;
import act.app.event.AppEventId;
import act.asm.AnnotationVisitor;
import act.asm.MethodVisitor;
import act.asm.Type;
import act.event.*;
import act.event.meta.SimpleEventListenerMetaInfo;
import act.job.AppJobManager;
import act.util.AsmTypes;
import act.util.Async;
import act.util.ByteCodeVisitor;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.exception.NotAppliedException;
import org.osgl.util.S;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public class SimpleEventListenerByteCodeScanner extends AppByteCodeScannerBase {

    private List<SimpleEventListenerMetaInfo> metaInfoList = new ArrayList<>();

    @Override
    protected void reset(String className) {
        metaInfoList.clear();
    }

    @Override
    protected boolean shouldScan(String className) {
        return true;
    }

    @Override
    public ByteCodeVisitor byteCodeVisitor() {
        return new _ByteCodeVisitor();
    }

    @Override
    public void scanFinished(String className) {
        if (!metaInfoList.isEmpty()) {
            final EventBus eventBus = app().eventBus();
            AppJobManager jobManager = app().jobManager();
            for (final SimpleEventListenerMetaInfo metaInfo : metaInfoList) {
                AppEventId hookOn = metaInfo.beforeAppStart() ? AppEventId.DEPENDENCY_INJECTOR_PROVISIONED : AppEventId.PRE_START;
                jobManager.on(hookOn, new Runnable() {
                    @Override
                    public void run() {
                        for (final Object event : metaInfo.events()) {
                            final boolean isStatic = metaInfo.isStatic();
                            if (metaInfo.isAsync()) {
                                eventBus.bindAsync(event, new ReflectedSimpleEventListener(metaInfo.className(), metaInfo.methodName(), metaInfo.paramTypes(), isStatic));
                            } else {
                                eventBus.bind(event, new ReflectedSimpleEventListener(metaInfo.className(), metaInfo.methodName(), metaInfo.paramTypes(), isStatic));
                            }
                        }
                    }
                });
            }
        }
    }

    private class _ByteCodeVisitor extends ByteCodeVisitor {

        private String className;

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            className = Type.getObjectType(name).getClassName();
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            Type returnType = Type.getReturnType(desc);
            final boolean isVoid = "V".equals(returnType.toString());
            final boolean isPublicNotAbstract = AsmTypes.isPublicNotAbstract(access);
            Type[] arguments = Type.getArgumentTypes(desc);
            final List<String> paramTypes = new ArrayList<>();
            if (null != arguments) {
                for (Type type : arguments) {
                    paramTypes.add(type.getClassName());
                }
            }
            final String methodName = name;
            final boolean isStatic = AsmTypes.isStatic(access);
            return new MethodVisitor(ASM5, mv) {

                private List<Object> events = new ArrayList<>();
                private List<$.Func0> delayedEvents = new ArrayList<>();
                private boolean isOnEvent = false;
                private boolean beforeAppStart = false;
                private boolean isAsync;

                private String asyncMethodName = null;


                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    AnnotationVisitor av = super.visitAnnotation(desc, visible);
                    String className = Type.getType(desc).getClassName();
                    final boolean isOn = On.class.getName().equals(className);
                    final boolean isOnC = OnClass.class.getName().equals(className);
                    final boolean isOnE = OnEvent.class.getName().equals(className);
                    if (isOnE) {
                        isOnEvent = true;
                    }
                    boolean isCustomMarker = false;
                    if (!isOn && !isOnC && !isOnE) {
                        Class<?> clz = $.classForName(className);
                        // note we can't use Class.isAnnotationPresent(Class) call here as the class loader might be different
                        // isCustomMarker = clz.isAnnotationPresent(SimpleEventListener.Marker.class);
                        Annotation[] annotations = clz.getAnnotations();
                        for (Annotation annotation : annotations) {
                            if (SimpleEventListener.Marker.class.getCanonicalName().equals(annotation.annotationType().getCanonicalName())) {
                                isCustomMarker = true;
                                break;
                            }
                        }
                    }

                    if (isOnE || isOn || isOnC || isCustomMarker) {
                        return new AnnotationVisitor(ASM5, av) {
                            @Override
                            public AnnotationVisitor visitArray(String name) {
                                AnnotationVisitor av0 = super.visitArray(name);
                                if ("value".equals(name)) {
                                    return new AnnotationVisitor(ASM5, av0) {
                                        @Override
                                        public void visit(String name, Object value) {
                                            if (isOn) {
                                                events.add(S.string(value).intern());
                                            } else {
                                                Type type = (Type) value;
                                                Class<?> c = $.classForName(type.getClassName(), app().classLoader());
                                                events.add(c);
                                            }
                                            super.visit(name, value);
                                        }

                                        @Override
                                        public void visitEnum(String name, String desc, final String value) {
                                            final String enumClassName = Type.getType(desc).getClassName();
                                            delayedEvents.add(new $.Func0() {
                                                @Override
                                                public Object apply() throws NotAppliedException, Osgl.Break {
                                                    Class<? extends Enum> enumClass = $.classForName(enumClassName, app().classLoader());
                                                    return (Enum.valueOf(enumClass, value));
                                                }
                                            });
                                            super.visitEnum(name, desc, value);
                                        }
                                    };
                                } else {
                                    return av0;
                                }
                            }

                            @Override
                            public void visit(String name, Object value) {
                                if ("async".equals(name)) {
                                    isAsync = Boolean.parseBoolean(S.string(value));
                                } else if ("beforeAppStart".equals(name)) {
                                    beforeAppStart = Boolean.parseBoolean(S.string(value));
                                }
                                super.visit(name, value);
                            }

                            @Override
                            public void visitEnum(String name, String desc, final String value) {
                                if ("value".equals(name)) {
                                    final String enumClassName = Type.getType(desc).getClassName();
                                    delayedEvents.add(new $.Func0() {
                                        @Override
                                        public Object apply() throws NotAppliedException, Osgl.Break {
                                            Class<? extends Enum> enumClass = $.classForName(enumClassName, app().classLoader());
                                            return (Enum.valueOf(enumClass, value));
                                        }
                                    });
                                }
                                super.visitEnum(name, desc, value);
                            }
                        };
                    } else if (Async.class.getName().equals(className)) {
                        if (!isVoid) {
                            logger.warn("Error found in method %s.%s: @Async annotation cannot be used with method that has return type", className, methodName);
                        } else if (!isPublicNotAbstract) {
                            logger.warn("Error found in method %s.%s: @Async annotation cannot be used with method that are not public or abstract method", className, methodName);
                        } else {
                            asyncMethodName = Async.MethodNameTransformer.transform(methodName);
                        }
                        return av;
                    } else {
                        return av;
                    }
                }

                @Override
                public void visitEnd() {
                    if (isOnEvent) {
                        if (paramTypes.isEmpty()) {
                            logger.warn("@OnEvent annotation shall be put on a method with exactly one event object (optionally with other injectable arguments");
                        } else {
                            String type = paramTypes.get(0);
                            events.add($.classForName(type, app().classLoader()));
                        }
                    }
                    if (!events.isEmpty() || !delayedEvents.isEmpty()) {
                        SimpleEventListenerMetaInfo metaInfo = new SimpleEventListenerMetaInfo(
                                events, delayedEvents, className, methodName, asyncMethodName, paramTypes, isAsync, isStatic, beforeAppStart, app());
                        metaInfoList.add(metaInfo);
                    }
                    super.visitEnd();
                }
            };
        }
    }
}
