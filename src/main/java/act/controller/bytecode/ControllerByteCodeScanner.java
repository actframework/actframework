package act.controller.bytecode;

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
import act.app.App;
import act.app.AppByteCodeScannerBase;
import act.app.AppClassLoader;
import act.app.event.AppEventId;
import act.asm.*;
import act.asm.signature.SignatureReader;
import act.asm.signature.SignatureVisitor;
import act.conf.AppConfig;
import act.controller.Controller;
import act.controller.annotation.Port;
import act.controller.annotation.UrlContext;
import act.controller.meta.*;
import act.handler.builtin.controller.RequestHandlerProxy;
import act.route.RouteSource;
import act.route.Router;
import act.util.*;
import act.ws.WsEndpoint;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.http.H;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.mvc.annotation.With;
import org.osgl.mvc.util.Binder;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.ListBuilder;
import org.osgl.util.S;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * New controller scanner implementation
 */
public class ControllerByteCodeScanner extends AppByteCodeScannerBase {

    private final static Logger logger = L.get(ControllerByteCodeScanner.class);
    private Router router;
    private ControllerClassMetaInfo classInfo;
    private volatile ControllerClassMetaInfoManager classInfoBase;

    public ControllerByteCodeScanner() {
    }

    @Override
    protected boolean shouldScan(String className) {
        boolean possibleController = config().possibleControllerClass(className);
        classInfo = new ControllerClassMetaInfo().possibleController(possibleController);
        return possibleController;
    }

    @Override
    protected void onAppSet() {
        router = app().router();
    }

    @Override
    public ByteCodeVisitor byteCodeVisitor() {
        return new _ByteCodeVisitor();
    }

    @Override
    public void scanFinished(String className) {
        if (classInfo.isController()) {
            classInfoBase().registerControllerMetaInfo(classInfo);
        }
    }

    private ControllerClassMetaInfoManager classInfoBase() {
        if (null == classInfoBase) {
            synchronized (this) {
                if (null == classInfoBase) {
                    classInfoBase = app().classLoader().controllerClassMetaInfoManager();
                }
            }
        }
        return classInfoBase;
    }

    private class _ByteCodeVisitor extends ByteCodeVisitor {
        private String[] ports = {};
        private Set<String> methodNames = new HashSet<>();

        private void checkMethodName(String methodName) {
            if (methodNames.contains(methodName)) {
                throw AsmException.of("Duplicate action/interceptor method name found: %s", methodName);
            }
            methodNames.add(methodName);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            logger.trace("Scanning %s", name);
            classInfo.className(name);
            String className = name.replace('/', '.');
            if (router.possibleController(className)) {
                classInfo.isController(true);
            }
            Type superType = Type.getObjectType(superName);
            classInfo.superType(superType);
            if (isAbstract(access)) {
                classInfo.setAbstract();
            }
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            AnnotationVisitor av = super.visitAnnotation(desc, visible);
            if (Type.getType(Controller.class).getDescriptor().equals(desc)) {
                classInfo.isController(true);
                return new ControllerAnnotationVisitor(av);
            } else if (Type.getType(UrlContext.class).getDescriptor().equals(desc)) {
                classInfo.isController(true);
                return new UrlContextAnnotationVisitor(av);
            } else if (Type.getType(Port.class).getDescriptor().equals(desc)) {
                return new PortAnnotationVisitor(av);
            } else if (Type.getType(With.class).getDescriptor().equals(desc)) {
                classInfo.isController(true);
                return new ClassWithAnnotationVisitor(av);
            } else if (Type.getType(WsEndpoint.class).getDescriptor().equals(desc)) {
                classInfo.isController(true);
                return new WsEndpointAnnotationVisitor(av);
            }
            return super.visitAnnotation(desc, visible);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if (!classInfo.possibleController() || !isEligibleMethod(access, name)) {
                return mv;
            }
            String className = classInfo.className();
            boolean isRoutedMethod = router.isActionMethod(className, name);
            return new ActionMethodVisitor(isRoutedMethod, mv, access, name, desc, signature, exceptions);
        }

        private boolean isEligibleMethod(int access, String name) {
            return !isAbstract(access) && !isConstructor(name);
        }

        private class StringArrayVisitor extends AnnotationVisitor {
            protected ListBuilder<String> strings = ListBuilder.create();

            public StringArrayVisitor(AnnotationVisitor av) {
                super(ASM5, av);
            }

            @Override
            public void visit(String name, Object value) {
                strings.add(value.toString());
                super.visit(name, value);
            }
        }

        private class WsEndpointAnnotationVisitor extends AnnotationVisitor {
            WsEndpointAnnotationVisitor(AnnotationVisitor av) {
                super(ASM5, av);
            }

            @Override
            public AnnotationVisitor visitArray(String name) {
                AnnotationVisitor av = super.visitArray(name);
                if ("value".equals(name)) {
                    return new StringArrayVisitor(av) {
                        @Override
                        public void visitEnd() {
                            List<Router> routers = routers();
                            if (strings.isEmpty()) {
                                strings.add("");
                            }
                            /*
                             * Note we need to schedule route registration after all app code scanned because we need the
                             * parent context information be set on class meta info, which is done after controller scanning
                             */
                            app().jobManager().on(AppEventId.APP_CODE_SCANNED, new RouteRegister(C.list(H.Method.GET), strings, WsEndpoint.PSEUDO_METHOD, routers, classInfo, false, $.var(false)));

                            super.visitEnd();
                        }
                    };
                }
                return super.visitArray(name);
            }
        }

        private class ClassWithAnnotationVisitor extends AnnotationVisitor {
            public ClassWithAnnotationVisitor(AnnotationVisitor av) {
                super(ASM5, av);
            }

            @Override
            public AnnotationVisitor visitArray(String name) {
                AnnotationVisitor av = super.visitArray(name);
                if ("value".equals(name)) {
                    return new StringArrayVisitor(av) {
                        @Override
                        public void visitEnd() {
                            String[] sa = new String[strings.size()];
                            sa = strings.toArray(sa);
                            classInfo.addWith(sa);
                            super.visitEnd();
                        }
                    };
                }
                return av;
            }
        }

        private class ControllerAnnotationVisitor extends AnnotationVisitor {
            ControllerAnnotationVisitor(AnnotationVisitor av) {
                super(ASM5, av);
            }

            @Override
            public void visit(String name, Object value) {
                if ("value".equals(name)) {
                    classInfo.contextPath(value.toString());
                }
            }

            @Override
            public AnnotationVisitor visitArray(String name) {
                AnnotationVisitor av = super.visitArray(name);
                if ("port".equals(name)) {
                    return new StringArrayVisitor(av) {
                        @Override
                        public void visitEnd() {
                            ports = new String[strings.size()];
                            ports = strings.toArray(ports);
                            super.visitEnd();
                        }
                    };
                }
                return av;
            }
        }

        private class UrlContextAnnotationVisitor extends AnnotationVisitor {
            UrlContextAnnotationVisitor(AnnotationVisitor av) {
                super(ASM5, av);
            }

            @Override
            public void visit(String name, Object value) {
                if ("value".equals(name)) {
                    classInfo.contextPath(value.toString());
                }
            }
        }

        private class PortAnnotationVisitor extends AnnotationVisitor {
            PortAnnotationVisitor(AnnotationVisitor av) {
                super(ASM5, av);
            }

            @Override
            public AnnotationVisitor visitArray(String name) {
                AnnotationVisitor av = super.visitArray(name);
                if ("value".equals(name)) {
                    return new StringArrayVisitor(av) {
                        @Override
                        public void visitEnd() {
                            ports = new String[strings.size()];
                            ports = strings.toArray(ports);
                            super.visitEnd();
                        }
                    };
                }
                return av;
            }
        }


        private class ActionMethodVisitor extends MethodVisitor implements Opcodes {

            private String methodName;
            private int access;
            private String desc;
            private String signature;
            private boolean isStatic;
            private boolean requireScan;
            private boolean disableJsonCircularRefDetect;
            private HandlerMethodMetaInfo methodInfo;
            private PropertySpec.MetaInfo propSpec;
            private Map<Integer, List<ParamAnnoInfoTrait>> paramAnnoInfoList = C.newMap();
            private Map<Integer, List<GeneralAnnoInfo>> genericParamAnnoInfoList = C.newMap();
            private BitSet contextInfo = new BitSet();
            private $.Var<Boolean> isVirtual = $.var(false);
            private HandlerWithAnnotationVisitor withAnnotationVisitor;
            private $.Var<Boolean> isGlobal = $.var(false);
            private List<InterceptorAnnotationVisitor> interceptorAnnotationVisitors = new ArrayList<>();

            ActionMethodVisitor(boolean isRoutedMethod, MethodVisitor mv, int access, String methodName, String desc, String signature, String[] exceptions) {
                super(ASM5, mv);
                this.access = access;
                this.methodName = methodName;
                this.desc = desc;
                this.signature = signature;
                this.isStatic = isStatic(access);
                if (classInfo.isAbstract()) {
                    this.isVirtual.set(true);
                }
                if (isRoutedMethod) {
                    markRequireScan();
                }
            }

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                AnnotationVisitor av = super.visitAnnotation(desc, visible);
                Type type = Type.getType(desc);
                String className = type.getClassName();
                Class<? extends Annotation> c = $.classForName(className);
                if (Virtual.class.getName().equals(c.getName())) {
                    isVirtual.set(true);
                    return av;
                }
                if (Global.class.getName().equals(c.getName())) {
                    isGlobal.set(true);
                    return av;
                }
                if (Type.getType(With.class).getDescriptor().equals(desc)) {
                    classInfo.isController(true);
                    withAnnotationVisitor = new HandlerWithAnnotationVisitor(av);
                    return withAnnotationVisitor;
                }
                if (ControllerClassMetaInfo.isActionAnnotation(c)) {
                    checkMethodName(methodName);
                    markRequireScan();
                    methodInfo = new ActionMethodMetaInfo(classInfo);
                    classInfo.addAction((ActionMethodMetaInfo) methodInfo);
                    if (null != propSpec) {
                        methodInfo.propertySpec(propSpec);
                    }
                    methodInfo.disableJsonCircularRefDetect(disableJsonCircularRefDetect);
                    return new ActionAnnotationVisitor(av, ControllerClassMetaInfo.lookupHttpMethod(c), ControllerClassMetaInfo.isActionUtilAnnotation(c), isStatic);
                } else if (ControllerClassMetaInfo.isInterceptorAnnotation(c)) {
                    checkMethodName(methodName);
                    markRequireScan();
                    InterceptorAnnotationVisitor visitor = new InterceptorAnnotationVisitor(av, c);
                    methodInfo = visitor.info;
                    if (null != propSpec) {
                        methodInfo.propertySpec(propSpec);
                    }
                    interceptorAnnotationVisitors.add(visitor);
                    return visitor;
                } else if ($.eq(AsmTypes.PROPERTY_SPEC.asmType(), type)) {
                    propSpec = new PropertySpec.MetaInfo();
                    if (null != methodInfo) {
                        methodInfo.propertySpec(propSpec);
                    }
                    return new AnnotationVisitor(ASM5, av) {
                        @Override
                        public AnnotationVisitor visitArray(String name) {
                            AnnotationVisitor av0 = super.visitArray(name);
                            if (S.eq("value", name)) {
                                return new AnnotationVisitor(ASM5, av0) {
                                    @Override
                                    public void visit(String name, Object value) {
                                        super.visit(name, value);
                                        propSpec.onValue(S.string(value));
                                    }
                                };
                            } else if (S.eq("cli", name)) {
                                return new AnnotationVisitor(ASM5, av0) {
                                    @Override
                                    public void visit(String name, Object value) {
                                        super.visit(name, value);
                                        propSpec.onCli(S.string(value));
                                    }
                                };
                            } else if (S.eq("http", name)) {
                                return new AnnotationVisitor(ASM5, av0) {
                                    @Override
                                    public void visit(String name, Object value) {
                                        super.visit(name, value);
                                        propSpec.onHttp(S.string(value));
                                    }
                                };
                            } else {
                                return av0;
                            }
                        }
                    };
                } else if ($.eq(AsmTypes.DISABLE_JSON_CIRCULAR_REF_DETECT.asmType(), type)) {
                    if (null != methodInfo) {
                        methodInfo.disableJsonCircularRefDetect(true);
                    } else {
                        disableJsonCircularRefDetect = true;
                    }
                }
                //markNotTargetClass();
                return av;
            }

            @Override
            public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
                AnnotationVisitor av = super.visitParameterAnnotation(parameter, desc, visible);
                Type type = Type.getType(desc);
                if ($.eq(type, AsmTypes.PARAM.asmType())) {
                    return new ParamAnnotationVisitor(av, parameter);
                } else if ($.eq(type, AsmTypes.BIND.asmType())) {
                    return new BindAnnotationVisitor(av, parameter);
                } else if ($.eq(type, AsmTypes.CONTEXT.asmType())) {
                    contextInfo.set(parameter);
                    return av;
                } else {
                    //return av;
                    GeneralAnnoInfo info = new GeneralAnnoInfo(type);
                    List<GeneralAnnoInfo> list = genericParamAnnoInfoList.get(parameter);
                    if (null == list) {
                        list = C.newList();
                        genericParamAnnoInfoList.put(parameter, list);
                    }
                    list.add(info);
                    return new GeneralAnnoInfo.Visitor(av, info);
                }
            }

            @Override
            public void visitEnd() {
                if (!requireScan()) {
                    super.visitEnd();
                    return;
                }
                if (isGlobal.get()) {
                    for (InterceptorAnnotationVisitor visitor : interceptorAnnotationVisitors) {
                        visitor.registerGlobalInterceptor();
                    }
                }
                classInfo.isController(true);
                if (null == methodInfo) {
                    ActionMethodMetaInfo action = new ActionMethodMetaInfo(classInfo);
                    methodInfo = action;
                    classInfo.addAction(action);
                }
                if (null != withAnnotationVisitor) {
                    if (methodInfo instanceof ActionMethodMetaInfo) {
                        ActionMethodMetaInfo actionInfo = $.cast(methodInfo);
                        actionInfo.addWith(withAnnotationVisitor.withArray);
                    }
                }
                final HandlerMethodMetaInfo info = methodInfo;
                info.name(methodName);
                boolean isStatic = AsmTypes.isStatic(access);
                if (isStatic) {
                    info.invokeStaticMethod();
                } else {
                    info.invokeInstanceMethod();
                }
                info.returnType(Type.getReturnType(desc));
                Type[] argTypes = Type.getArgumentTypes(desc);
                boolean ctxByParam = false;
                for (int i = 0; i < argTypes.length; ++i) {
                    Type type = argTypes[i];
                    if (AsmTypes.ACTION_CONTEXT.asmType().equals(type)) {
                        ctxByParam = true;
                        info.appContextViaParam(i);
                    }
                    HandlerParamMetaInfo param = new HandlerParamMetaInfo().type(type);
                    if (contextInfo.get(i)) {
                        param.setContext();
                    }
                    List<ParamAnnoInfoTrait> paraAnnoList = paramAnnoInfoList.get(i);
                    if (null != paraAnnoList) {
                        for (ParamAnnoInfoTrait trait : paraAnnoList) {
                            trait.attachTo(param);
                        }
                    }
                    List<GeneralAnnoInfo> list = genericParamAnnoInfoList.get(i);
                    if (null != list) {
                        param.addGeneralAnnotations(list);
                    }
                    info.addParam(param);
                }
                if (!ctxByParam) {
                    if (classInfo.hasCtxField() && !isStatic) {
                        info.appContextViaField(classInfo.ctxField());
                    } else {
                        info.appContextViaLocalStorage();
                    }
                }
                if (null != signature) {
                    SignatureReader sr = new SignatureReader(signature);
                    final $.Var<Integer> id = new $.Var<Integer>(-1);
                    sr.accept(new SignatureVisitor(ASM5) {

                        boolean startParsing;

                        @Override
                        public SignatureVisitor visitParameterType() {
                            id.set(id.get() + 1);
                            return this;
                        }

                        @Override
                        public SignatureVisitor visitTypeArgument(char wildcard) {
                            if (wildcard == '=') {
                                startParsing = true;
                            }
                            return this;
                        }

                        @Override
                        public void visitClassType(String name) {
                            if (startParsing) {
                                Type type = Type.getObjectType(name);
                                int n = id.get();
                                if (n < 0) {
                                    info.returnComponentType(type);
                                } else {
                                    info.param(n).componentType(type);
                                }
                            }
                            startParsing = false;
                        }
                    });
                }
                super.visitEnd();
            }

            private class HandlerWithAnnotationVisitor extends AnnotationVisitor {

                private String[] withArray;

                public HandlerWithAnnotationVisitor(AnnotationVisitor av) {
                    super(ASM5, av);
                }

                @Override
                public AnnotationVisitor visitArray(String name) {
                    AnnotationVisitor av = super.visitArray(name);
                    if ("value".equals(name)) {
                        return new StringArrayVisitor(av) {
                            @Override
                            public void visitEnd() {
                                String[] sa = new String[strings.size()];
                                sa = strings.toArray(sa);
                                withArray = sa;
                                super.visitEnd();
                            }
                        };
                    }
                    return av;
                }
            }

            private void markRequireScan() {
                this.requireScan = true;
            }

            private boolean requireScan() {
                return requireScan;
            }

            private class InterceptorAnnotationVisitor extends AnnotationVisitor implements Opcodes {
                private InterceptorMethodMetaInfo info;
                private InterceptorType interceptorType;

                public InterceptorAnnotationVisitor(AnnotationVisitor av, Class<? extends Annotation> annoCls) {
                    super(ASM5, av);
                    interceptorType = InterceptorType.of(annoCls);
                    info = interceptorType.createMetaInfo(classInfo);
                    classInfo.addInterceptor(info, annoCls);
                }

                @Override
                public void visit(String name, Object value) {
                    if ("priority".equals(name)) {
                        info.priority((Integer) value);
                    }
                    super.visit(name, value);
                }

                @Override
                public AnnotationVisitor visitArray(String name) {
                    if ("only".equals(name)) {
                        return new OnlyValueVisitor(av);
                    } else if ("except".equals(name)) {
                        return new ExceptValueVisitor(av);
                    } else if ("value".equals(name)) {
                        if (info instanceof CatchMethodMetaInfo) {
                            return new CatchValueVisitor(av);
                        }
                    }
                    return super.visitArray(name);
                }

                void registerGlobalInterceptor() {
                    RequestHandlerProxy.registerGlobalInterceptor(info, interceptorType);
                }

                private class OnlyValueVisitor extends StringArrayVisitor {
                    public OnlyValueVisitor(AnnotationVisitor av) {
                        super(av);
                    }

                    @Override
                    public void visitEnd() {
                        String[] sa = new String[strings.size()];
                        sa = strings.toArray(sa);
                        info.addOnly(sa);
                        super.visitEnd();
                    }
                }

                private class ExceptValueVisitor extends StringArrayVisitor {
                    public ExceptValueVisitor(AnnotationVisitor av) {
                        super(av);
                    }

                    @Override
                    public void visitEnd() {
                        String[] sa = new String[strings.size()];
                        sa = strings.toArray(sa);
                        info.addExcept(sa);
                        super.visitEnd();
                    }
                }

                private class CatchValueVisitor extends AnnotationVisitor {
                    List<String> exceptions = C.newList();

                    public CatchValueVisitor(AnnotationVisitor av) {
                        super(ASM5, av);
                    }

                    @Override
                    public void visit(String name, Object value) {
                        exceptions.add(((Type) value).getClassName());
                    }

                    @Override
                    public void visitEnd() {
                        CatchMethodMetaInfo ci = (CatchMethodMetaInfo) info;
                        ci.exceptionClasses(exceptions);
                        super.visitEnd();
                    }
                }
            }

            private class ActionAnnotationVisitor extends AnnotationVisitor implements Opcodes {

                List<H.Method> httpMethods = C.newList();
                List<String> paths = C.newList();
                boolean isUtil;
                boolean isStatic;

                public ActionAnnotationVisitor(AnnotationVisitor av, H.Method method, boolean isUtil, boolean staticMethod) {
                    super(ASM5, av);
                    if (null != method) {
                        httpMethods.add(method);
                    }
                    this.isUtil = isUtil;
                    this.isStatic = staticMethod;
                }

                @Override
                public AnnotationVisitor visitArray(String name) {
                    AnnotationVisitor av = super.visitArray(name);
                    if ("value".equals(name)) {
                        return new AnnotationVisitor(ASM5, av) {
                            @Override
                            public void visit(String name, Object value) {
                                super.visit(name, value);
                                paths.add((String) value);
                            }
                        };
                    } else if ("methods".equals(name)) {
                        return new AnnotationVisitor(ASM5, av) {
                            @Override
                            public void visitEnum(String name, String desc, String value) {
                                super.visitEnum(name, desc, value);
                                String enumClass = Type.getType(desc).getClassName();
                                if (H.Method.class.getName().equals(enumClass)) {
                                    H.Method method = H.Method.valueOf(value);
                                    httpMethods.add(method);
                                }
                            }
                        };
                    } else {
                        return av;
                    }
                }

                @Override
                public void visitEnd() {
                    super.visitEnd();
                    if (isUtil) {
                        return;
                    }
                    if (httpMethods.isEmpty()) {
                        // start(*) match
                        httpMethods.addAll(H.Method.actionMethods());
                    }
                    final List<Router> routers = routers();
                    if (paths.isEmpty()) {
                        paths.add("");
                    }

                    /*
                     * Note we need to schedule route registration after all app code scanned because we need the
                     * parent context information be set on class meta info, which is done after controller scanning
                     */
                    app().jobManager().on(AppEventId.APP_CODE_SCANNED, new RouteRegister(httpMethods, paths, methodName, routers, classInfo, classInfo.isAbstract() && !isStatic, isVirtual));
                }

            }

            private abstract class ParamAnnotationVisitorBase<T extends ParamAnnoInfoTrait>
                    extends AnnotationVisitor implements Opcodes {
                protected int index;
                protected T info;

                public ParamAnnotationVisitorBase(AnnotationVisitor av, int index) {
                    super(ASM5, av);
                    this.index = index;
                    this.info = createAnnotationInfo(index);
                }

                @Override
                public void visitEnd() {
                    List<ParamAnnoInfoTrait> traits = paramAnnoInfoList.get(index);
                    if (null == traits) {
                        traits = C.newList();
                        paramAnnoInfoList.put(index, traits);
                    } else {
                        for (ParamAnnoInfoTrait trait : traits) {
                            if (!info.compatibleWith(trait)) {
                                throw E.unexpected(info.compatibilityErrorMessage(trait));
                            }
                        }
                    }
                    traits.add(info);
                    super.visitEnd();
                }

                protected abstract T createAnnotationInfo(int index);
            }

            private class ParamAnnotationVisitor extends ParamAnnotationVisitorBase<ParamAnnoInfo> {
                public ParamAnnotationVisitor(AnnotationVisitor av, int index) {
                    super(av, index);
                }

                @Override
                protected ParamAnnoInfo createAnnotationInfo(int index) {
                    return new ParamAnnoInfo(index);
                }

                @Override
                public void visit(String name, Object value) {
                    if (S.eq("value", name)) {
                        info.bindName((String) value);
                    } else if (S.eq("defVal", name)) {
                        info.defVal(String.class, value);
                    } else if (S.eq("defIntVal", name)) {
                        info.defVal(Integer.class, value);
                    } else if (S.eq("defBooleanVal", name)) {
                        info.defVal(Boolean.class, value);
                    } else if (S.eq("defLongVal", name)) {
                        info.defVal(Long.class, value);
                    } else if (S.eq("defDoubleVal", name)) {
                        info.defVal(Double.class, value);
                    } else if (S.eq("defFloatVal", name)) {
                        info.defVal(Float.class, value);
                    } else if (S.eq("defCharVal", name)) {
                        info.defVal(Character.class, value);
                    } else if (S.eq("defByteVal", name)) {
                        info.defVal(Byte.class, name);
                    }
                    super.visit(name, value);
                }

                private <T> T c(Object v) {
                    return $.cast(v);
                }
            }

            private class BindAnnotationVisitor extends ParamAnnotationVisitorBase<BindAnnoInfo> {
                public BindAnnotationVisitor(AnnotationVisitor av, int index) {
                    super(av, index);
                }

                @Override
                protected BindAnnoInfo createAnnotationInfo(int index) {
                    return new BindAnnoInfo(index);
                }

                @Override
                public void visit(String name, Object value) {
                    if ("model".endsWith(name)) {
                        info.model((String) value);
                    }
                }

                @Override
                public AnnotationVisitor visitArray(String name) {
                    AnnotationVisitor av = super.visitArray(name);
                    if ("value".equals(name)) {
                        return new AnnotationVisitor(ASM5, av) {
                            @Override
                            public void visit(String name, Object value) {
                                Type type = (Type) value;
                                Class<? extends Binder> c = $.classForName(type.getClassName(), getClass().getClassLoader());
                                info.binder(c);
                            }
                        };
                    }
                    return av;
                }
            }
        }

        private List<Router> routers() {
            final List<Router> routers = C.newList();
            if (null == ports || ports.length == 0) {
                routers.add(app().router());
            } else {
                App app = app();
                for (String portName : ports) {
                    Router r = app.router(portName);
                    if (null == r) {
                        if (S.eq(AppConfig.PORT_CLI_OVER_HTTP, portName)) {
                            // cli over http is disabled
                            return routers;
                        }
                        throw E.invalidConfiguration("Cannot find configuration for named port[%s]", portName);
                    }
                    routers.add(r);
                }
            }
            return routers;
        }
    }

    private static class RouteRegister implements Runnable {
        List<Router> routers;
        List<String> paths;
        String methodName;
        ControllerClassMetaInfo classInfo;
        List<H.Method> httpMethods;
        $.Var<Boolean> isVirtual;
        boolean noRegister; // do not register virtual method of an abstract class

        RouteRegister(List<H.Method> methods, List<String> paths, String methodName, List<Router> routers, ControllerClassMetaInfo classInfo, boolean noRegister, $.Var<Boolean> isVirtual) {
            this.routers = routers;
            this.paths = paths;
            this.methodName = methodName;
            this.classInfo = classInfo;
            this.httpMethods = methods;
            this.noRegister = noRegister;
            this.isVirtual = isVirtual;
        }

        @Override
        public void run() {
            final Set<String> contexts = new HashSet<>();
            if (!noRegister) {
                String contextPath = classInfo.contextPath();
                String className = classInfo.className();
                String action = WsEndpoint.PSEUDO_METHOD == methodName ? methodName : S.concat(className, ".", methodName);
                registerOnContext(contextPath, action);
                contexts.add(contextPath);
            }

            if (!isVirtual.get()) {
                // not virtual handler method, so don't need to register sub class routes
                return;
            }

            // now check on sub classes
            App app = Act.app();
            final AppClassLoader classLoader = app.classLoader();
            ClassNode node = classLoader.classInfoRepository().node(classInfo.className());
            node.visitSubTree(new $.Visitor<ClassNode>() {
                @Override
                public void visit(ClassNode classNode) throws Osgl.Break {
                    String className = classNode.name();
                    ControllerClassMetaInfo subClassInfo = classLoader.controllerClassMetaInfo(className);
                    if (null != subClassInfo) {
                        String subClassContextPath = subClassInfo.contextPath();
                        if (null != subClassContextPath) {
                            if (!contexts.contains(subClassContextPath)) {
                                registerOnContext(subClassContextPath, S.builder(subClassInfo.className()).append(".").append(methodName).toString());
                                contexts.add(subClassContextPath);
                            } else {
                                throw E.invalidConfiguration("the context path of Sub controller %s has already been registered: %s", className, subClassContextPath);
                            }
                        }
                    }
                }
            }, true, true);
        }

        private void registerOnContext(String ctxPath, String action) {
            S.Buffer sb = S.newBuffer();
            for (Router r : routers) {
                for (String urlPath : paths) {
                    if (!urlPath.startsWith("/")) {
                        if (!(S.blank(ctxPath) || "/".equals(ctxPath))) {
                            if (ctxPath.endsWith("/")) {
                                ctxPath = ctxPath.substring(0, ctxPath.length() - 1);
                            }
                            sb.setLength(0);
                            sb.append(ctxPath);
                            if (!urlPath.startsWith("/")) {
                                sb.append("/");
                            }
                            sb.append(urlPath);
                            urlPath = sb.toString();
                        }
                    }
                    for (H.Method m : httpMethods) {
                        try {
                            r.addMapping(m, urlPath, action, RouteSource.ACTION_ANNOTATION);
                        } catch (RuntimeException e) {
                            logger.error(e, "add router mapping failed: \n\tmethod[%s]\n\turl path: %s\n\taction: %s", m, urlPath, action);
                        }
                    }
                }
            }
        }
    }
}
