package act.controller.bytecode;

import act.app.AppByteCodeScannerBase;
import act.asm.*;
import act.asm.signature.SignatureReader;
import act.asm.signature.SignatureVisitor;
import act.controller.Controller;
import act.controller.meta.*;
import act.util.GeneralAnnoInfo;
import act.util.AsmTypes;
import org.osgl._;
import org.osgl.http.H;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.mvc.annotation.With;
import act.route.Router;
import act.util.ByteCodeVisitor;
import org.osgl.mvc.util.Binder;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.ListBuilder;
import org.osgl.util.S;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

/**
 * New controller scanner implementation
 */
public class ControllerByteCodeScanner extends AppByteCodeScannerBase {

    private final static Logger logger = L.get(ControllerByteCodeScanner.class);
    private Router router;
    private ControllerClassMetaInfo classInfo;
    private volatile ControllerClassMetaInfoManager classInfoBase;

    @Override
    protected boolean shouldScan(String className) {
        boolean isController = config().possibleControllerClass(className);
        classInfo = new ControllerClassMetaInfo().isController(isController);
        return isController;
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
        classInfoBase().registerControllerMetaInfo(classInfo);
    }

    @Override
    public void allScanFinished() {
        classInfoBase().mergeActionMetaInfo();
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
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            if (classInfo.isController() && AsmTypes.APP_CONTEXT_DESC.equals(desc)) {
                classInfo.ctxField(name, isPrivate(access));
            }
            return super.visitField(access, name, desc, signature, value);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            AnnotationVisitor av = super.visitAnnotation(desc, visible);
            if (Type.getType(Controller.class).getDescriptor().equals(desc)) {
                classInfo.isController(true);
                return new ControllerAnnotationVisitor(av);
            }
            if (Type.getType(With.class).getDescriptor().equals(desc)) {
                return new WithAnnotationVisitor(av);
            }
            return super.visitAnnotation(desc, visible);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if (!classInfo.isController() || !isEligibleMethod(access, name, desc)) {
                return mv;
            }
            String className = classInfo.className();
            boolean isRoutedMethod = router.isActionMethod(className, name);
            return new ActionMethodVisitor(isRoutedMethod, mv, access, name, desc, signature, exceptions);
        }

        private boolean isEligibleMethod(int access, String name, String desc) {
            return isPublic(access) && !isAbstract(access) && !isConstructor(name);
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

        private class WithAnnotationVisitor extends AnnotationVisitor {
            public WithAnnotationVisitor(AnnotationVisitor av) {
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
        }

        private class ActionMethodVisitor extends MethodVisitor implements Opcodes {

            private String methodName;
            private int access;
            private String desc;
            private String signature;
            private String[] exceptions;
            private boolean requireScan;
            private boolean isRoutedMethod;
            private HandlerMethodMetaInfo methodInfo;
            private Map<Integer, List<ParamAnnoInfoTrait>> paramAnnoInfoList = C.newMap();
            private Map<Integer, List<GeneralAnnoInfo>> genericParamAnnoInfoList = C.newMap();

            ActionMethodVisitor(boolean isRoutedMethod, MethodVisitor mv, int access, String methodName, String desc, String signature, String[] exceptions) {
                super(ASM5, mv);
                this.isRoutedMethod = isRoutedMethod;
                this.access = access;
                this.methodName = methodName;
                this.desc = desc;
                this.signature = signature;
                this.exceptions = exceptions;
                if (isRoutedMethod) {
                    markRequireScan();
                }
            }

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                AnnotationVisitor av = super.visitAnnotation(desc, visible);
                Type type = Type.getType(desc);
                String className = type.getClassName();
                Class<? extends Annotation> c = _.classForName(className);
                if (ControllerClassMetaInfo.isActionAnnotation(c)) {
                    markRequireScan();
                    ActionMethodMetaInfo tmp = new ActionMethodMetaInfo(classInfo);
                    methodInfo = tmp;
                    classInfo.addAction(tmp);
                    return new ActionAnnotationVisitor(av, AnnotationMethodLookup.get(c));
                } else if (ControllerClassMetaInfo.isInterceptorAnnotation(c)) {
                    markRequireScan();
                    InterceptorAnnotationVisitor visitor = new InterceptorAnnotationVisitor(av, c);
                    methodInfo = visitor.info;
                    return visitor;
                }
                //markNotTargetClass();
                return av;
            }

            @Override
            public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
                AnnotationVisitor av = super.visitParameterAnnotation(parameter, desc, visible);
                Type type = Type.getType(desc);
                if (_.eq(type, AsmTypes.PARAM.asmType())) {
                    return new ParamAnnotationVisitor(av, parameter);
                } else if (_.eq(type, AsmTypes.BIND.asmType())) {
                    return new BindAnnotationVisitor(av, parameter);
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
                if (null == methodInfo) {
                    ActionMethodMetaInfo action = new ActionMethodMetaInfo(classInfo);
                    methodInfo = action;
                    classInfo.addAction(action);
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
                    if (AsmTypes.APP_CONTEXT.asmType().equals(type)) {
                        ctxByParam = true;
                        info.appContextViaParam(i);
                    }
                    ParamMetaInfo param = new ParamMetaInfo().type(type);
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
                    final _.Var<Integer> id = new _.Var<>(-1);
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
                            if (startParsing) info.param(id.get()).componentType(Type.getObjectType(name));
                            startParsing = false;
                        }
                    });
                }
                super.visitEnd();
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

                public ActionAnnotationVisitor(AnnotationVisitor av, H.Method method) {
                    super(ASM5, av);
                    if (null != method) {
                        httpMethods.add(method);
                    }
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
                    if (httpMethods.isEmpty()) {
                        // start(*) match
                        httpMethods.addAll(H.Method.actionMethods());
                    }
                    StringBuilder sb = S.builder(classInfo.className().replace('/', '.')).append(".").append(methodName);
                    String action = sb.toString();

                    for (String actionPath : paths) {
                        String ctxPath = classInfo.contextPath();
                        if (!(S.blank(ctxPath) || "/".equals(ctxPath))) {
                            if (ctxPath.endsWith("/")) {
                                ctxPath = ctxPath.substring(0, ctxPath.length() - 1);
                            }
                            sb = new StringBuilder(ctxPath);
                            if (!actionPath.startsWith("/")) {
                                sb.append("/");
                            }
                            sb.append(actionPath);
                            actionPath = sb.toString();
                        }
                        for (H.Method m : httpMethods) {
                            router.addMappingIfNotMapped(m, actionPath, action);
                        }
                    }
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
                    } else if (S.eq("resolveClass", name)) {
                        info.resolver((Class) value);
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
                    return _.cast(v);
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
                    } else if ("value".endsWith(name)) {
                        Type type = (Type) value;
                        Class<? extends Binder> c = _.classForName(type.getClassName(), getClass().getClassLoader());
                        info.binder(c);
                    }
                }
            }
        }
    }

}
