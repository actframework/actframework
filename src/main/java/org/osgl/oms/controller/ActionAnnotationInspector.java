package org.osgl.oms.controller;

import org.osgl.http.H;
import org.osgl.mvc.annotation.*;
import org.osgl.oms.asm.AnnotationVisitor;
import org.osgl.oms.asm.Opcodes;
import org.osgl.oms.route.Router;
import org.osgl.util.C;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.osgl.http.H.Method.*;

public class ActionAnnotationInspector extends AnnotationVisitor implements Opcodes {

    private ActionMethodInspector methodVisitor;
    private List<H.Method> httpMethods = C.newList();
    private String path;

    public ActionAnnotationInspector(AnnotationVisitor av, ActionMethodInspector detector, Class<? extends Annotation> c) {
        super(ASM5, av);
        methodVisitor = detector;
        H.Method method = methodFromAnnotationClass(c);
        if (null != method) {
            httpMethods.add(method);
        }
    }

    @Override
    public void visit(String name, Object value) {
        super.visit(name, value);
        path = value.toString();
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        if ("methods".equals(name)) {
            return new MethodEnumVisitor(av, this);
        } else {
            return super.visitArray(name);
        }
    }

    @Override
    public void visitEnd() {
        int len = httpMethods.size();
        for (int i = 0; i < len; ++i) {
            H.Method method = httpMethods.get(i);
            String actionStr = methodVisitor.actionStr();
            //App.router().addMappingIfNotMapped(method, path, actionStr);
        }
        super.visitEnd();
        methodVisitor.annotationVisitEnded();
    }


    private static Map<Class, H.Method> methodMap = C.map(
            GetAction.class, GET,
            PostAction.class, POST,
            PutAction.class, PUT,
            DeleteAction.class, DELETE
    );

    private static H.Method methodFromAnnotationClass(Class<? extends Annotation> cls) {
        if (GetAction.class == cls) return GET;
        if (PostAction.class == cls) return POST;
        if (Action.class == cls) return null;
        if (DeleteAction.class == cls) return DELETE;
        if (PutAction.class == cls) return PUT;
        return methodMap.get(cls);
    }

    private static class MethodEnumVisitor extends AnnotationVisitor {
        ActionAnnotationInspector parent;

        MethodEnumVisitor(AnnotationVisitor av, ActionAnnotationInspector parent) {
            super(ASM5, av);
            this.parent = parent;
        }

        @Override
        public void visitEnum(String name, String desc, String value) {
            parent.httpMethods.add(H.Method.valueOf(value));
            super.visitEnum("method", desc, value);
        }

        @Override
        public void visitEnd() {
            if (parent.httpMethods.isEmpty()) {
                Collections.addAll(parent.httpMethods, Router.supportedHttpMethods());
            }
            super.visitEnd();
        }
    }
}
