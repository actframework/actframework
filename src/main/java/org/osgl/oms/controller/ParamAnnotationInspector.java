package org.osgl.oms.controller;

import org.osgl.oms.asm.AnnotationVisitor;
import org.osgl.oms.asm.Opcodes;

import java.lang.annotation.Annotation;

public class ParamAnnotationInspector extends AnnotationVisitor implements Opcodes {

    private ActionMethodInspector methodVisitor;

    public ParamAnnotationInspector(AnnotationVisitor av, ActionMethodInspector detector, Class<? extends Annotation> c) {
        super(ASM5, av);
        methodVisitor = detector;
    }

    @Override
    public void visit(String name, Object value) {
        super.visit(name, value);
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        return super.visitArray(name);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        methodVisitor.annotationVisitEnded();
    }

}
