package playground;

import org.osgl.http.H;
import org.osgl.mvc.annotation.*;
import org.osgl.mvc.server.App;
import org.osgl.mvc.server.asm.AnnotationVisitor;
import org.osgl.mvc.server.asm.Opcodes;
import org.osgl.mvc.server.route.Router;
import org.osgl.util.C;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.osgl.http.H.Method.*;

public class ActionAnnotationInspector extends AnnotationVisitor implements Opcodes {

    ActionMethodDetector methodVistor;
    Class<? extends Annotation> cls;
    List<H.Method> methods = C.newList();
    String path;

    public ActionAnnotationInspector(AnnotationVisitor av, ActionMethodDetector detector, Class<? extends Annotation> c) {
        super(ASM5, av);
        methodVistor = detector;
        cls = c;
        H.Method method = methodFromAnnotationClass(c);
        if (null != method) {
            methods.add(method);
        }
    }

    @Override
    public void visit(String name, Object value) {
        super.visit(name, value);
        path = value.toString();
        System.out.printf("visiting annotation param: %s=%s\n", name, value);
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        System.out.printf("visiting annotation araray param: %s\n", name);
        if ("methods".equals(name)) {
            return new MethodEnumVisitor(av, this);
        } else {
            return super.visitArray(name);
        }
    }

    @Override
    public void visitEnd() {
        int len = methods.size();
        for (int i = 0; i < len; ++i) {
            H.Method method = methods.get(i);
            String actionStr = methodVistor.actionStr();
            App.router().addMappingIfNotMapped(method, path, actionStr);
        }
        super.visitEnd();
        methodVistor.annotationVisitEnded();
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
            parent.methods.add(H.Method.valueOf(value));
            super.visitEnum("method", desc, value);
        }

        @Override
        public void visitEnd() {
            if (parent.methods.isEmpty()) {
                Collections.addAll(parent.methods, Router.supportedHttpMethods());
            }
            super.visitEnd();
        }
    }
}
