package playground;

import org.osgl.mvc.result.Result;
import org.osgl.mvc.server.asm.ClassVisitor;
import org.osgl.mvc.server.asm.MethodVisitor;
import org.osgl.mvc.server.asm.Opcodes;
import org.osgl.mvc.server.asm.Type;

public class ControllerClassVisitor extends ClassVisitor
implements Opcodes {

    protected String controller;
    private boolean isAbstract;

    public ControllerClassVisitor(ClassVisitor cv) {
        super(ASM5, cv);
    }

    @Override
    public void visit(final int version, final int access, final String name,
            final String signature, final String superName,
            final String[] interfaces) {
        controller = name;
        isAbstract = (access & ACC_ABSTRACT) > 0;
        super.visit(version, access, name, signature, superName, interfaces);
        System.out.printf("visiting class %s ...\n", controller);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions
    ) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (shouldSkip(access, name, desc)) {
            return mv;
        }
        if (null != mv) {
            System.out.printf("visiting method %s.%s ...\n", controller, name);
            return new ActionMethodDetector(mv, access, name, desc, signature, exceptions, this);
        } else {
            return null;
        }
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

    private static final String CONTROLLER_BASE = Type.getType(CBase.class).getInternalName();
    private static final String SCAN_PKG = "playground";
    private boolean shouldSkip(int access, String name, String desc) {
        Type methodType = Type.getMethodType(desc);
        Type retType = methodType.getReturnType();
        boolean typeMatches = (Type.VOID_TYPE.equals(retType));
        typeMatches = typeMatches || Result.class.getName().equals(retType.getClassName());
        boolean pkgMatches = controller.startsWith(SCAN_PKG);
        return !typeMatches || isAbstract || !isPublic(access) || isConstructor(name) || !pkgMatches;
    }
}
