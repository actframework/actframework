package act.controller;

import act.asm.Opcodes;
import act.asm.signature.SignatureVisitor;

public class ActionMethodSignatureVisitor extends SignatureVisitor implements Opcodes {
    ActionMethodInspector detector;

    public ActionMethodSignatureVisitor(ActionMethodInspector detector) {
        super(ASM5);
        this.detector = detector;
    }

    @Override
    public void visitTypeVariable(String name) {
        System.out.printf("sig type variable %s\n", name);
        super.visitTypeVariable(name);
    }

    @Override
    public void visitBaseType(char descriptor) {
        System.out.printf("sig base type %s\n", descriptor);
        super.visitBaseType(descriptor);
    }

    @Override
    public void visitClassType(String name) {
        System.out.printf("sig class type %s\n", name);
        super.visitClassType(name);
    }

    @Override
    public void visitFormalTypeParameter(String name) {
        System.out.printf("sig formal type parameter %s\n", name);
        super.visitFormalTypeParameter(name);
    }

    @Override
    public void visitInnerClassType(String name) {
        System.out.printf("sig inner class type %s\n", name);
        super.visitInnerClassType(name);
    }

    @Override
    public SignatureVisitor visitClassBound() {
        System.out.printf("sig visit class bound\n");
        return super.visitClassBound();
    }

    @Override
    public SignatureVisitor visitParameterType() {
        System.out.printf("sig visit parameter type\n");
        return super.visitParameterType();
    }

    @Override
    public SignatureVisitor visitReturnType() {
        System.out.printf("sig visit return type\n");
        return super.visitReturnType();
    }
}
