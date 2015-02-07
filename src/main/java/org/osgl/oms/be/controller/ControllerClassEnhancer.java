package org.osgl.oms.be.controller;

import org.osgl.oms.asm.*;
import org.osgl.oms.be.Types;

public class ControllerClassEnhancer extends ClassVisitor implements Opcodes {

    protected String className;
    private boolean isAbstract;

    public ControllerClassEnhancer(ClassVisitor cv) {
        super(ASM5, cv);
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
    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions ) {
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

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return super.visitAnnotation(desc, visible);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        return super.visitTypeAnnotation(typeRef, typePath, desc, visible);
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

    private boolean shouldSkip(int access, String name, String desc) {
        Type methodType = Type.getMethodType(desc);
        Type retType = methodType.getReturnType();
        boolean typeMatches = (Type.VOID_TYPE.equals(retType));
        typeMatches = typeMatches || Types.RESULT_TYPE.equals(retType);
        return !typeMatches || isAbstract || !isPublic(access) || isConstructor(name);
    }
}
