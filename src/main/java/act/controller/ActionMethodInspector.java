package act.controller;

import act.asm.*;
import act.controller.meta.ParamMetaInfo;
import org.osgl._;
import act.controller.meta.LocalVariableMetaInfo;
import org.osgl.util.E;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import static act.controller.meta.ControllerClassMetaInfo.isActionAnnotation;

public class ActionMethodInspector extends MethodVisitor
        implements Opcodes {

    ControllerClassEnhancer classVisitor;
    int access;
    String method;
    String actionStr;
    String desc;
    String signature;
    String[] exceptions;
    ActionMethodMetaInfoL spec = new ActionMethodMetaInfoL();

    public ActionMethodInspector(MethodVisitor mv, int access, String name, String desc, String signature, String[] exceptions, ControllerClassEnhancer classVisitor) {
        super(ASM5, mv);
        boolean isStatic = (ACC_STATIC & access) > 0;
        spec.name(name).invokeType(isStatic ? ActionMethodMetaInfoL.InvokeType.STATIC : ActionMethodMetaInfoL.InvokeType.VIRTUAL);
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
        boolean b = isActionAnnotation(c);
        return b ? new ActionAnnotationInspector(av, this, c) : av;
    }

    @Override
    public void visitParameter(String name, int access) {
        System.out.printf(">>>> param: %s\n", name);
        super.visitParameter(name, access);
    }

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
                if (ActionMethodMetaInfoL.APP_CONTEXT_TYPE.equals(param.type())) {
                    spec.appContextIndex(index);
                }
            }
            LocalVariableMetaInfo local = new LocalVariableMetaInfo(index, name, desc, start, end);
            spec.addLocal(local);
        }
        super.visitLocalVariable(name, desc, signature, start, end, index);
    }

    void annotationVisitEnded() {
        boolean isActionHandler = false;//App.router().isActionMethod(classVisitor.className, method);
        if (isActionHandler) {
            mv = new ActionMethodEnhancer(mv, spec, access, method, desc, signature, exceptions);
        }
    }

    String actionStr() {
        if (null == actionStr) {
            StringBuilder sb = new StringBuilder(classVisitor.className);
            sb.append(".").append(method);
            actionStr = sb.toString();
        }
        return actionStr;
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
    }
}
