package org.osgl.oms.controller.bytecode;

import org.osgl._;
import org.osgl.http.H;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.mvc.annotation.With;
import org.osgl.oms.asm.*;
import org.osgl.oms.controller.meta.*;
import org.osgl.oms.route.Router;
import org.osgl.oms.util.AsmTypes;
import org.osgl.oms.util.BytecodeVisitor;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.ListBuilder;
import org.osgl.util.S;

import java.lang.annotation.Annotation;
import java.util.List;

import static org.osgl.oms.controller.meta.ControllerClassMetaInfo.isActionAnnotation;
import static org.osgl.oms.controller.meta.ControllerClassMetaInfo.isInterceptorAnnotation;

/**
 * Controller action method scanner. Used to build router from
 * annotations associated with controller action methods
 */
public final class ControllerScanner extends BytecodeVisitor {

    private final static Logger logger = L.get(ControllerScanner.class);

    private Router router;
    private _.Func1<String, byte[]> bytecodeLookup;
    private ControllerClassMetaInfo classInfo;

    public ControllerScanner(Router router, _.Func1<String, byte[]> bytecodeLookup) {
        super(null);
        E.NPE(router, bytecodeLookup);
        this.router = router;
        this.bytecodeLookup = bytecodeLookup;
    }

    private static boolean isConstructor(String methodName) {
        return methodName.contains("<init>");
    }

    private static boolean isPublic(int access) {
        return (ACC_PUBLIC & access) > 0;
    }

    private static boolean isAbstract(int access) {
        return (ACC_ABSTRACT & access) > 0;
    }

    public ControllerClassMetaInfo scan(String className) {
        byte[] bytecode = bytecodeLookup.apply(className);
        if (null != bytecode) {
            return scan(bytecode);
        } else {
            logger.warn("cannot find bytecode for class: %s", className);
            return null;
        }
    }

    public ControllerClassMetaInfo scan(byte[] bytecode) {
        ClassReader cr = new ClassReader(bytecode);
        cr.accept(this, 0);
        return classInfo;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        classInfo = new ControllerClassMetaInfo().className(name);
        if (isAbstract(access)) {
            classInfo.setAbstract();
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (!classInfo.isAbstract() && AsmTypes.APP_CONTEXT_DESC.equals(desc)) {
            classInfo.ctxField(name);
        }
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        AnnotationVisitor av = super.visitAnnotation(desc, visible);
        if (Type.getType(With.class).getDescriptor().equals(desc)) {
            return new WithAnnotationVisitor(av);
        }
        return super.visitAnnotation(desc, visible);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (isEligibleMethod(access, name, desc)) {
            String className = classInfo.className();
            boolean isRoutedMethod = router.isActionMethod(className, name);
            return new ActionMethodVisitor(isRoutedMethod, mv, access, name, desc, signature, exceptions);
        }
        return mv;
    }

    private void markNotTargetClass() {
        classInfo = null;
    }

    private boolean isEligibleMethod(int access, String name, String desc) {
        return isPublic(access) && !isAbstract(access) && !isConstructor(name);
    }

    private static class StringArrayVisitor extends AnnotationVisitor {
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

    private class ActionMethodVisitor extends MethodVisitor implements Opcodes {

        private String methodName;
        private int access;
        private String desc;
        private String signature;
        private String[] exceptions;
        private boolean requireScan;
        private boolean isRoutedMethod;
        private ActionMethodMetaInfoBase methodInfo;

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
            if (isActionAnnotation(c)) {
                markRequireScan();
                ActionMethodMetaInfo tmp = new ActionMethodMetaInfo();
                methodInfo = tmp;
                classInfo.addAction(tmp);
                return new ActionAnnotationVisitor(av);
            } else if (isInterceptorAnnotation(c)) {
                markRequireScan();
                InterceptorAnnotationVisitor visitor = new InterceptorAnnotationVisitor(av, c);
                methodInfo = visitor.info;
                return visitor;
            }
            markNotTargetClass();
            return av;
        }

        @Override
        public void visitEnd() {
            if (!requireScan()) {
                return;
            }
            if (null == methodInfo) {
                ActionMethodMetaInfo action = new ActionMethodMetaInfo();
                methodInfo = action;
                classInfo.addAction(action);
            }
            ActionMethodMetaInfoBase info = methodInfo;
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
                info.addParam(param);
            }
            if (!ctxByParam) {
                if (classInfo.hasCtxField() && !isStatic) {
                    info.appContextViaField(classInfo.ctxField());
                } else {
                    info.appContextViaLocalStorage();
                }
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
            String path;

            public ActionAnnotationVisitor(AnnotationVisitor av) {
                super(ASM5, av);
            }

            @Override
            public void visit(String name, Object value) {
                if ("value".equals(name)) {
                    path = value.toString();
                }
            }

            @Override
            public void visitEnum(String name, String desc, String value) {
                if (null == name) {
                    name = Type.getType(desc).getClassName();
                }
                if (H.Method.class.getName().equals(name)) {
                    H.Method method = H.Method.valueOf(value);
                    httpMethods.add(method);
                }
                super.visitEnum(name, desc, value);
            }

            @Override
            public void visitEnd() {
                if (httpMethods.isEmpty()) {
                    // start(*) match
                    httpMethods.addAll(H.Method.actionMethods());
                }
                StringBuilder sb = S.builder(classInfo.className().replace('/', '.')).append(".").append(methodName);
                String action = sb.toString();
                for (H.Method m : httpMethods) {
                    router.addMappingIfNotMapped(m, path, action);
                }
            }
        }
    }

}
