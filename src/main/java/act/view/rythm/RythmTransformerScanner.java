package act.view.rythm;

import act.Act;
import act.app.AppByteCodeScannerBase;
import act.app.event.AppEventId;
import act.asm.AnnotationVisitor;
import act.asm.MethodVisitor;
import act.asm.Type;
import act.util.AsmTypes;
import act.util.ByteCodeVisitor;
import org.osgl.$;
import org.osgl.util.S;
import org.rythmengine.extension.Transformer;

/**
 * Search for method that has {@link org.rythmengine.extension.Transformer} annotation.
 * Register the class if such method found
 */
public class RythmTransformerScanner extends AppByteCodeScannerBase {

    private static final String TRANSFORMER = Transformer.class.getName();

    @Override
    protected boolean shouldScan(String className) {
        return true;
    }

    @Override
    public ByteCodeVisitor byteCodeVisitor() {
        return new Visitor();
    }

    @Override
    public void scanFinished(String className) {
    }

    private class Visitor extends ByteCodeVisitor {

        boolean found;
        String className;

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            if (!AsmTypes.isPublic(access)) {
                return;
            }
            className = Type.getObjectType(name).getClassName();
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            if (!found) {
                String className = Type.getType(desc).getClassName();
                if (S.eq(TRANSFORMER, className)) {
                    found = true;
                }
            }
            return super.visitAnnotation(desc, visible);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if (found) {
                return mv;
            }
            return new MethodVisitor(ASM5, mv) {
                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    if (!found) {
                        String className = Type.getType(desc).getClassName();
                        if (S.eq(TRANSFORMER, className)) {
                            found = true;
                        }
                    }
                    return super.visitAnnotation(desc, visible);
                }
            };
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            if (found) {
                app().jobManager().on(AppEventId.PRE_START, new Runnable() {
                    @Override
                    public void run() {
                        RythmView rythmView = (RythmView) Act.viewManager().view(RythmView.ID);
                        rythmView.registerTransformer(app(), $.classForName(className, app().classLoader()));
                    }
                });
            }
        }
    }
}
