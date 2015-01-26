package playground;

import org.osgl._;
import org.osgl.mvc.server.App;
import org.osgl.mvc.server.AppContext;
import org.osgl.mvc.server.asm.*;
import org.osgl.mvc.server.bytecode.ActionMethodMetaInfo;
import org.osgl.mvc.server.bytecode.LocalVariableMetaInfo;
import org.osgl.mvc.server.bytecode.ParamMetaInfo;
import org.osgl.util.E;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import static org.osgl.mvc.server.bytecode.ActionMethodMetaInfo.InvokeType.*;

public class ActionMethodDetector extends MethodVisitor
        implements Opcodes {

    ControllerClassVisitor classVisitor;
    int access;
    String method;
    String actionStr;
    String desc;
    String signature;
    String[] exceptions;
    ActionMethodMetaInfo spec = new ActionMethodMetaInfo();

    public ActionMethodDetector(MethodVisitor mv, int access, String name, String desc, String signature, String[] exceptions, ControllerClassVisitor classVisitor) {
        super(ASM5, mv);
        boolean isStatic = (ACC_STATIC & access) > 0;
        spec.name(name).invokeType(isStatic ? STATIC : VIRTUAL);
        this.access = access;
        this.method = name;
        this.classVisitor = classVisitor;
        this.desc = desc;
        this.signature = signature;
        this.exceptions = exceptions;
        Type retType = Type.getReturnType(desc);
        spec.returnType(retType);
        if (null != signature) {
            /* generic parameter binding will be supported later
            SignatureReader sr = new SignatureReader(signature);
            SignatureVisitor sv = new ActionMethodSignatureVisitor(this);
            sr.accept(sv);
            */
            throw E.tbd("generic type not supported");
        }
        Type[] argTypes = Type.getArgumentTypes(desc);
        for (int i = 0; i < argTypes.length; ++i) {
            Type type = argTypes[i];
            ParamMetaInfo param = new ParamMetaInfo().type(type);
            spec.addParam(param);
        }
        System.out.printf("%s, %s\n", retType, Arrays.toString(argTypes));
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
        return super.visitParameterAnnotation(parameter, desc, visible);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        AnnotationVisitor av = super.visitAnnotation(desc, visible);
        Type type = Type.getType(desc);
        String className = type.getClassName();
        Class<? extends Annotation> c = _.classForName(className);
        boolean b = App.isActionAnnotation(c);
        return b ? new ActionAnnotationInspector(av, this, c) : av;
    }

    private static final Type APP_CONTEXT_TYPE = Type.getType(AppContext.class);
    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index
    ) {
        if (!"this".equals(name)) {
            int paramId = index;
            if (!spec.isStatic()) {
                paramId--;
            }
            if (paramId < spec.paramCount()) {
                ParamMetaInfo param = spec.param(paramId);
                param.name(name);
                if (APP_CONTEXT_TYPE.equals(param.type())) {
                    spec.appContextIndex(index);
                }
            }
            LocalVariableMetaInfo local = new LocalVariableMetaInfo(index, name, desc, start, end);
            spec.addLocal(local);
        }
        super.visitLocalVariable(name, desc, signature, start, end, index);
    }

    void annotationVisitEnded() {
        boolean isActionHandler = App.router().isActionMethod(classVisitor.controller, method);
        if (isActionHandler) {
            mv = new ActionMethodEnhancer(mv, spec, access, method, desc, signature, exceptions);
        }
    }

    String actionStr() {
        if (null == actionStr) {
            StringBuilder sb = new StringBuilder(classVisitor.controller);
            sb.append(".").append(method);
            actionStr = sb.toString();
        }
        return actionStr;
    }

    @Override
    public void visitEnd() {
        System.out.println(spec);
        super.visitEnd();
    }
}
