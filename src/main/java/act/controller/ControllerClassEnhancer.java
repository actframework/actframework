package act.controller;

import act.asm.ClassVisitor;
import act.util.AsmTypes;
import act.asm.FieldVisitor;
import act.asm.MethodVisitor;
import act.asm.Type;
import act.util.ByteCodeVisitor;

public class ControllerClassEnhancer extends ByteCodeVisitor {

    protected String className;
    private boolean isAbstract;

    public ControllerClassEnhancer() {
    }

    public ControllerClassEnhancer(ClassVisitor cv) {
        super(cv);
    }

    @Override
    public void visit(final int version, final int access, final String name,
                      final String signature, final String superName,
                      final String[] interfaces) {
        className = name;
        isAbstract = isAbstract(access);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (shouldSkip(access, name, desc)) {
            return mv;
        }
        if (null != mv) {
            return new ActionMethodInspector(mv, access, name, desc, signature, exceptions, this);
        } else {
            return null;
        }
    }

    private boolean shouldSkip(int access, String name, String desc) {
        Type methodType = Type.getMethodType(desc);
        Type retType = methodType.getReturnType();
        boolean typeMatches = (Type.VOID_TYPE.equals(retType));
        typeMatches = typeMatches || AsmTypes.RESULT_TYPE.equals(retType);
        return !typeMatches || isAbstract || !isPublic(access) || isConstructor(name);
    }
}
