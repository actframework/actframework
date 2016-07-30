package act.di.genie;

import act.app.AppByteCodeScannerBase;
import act.asm.AnnotationVisitor;
import act.asm.MethodVisitor;
import act.asm.Type;
import act.util.AsmTypes;
import act.util.ByteCodeVisitor;
import org.osgl.inject.Module;
import org.osgl.util.C;

import java.util.Set;

/**
 * The `GenieFactoryFinder` find classes that contains `@org.osgl.genie.annotation.Provides`
 * annotated factory methods
 */
public class GenieFactoryFinder extends AppByteCodeScannerBase {

    private static Set<String> factories = C.newSet();

    private boolean isFactory;

    private boolean isModule;

    @Override
    public ByteCodeVisitor byteCodeVisitor() {
        return new ByteCodeVisitor() {

            private boolean isPublic;

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                super.visit(version, access, name, signature, superName, interfaces);
                isPublic = AsmTypes.isPublic(access);
                isModule = Module.class.getName().equals(Type.getObjectType(superName).getClassName());
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                return !isPublic || isModule || isFactory ? mv : new MethodVisitor(ASM5, mv) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                        Type annoType = Type.getType(desc);
                        if (AsmTypes.PROVIDES.asmType().equals(annoType)) {
                            isFactory = true;
                        }
                        return super.visitAnnotation(desc, visible);
                    }
                };
            }
        };
    }

    @Override
    public void scanFinished(String className) {
        if (isFactory) {
            factories.add(className);
        }
    }

    @Override
    protected boolean shouldScan(String className) {
        return true;
    }

    public static Set<String> factories() {
        return factories;
    }
}
