package act.util;

import act.app.AppByteCodeScannerBase;
import act.asm.AnnotationVisitor;
import act.asm.MethodVisitor;
import act.asm.Type;

import static act.util.ClassFinderData.By.ANNOTATION;
import static act.util.ClassFinderData.By.SUPER_TYPE;

/**
 * Scans all public non-abstract methods for {@link SubClassFinder}
 * annotations. If found then it will create a {@link ClassFinderData}
 * and schedule it to run finding process
 */
public class ClassFinderByteCodeScanner extends AppByteCodeScannerBase {

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
    }

    private class _ByteCodeVisitor extends ByteCodeVisitor {

        private String className;

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            if (!AsmTypes.isPublic(access)) {
                return;
            }
            className = Type.getObjectType(name).getClassName();
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, final String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if (null == className || !AsmTypes.isPublicNotAbstract(access)) {
                return mv;
            }
            final String methodName = name;
            final boolean isStatic = AsmTypes.isStatic(access);
            return new MethodVisitor(ASM5, mv) {
                @Override
                public AnnotationVisitor visitAnnotation(final String desc, boolean visible) {
                    Type annoType = Type.getType(desc);
                    ClassFinderData.By by = null;
                    if (AsmTypes.SUB_CLASS_FINDER.asmType().equals(annoType)) {
                        by = SUPER_TYPE;
                    } else if (AsmTypes.ANN_CLASS_FINDER.asmType().equals(annoType)) {
                        by = ANNOTATION;
                    }
                    AnnotationVisitor av = super.visitAnnotation(desc, visible);
                    if (null == by) {
                        return av;
                    }
                    final ClassFinderData.By how = by;
                    return new AnnotationVisitor(ASM5, av) {

                        ClassFinderData finder = new ClassFinderData();
                        String what = SubClassFinder.DEF_VALUE;

                        @Override
                        public void visit(String name, Object value) {
                            Type type = (Type)value;
                            String className = type.getClassName();
                            finder.what(className);
                            super.visit(name, value);
                        }

                        @Override
                        public void visitEnum(String name, String desc, String value) {
                            finder.when(value);
                            super.visitEnum(name, desc, value);
                        }

                        @Override
                        public void visitEnd() {
                            if (SubClassFinder.DEF_VALUE.equals(what)) {
                                what = classNameFromMethodSignature();
                            }
                            finder.what(what);
                            finder.how(how);
                            finder.callback(className, methodName, isStatic);
                            if (finder.isValid()) {
                                finder.scheduleFind();
                            }
                        }

                        /*
                         * Valid method signature should be something like
                         * (Ljava/lang/Class<Lorg/osgl/aaa/AAAPersistentService;>;)V
                         * And we need to get the type descriptor "Lorg/osgl/aaa/AAAPersistentService;"
                         * from inside
                         */
                        private String classNameFromMethodSignature() {
                            String descriptor = signature.substring(18);
                            descriptor = descriptor.substring(0, descriptor.length() - 4);
                            return Type.getType(descriptor).getClassName();
                        }
                    };
                }
            };
        }
    }

}
