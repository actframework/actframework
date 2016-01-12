package act.job.bytecode;

import act.ActComponent;
import act.app.AppByteCodeScannerBase;
import act.app.event.AppEventId;
import act.asm.*;
import act.job.JobAnnotationProcessor;
import act.job.meta.JobClassMetaInfo;
import act.job.meta.JobClassMetaInfoManager;
import act.job.meta.JobMethodMetaInfo;
import act.util.AsmTypes;
import act.util.ByteCodeVisitor;
import org.osgl.$;
import org.osgl.util.E;
import org.osgl.util.ListBuilder;

import java.lang.annotation.Annotation;

/**
 * Scan class to collect Job class meta info
 */
@ActComponent
public class JobByteCodeScanner extends AppByteCodeScannerBase {

    private JobAnnotationProcessor annotationProcessor;
    private JobClassMetaInfo classInfo;
    private volatile JobClassMetaInfoManager classInfoBase;

    @Override
    protected boolean shouldScan(String className) {
        classInfo = new JobClassMetaInfo();
        return true;
    }

    @Override
    protected void onAppSet() {
        annotationProcessor = new JobAnnotationProcessor(app());
    }

    @Override
    public ByteCodeVisitor byteCodeVisitor() {
        return new _ByteCodeVisitor();
    }

    @Override
    public void scanFinished(String className) {
        classInfoBase().registerJobMetaInfo(classInfo);
    }

    private JobClassMetaInfoManager classInfoBase() {
        if (null == classInfoBase) {
            synchronized (this) {
                if (null == classInfoBase) {
                    classInfoBase = app().classLoader().jobClassMetaInfoManager();
                }
            }
        }
        return classInfoBase;
    }

    private class _ByteCodeVisitor extends ByteCodeVisitor {
        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            classInfo.className(name);
            Type superType = Type.getObjectType(superName);
            classInfo.superType(superType);
            if (isAbstract(access)) {
                classInfo.setAbstract();
            }
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            if (AsmTypes.APP_DESC.equals(desc)) {
                classInfo.appField(name, isPrivate(access));
            } else if (AsmTypes.APP_CONFIG_DESC.equals(desc)) {
                classInfo.appConfigField(name, isPrivate(access));
            }
            return super.visitField(access, name, desc, signature, value);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if (!isEligibleMethod(access, name, desc)) {
                return mv;
            }
            return new JobMethodVisitor(mv, access, name, desc, signature, exceptions);
        }

        private boolean isEligibleMethod(int access, String name, String desc) {
            // TODO: analyze parameters
            return isPublic(access) && !isAbstract(access) && !isConstructor(name) && (desc.startsWith("()"));
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

        private class JobMethodVisitor extends MethodVisitor implements Opcodes {

            private String methodName;
            private int access;
            private boolean requireScan;
            private JobMethodMetaInfo methodInfo;
            private ActionAnnotationVisitor av;

            JobMethodVisitor(MethodVisitor mv, int access, String methodName, String desc, String signature, String[] exceptions) {
                super(ASM5, mv);
                this.access = access;
                this.methodName = methodName;
            }

            @SuppressWarnings("unchecked")
            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                AnnotationVisitor av = super.visitAnnotation(desc, visible);
                Type type = Type.getType(desc);
                String className = type.getClassName();
                try {
                    Class<? extends Annotation> c = (Class<? extends Annotation>)Class.forName(className);
                    if (JobClassMetaInfo.isActionAnnotation(c)) {
                        markRequireScan();
                        JobMethodMetaInfo tmp = new JobMethodMetaInfo(classInfo);
                        methodInfo = tmp;
                        classInfo.addAction(tmp);
                        this.av = new ActionAnnotationVisitor(av, c, methodInfo);
                        return this.av;
                    }
                } catch (Exception e) {
                    throw E.unexpected(e);
                }
                //markNotTargetClass();
                return av;
            }

            @Override
            public void visitEnd() {
                if (!requireScan()) {
                    super.visitEnd();
                    return;
                }
                JobMethodMetaInfo info = methodInfo;
                info.name(methodName);
                boolean isStatic = AsmTypes.isStatic(access);
                if (isStatic) {
                    info.invokeStaticMethod();
                } else {
                    info.invokeInstanceMethod();
                }
                if (null != av) av.doRegistration();
                super.visitEnd();
            }

            private void markRequireScan() {
                this.requireScan = true;
            }

            private boolean requireScan() {
                return requireScan;
            }

            private class ActionAnnotationVisitor extends AnnotationVisitor implements Opcodes {

                Object value;
                Object async;
                JobMethodMetaInfo method;
                Class<? extends Annotation> c;

                public ActionAnnotationVisitor(AnnotationVisitor av,Class<? extends Annotation> c, JobMethodMetaInfo methodMetaInfo) {
                    super(ASM5, av);
                    this.c = c;
                    this.method = methodMetaInfo;
                }

                @Override
                public void visitEnum(String name, String desc, String value) {
                    if (desc.contains("AppEventId")) {
                        this.value = AppEventId.valueOf(value);
                    }
                }

                @Override
                public void visit(String name, Object value) {
                    if ("value".equals(name)) {
                        this.value = value;
                    }
                    if ("async".equals(name)) {
                        this.async = value;
                    }
                }

                public void doRegistration() {
                    if (value != null && async != null) {
                        value = $.T2(value, async);
                    } else if (value == null) {
                        value = async;
                    }
                    annotationProcessor.register(method, c, value);
                }
            }
        }
    }

}
