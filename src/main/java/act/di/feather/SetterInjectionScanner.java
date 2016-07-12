package act.di.feather;

import act.ActComponent;
import act.app.App;
import act.app.AppByteCodeScanner;
import act.app.AppByteCodeScannerBase;
import act.app.AppSourceCodeScanner;
import act.asm.AnnotationVisitor;
import act.asm.MethodVisitor;
import act.asm.Type;
import act.util.AppCodeScannerPluginBase;
import act.util.ByteCodeVisitor;

import javax.inject.Inject;

/**
 * Print out warn log if user's application relies on setter injection
 * which is not supported by feather at the moment
 */
@ActComponent
public class SetterInjectionScanner extends AppCodeScannerPluginBase {

    @Override
    public AppSourceCodeScanner createAppSourceCodeScanner(App app) {
        return null;
    }

    @Override
    public AppByteCodeScanner createAppByteCodeScanner(App app) {
        return new _ByteCodeScanner();
    }

    @Override
    public boolean load() {
        return true;
    }

    private static class _ByteCodeScanner extends AppByteCodeScannerBase {
        private static final String INJECT_ANN_DESC = Type.getType(Inject.class).getDescriptor().intern();

        @Override
        public ByteCodeVisitor byteCodeVisitor() {
            return new ByteCodeVisitor() {

                private String className;

                @Override
                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                    super.visit(version, access, name, signature, superName, interfaces);
                    className = Type.getObjectType(name).getClassName();
                }

                @Override
                public MethodVisitor visitMethod(int access, final String methodName, String desc, String signature, String[] exceptions) {
                    MethodVisitor mv = super.visitMethod(access, methodName, desc, signature, exceptions);
                    if ("<init>".equals(methodName)) {
                        // we don't care about constructor
                        return mv;
                    }
                    return new MethodVisitor(ASM5, mv) {
                        @Override
                        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                            if (INJECT_ANN_DESC.equals(desc)) {
                                logger.warn("Feather does not support setter injection on %s.%s", className, methodName);
                            }
                            return super.visitAnnotation(desc, visible);
                        }
                    };
                }
            };
        }

        @Override
        public void scanFinished(String className) {
        }

        @Override
        protected boolean shouldScan(String className) {
            return true;
        }
    }

}
