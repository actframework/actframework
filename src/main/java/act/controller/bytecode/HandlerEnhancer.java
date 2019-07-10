package act.controller.bytecode;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import static act.asm.tree.AbstractInsnNode.*;

import act.app.ActionContext;
import act.asm.*;
import act.asm.tree.*;
import act.controller.meta.*;
import act.util.AsmTypes;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.mvc.result.Result;
import org.osgl.util.*;

import java.util.*;

public class HandlerEnhancer extends MethodVisitor implements Opcodes {

    private static final Logger logger = LogManager.get(HandlerEnhancer.class);

    private static final String RESULT_CLASS = Result.class.getName();

    private HandlerMethodMetaInfo info;
    private MethodVisitor next;
    private int paramIdShift = 0;
    private Set<Integer> skipNaming = new HashSet<>();
    private Map<Integer, String> overriddenNames = new HashMap<>();
    private boolean notAction;

    public HandlerEnhancer(final MethodVisitor mv, HandlerMethodMetaInfo meta, final int access, final String name, final String desc, final String signature, final String[] exceptions) {
        super(ASM5, new MethodNode(access, name, desc, signature, exceptions));
        if (logger.isTraceEnabled()) {
            logger.trace("enhance handler: " + meta.fullName());
        }
        this.info = meta;
        this.next = mv;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if ("Lact/controller/NotAction;".equals(desc)) {
            notAction = true;
        }
        return super.visitAnnotation(desc, visible);
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(final int parameter, String desc, boolean visible) {
        if ("Ljavax/inject/Named;".equals(desc)) {
            skipNaming.add(parameter);
        } else if ("Lorg/osgl/mvc/annotation/Param;".equals(desc)) {
            return new AnnotationVisitor(ASM5, super.visitParameterAnnotation(parameter, desc, visible)) {
                @Override
                public void visit(String name, Object value) {
                    if ("value".equals(name)) {
                        overriddenNames.put(parameter, S.string(value));
                    }
                    super.visit(name, value);
                }
            };
        }
        return super.visitParameterAnnotation(parameter, desc, visible);
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        if (!"this".equals(name)) {
            int paramId = index;
            if (!info.isStatic()) {
                paramId--;
            }
            paramId -= paramIdShift;
            if (paramId < info.paramCount()) {
                HandlerParamMetaInfo param = info.param(paramId);
                param.name(name);
                if (Type.getType(long.class).equals(param.type()) || Type.getType(double.class).equals(param.type())) {
                    paramIdShift++;
                }
            }
            LocalVariableMetaInfo local = new LocalVariableMetaInfo(index, name, desc, start, end);
            info.addLocal(local);
        }
        super.visitLocalVariable(name, desc, signature, start, end, index);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);
        AsmContext.line(line);
    }

    @Override
    public void visitEnd() {
        if (notAction) {
            super.visitEnd();
            return;
        }
        MethodNode mn = (MethodNode) mv;
        addParamAnnotations();
        transform(mn);
        mn.accept(next);
    }

    private void addParamAnnotations() {
        int sz = info.paramCount();
        for (int i = 0; i < sz; ++i) {
            if (!skipNaming.contains(i)) {
                String name = overriddenNames.get(i);
                if (null == name) name = info.param(i).name();
                AnnotationVisitor av = mv.visitParameterAnnotation(i, "Ljavax/inject/Named;", true);
                av.visit("value", name);
            }
        }
    }

    private void transform(MethodNode mn) {
        new Transformer(mn, info).doIt();
    }

    private static class Transformer {
        MethodNode mn;
        InsnList instructions;
        private HandlerMethodMetaInfo meta;
        List<Label> lblList = C.newSizedList(20);

        Transformer(MethodNode mn, HandlerMethodMetaInfo meta) {
            this.mn = mn;
            this.meta = meta;
            this.instructions = mergeRenderLineBreaks(mn.instructions);
        }

        void doIt() {
            ListIterator<AbstractInsnNode> itr = instructions.iterator();
            Segment cur = null;
            while (itr.hasNext()) {
                AbstractInsnNode insn = itr.next();
                if (insn.getType() == LABEL) {
                    cur = new Segment(((LabelNode) insn).getLabel(), meta, instructions, itr, this);
                } else if (null != cur) {
                    cur.handle(insn);
                }
            }
        }

        private InsnList mergeRenderLineBreaks(InsnList list) {
            ListIterator<AbstractInsnNode> itr = list.iterator();
            while (itr.hasNext()) {
                AbstractInsnNode insn = itr.next();
                if (isRenderLine(insn)) {
                    mergeRenderLineBreaks(insn, list);
                }
            }
            return list;
        }

        private void mergeRenderLineBreaks(AbstractInsnNode renderLine, InsnList list) {
            AbstractInsnNode node = renderLine.getPrevious();
            while (null != node) {
                if (node.getOpcode() == ANEWARRAY) {
                    return;
                } else if (node instanceof LabelNode) {
                    AbstractInsnNode node0 = node.getPrevious();
                    list.remove(node);
                    node = node0;
                } else {
                    node = node.getPrevious();
                }
            }
        }

        static boolean isRenderLine(AbstractInsnNode insnNode) {
            if (!(insnNode instanceof MethodInsnNode)) {
                return false;
            }
            MethodInsnNode node = (MethodInsnNode) insnNode;
            Type type = Type.getMethodType(node.desc);
            Type retType = type.getReturnType();
            return (isResult(retType) && node.desc.startsWith("([Ljava/lang/Object;)"));
        }

        static boolean isResult(Type type) {
            return ResultClassLookup.isResult(type.getClassName());
        }

        private static abstract class InstructionHandler {
            Segment segment;
            HandlerMethodMetaInfo meta;

            InstructionHandler(Segment segment, HandlerMethodMetaInfo meta) {
                this.segment = segment;
                this.meta = meta;
            }

            protected abstract void handle(AbstractInsnNode node);

            protected void refreshIteratorNext() {
                segment.itr.previous();
                segment.itr.next();
            }
        }

        private static class Segment {
            Label startLabel;
            InsnList instructions;
            HandlerMethodMetaInfo meta;
            ListIterator<AbstractInsnNode> itr;
            Transformer trans;
            private Map<Integer, InstructionHandler> handlers;

            Segment(Label start, HandlerMethodMetaInfo meta, InsnList instructions, ListIterator<AbstractInsnNode> itr, Transformer trans) {
                this.startLabel = start;
                this.meta = meta;
                this.instructions = instructions;
                this.itr = itr;
                this.trans = trans;
                this.handlers  = C.Map(
                        AbstractInsnNode.METHOD_INSN, new InvocationHandler(this, meta)
                );
                trans.lblList.add(start);
            }

            String varName(int index) {
                Label lbl = startLabel;
                int pos = -1;
                List<Label> lblList = trans.lblList;
                while (null != lbl) {
                    LocalVariableMetaInfo var = meta.localVariable(index, lbl);
                    if (null != var) return var.name();
                    if (-1 == pos) {
                        pos = lblList.indexOf(lbl);
                        if (pos <= 0) {
                            return null;
                        }
                    }
                    lbl = lblList.get(--pos);
                }
                logger.warn("Unable to locate var name for param #%n, possibly because source is compiled without debug info", index);
                return null;
            }

            void handle(AbstractInsnNode node) {
                InstructionHandler handler = handlers.get(node.getType());
                if (null != handler) {
                    handler.handle(node);
                }
            }
        }

        private static class InvocationHandler extends InstructionHandler {

            InvocationHandler(Segment segment, HandlerMethodMetaInfo meta) {
                super(segment, meta);
            }

            @Override
            protected void handle(AbstractInsnNode node) {
                MethodInsnNode n = (MethodInsnNode) node;
                Type type = Type.getMethodType(n.desc);
                Type retType = type.getReturnType();
                if (isResult(retType)) {
                    if (n.desc.startsWith("([Ljava/lang/Object;)")) {
                        injectRenderArgSetCode(n);
                    }
                    injectThrowCode(n);
                    meta.setThrowRenderResult();
                }
            }

            private String ctxFieldName() {
                return segment.meta.classInfo().ctxField();
            }

            private int ctxIndex() {
                return segment.meta.appCtxLocalVariableTableIndex();
            }

            @SuppressWarnings("FallThrough")
            private void injectRenderArgSetCode(AbstractInsnNode invokeNode) {
                AbstractInsnNode node = invokeNode.getPrevious();
                List<LoadInsnInfo> loadInsnInfoList = new ArrayList<>();
                String templateLiteral = null;
                String renderArgName = null;
                InsnList nodeList = new InsnList();
                boolean invalidParam = false;
                while (null != node) {
                    int type = node.getType();
                    boolean breakWhile = false;
                    switch (type) {
                        case LABEL:
                        case FRAME:
                            node = node.getNext();
                            breakWhile = true;
                            if (!invalidParam && null != renderArgName) {
                                loadInsnInfoList.add(new LoadInsnInfo(renderArgName, nodeList));
                            }
                            break;
                        case INSN:
                            switch (node.getOpcode()) {
                                case DUP:
                                    // need to check if previous insn is NEW which is invalid in param list
                                    AbstractInsnNode prev = node.getPrevious();
                                    if (NEW == prev.getOpcode()) {
                                        logger.warn("Invalid render argument found in %s: new object is not accepted", segment.meta.fullName());
                                        renderArgName = null;
                                        nodeList = new InsnList();
                                        invalidParam = false;
                                        node = prev.getPrevious();
                                        continue;
                                    }
                                    if (!invalidParam && null != renderArgName) {
                                        loadInsnInfoList.add(new LoadInsnInfo(renderArgName, nodeList));
                                    }
                                    renderArgName = null;
                                    nodeList = new InsnList();
                                    invalidParam = false;
                                case AASTORE:
                                case ICONST_0:
                                case ICONST_1:
                                case ICONST_2:
                                case ICONST_3:
                                case ICONST_4:
                                case ICONST_5:
                                    break;
                                case POP: // inline template
                                    invalidParam = true;
                                    break;
                                default:
                                    logger.warn("Invalid render argument found in %s: unknow opcode: %s", segment.meta.fullName(), node.getOpcode());
                                    invalidParam = true;
                            }
                            break;
                        case TYPE_INSN:
                            if (NEW == node.getOpcode()) {
                                logger.warn("Invalid render argument found in %s: new object is not accepted", segment.meta.fullName());
                                invalidParam = true;
                            }
                            break;
                        case VAR_INSN:
                            if (null == renderArgName) {
                                VarInsnNode vn = (VarInsnNode) node;
                                if (0 == vn.var && !segment.meta.isStatic()) {
                                    // skip "this"
                                } else {
                                    renderArgName = segment.varName(vn.var);
                                }
                            }
                            nodeList.insert(node.clone(C.<LabelNode, LabelNode>Map()));
                            break;
                        case METHOD_INSN:
                            if (invalidParam) {
                                break;
                            }
                            if (null == renderArgName) {
                                MethodInsnNode mn = (MethodInsnNode) node;
                                if (mn.desc.startsWith("()")) {
                                    // if method does not have parameter, e.g. `this.foo()` then
                                    // we take it's name as render arg name
                                    renderArgName = mn.name;
                                } else if (!"valueOf".equals(mn.name)) {
                                    // if method is not something like `Integer.valueOf` then
                                    // we say it is an invalid render parameter
                                    logger.warn("Invalid render argument found in %s: method with param is not supported", segment.meta.fullName());
                                    invalidParam = true;
                                }
                            }
                            nodeList.insert(node.clone(C.<LabelNode, LabelNode>Map()));
                            break;
                        case INT_INSN:
                            if (BIPUSH == node.getOpcode()) {
                                if (!invalidParam && null != renderArgName) {
                                    loadInsnInfoList.add(new LoadInsnInfo(renderArgName, nodeList));
                                }
                                renderArgName = null;
                                nodeList = new InsnList();
                                invalidParam = false;
                            }
                            break;
                        case FIELD_INSN:
                            if (invalidParam) {
                                break;
                            }
                            if (null == renderArgName) {
                                FieldInsnNode n = (FieldInsnNode) node;
                                renderArgName = n.name;
                            }
                            nodeList.insert(node.clone(C.<LabelNode, LabelNode>Map()));
                            break;
                        case AbstractInsnNode.LDC_INSN:
                            if (invalidParam) {
                                break;
                            }
                            LdcInsnNode ldc = (LdcInsnNode) node;
                            if (null != templateLiteral) {
                                logger.warn("Cannot have more than one template path parameter in the render call. Template path[%s] ignored", templateLiteral);
                            } else if (!(ldc.cst instanceof String)) {
                                logger.warn("Template path must be strictly String type. Found: %s", ldc.cst);
                            } else {
                                templateLiteral = ldc.cst.toString();
                            }
                    }
                    if (breakWhile) {
                        break;
                    }
                    node = node.getPrevious();
                }
                InsnList list = new InsnList();
                int appCtxIdx = ctxIndex();
                // setTemplatePath enhancement
                if (null != templateLiteral) {
                    if (appCtxIdx < 0) {
                        String appCtxFieldName = ctxFieldName();
                        if (null == appCtxFieldName) {
                            MethodInsnNode getAppCtx = new MethodInsnNode(INVOKESTATIC, AsmTypes.ACTION_CONTEXT_INTERNAL_NAME, ActionContext.METHOD_GET_CURRENT, GET_ACTION_CTX_DESC, false);
                            list.add(getAppCtx);
                        } else {
                            VarInsnNode loadThis = new VarInsnNode(ALOAD, 0);
                            FieldInsnNode getCtx = new FieldInsnNode(GETFIELD, segment.meta.classInfo().internalName(), appCtxFieldName, AsmTypes.ACTION_CONTEXT_DESC);
                            list.add(loadThis);
                            list.add(getCtx);
                        }
                    } else {
                        LabelNode lbl = new LabelNode();
                        VarInsnNode loadCtx = new VarInsnNode(ALOAD, appCtxIdx);
                        list.add(lbl);
                        list.add(loadCtx);
                    }
                    LdcInsnNode insnNode = new LdcInsnNode(templateLiteral);
                    list.add(insnNode);
                    MethodInsnNode invokeTemplatePath = new MethodInsnNode(INVOKEVIRTUAL, AsmTypes.ACTION_CONTEXT_INTERNAL_NAME, TEMPLATE_LITERAL_NM, TEMPLATE_LITERAL_DESC, false);
                    list.add(invokeTemplatePath);
                    InsnNode pop = new InsnNode(POP);
                    list.add(pop);
                }

                int len = loadInsnInfoList.size();
                if (0 == len) {
                    if (list.size() > 0) {
                        segment.instructions.insertBefore(node, list);
                    }
                    return;
                }

                // SetRenderArgs enhancement
                if (appCtxIdx < 0) {
                    String appCtxFieldName = ctxFieldName();
                    if (null == appCtxFieldName) {
                        MethodInsnNode getActionContext = new MethodInsnNode(INVOKESTATIC, AsmTypes.ACTION_CONTEXT_INTERNAL_NAME, ActionContext.METHOD_GET_CURRENT, GET_ACTION_CTX_DESC, false);
                        list.add(getActionContext);
                    } else {
                        VarInsnNode loadThis = new VarInsnNode(ALOAD, 0);
                        FieldInsnNode getCtx = new FieldInsnNode(GETFIELD, segment.meta.classInfo().internalName(), appCtxFieldName, AsmTypes.ACTION_CONTEXT_DESC);
                        list.add(loadThis);
                        list.add(getCtx);
                    }
                } else {
                    LabelNode lbl = new LabelNode();
                    VarInsnNode loadCtx = new VarInsnNode(ALOAD, appCtxIdx);
                    list.add(lbl);
                    list.add(loadCtx);
                }

                S.Buffer sb = S.newBuffer();
                for (int i = 0; i < len; ++i) {
                    LoadInsnInfo info = loadInsnInfoList.get(i);
                    info.appendTo(list, sb);
                }
                LdcInsnNode ldc = new LdcInsnNode(sb.toString());
                list.add(ldc);

                MethodInsnNode invokeRenderArg = new MethodInsnNode(INVOKEVIRTUAL, AsmTypes.ACTION_CONTEXT_INTERNAL_NAME, RENDER_ARG_NAMES_NM, RENDER_ARG_NAMES_DESC, false);
                list.add(invokeRenderArg);

                InsnNode pop = new InsnNode(POP);
                list.add(pop);


                segment.instructions.insertBefore(node, list);
            }

            @SuppressWarnings("FallThrough")
            private void injectThrowCode(AbstractInsnNode invokeNode) {
                if (segment.meta.hasReturn()) {
                    return;
                }
                AbstractInsnNode next = invokeNode.getNext();
                if (next.getOpcode() == POP) {
                    AbstractInsnNode newNext = new InsnNode(ATHROW);
                    InsnList instructions = segment.instructions;
                    instructions.insert(invokeNode, newNext);
                    instructions.remove(next);
                    next = newNext.getNext();
                    int curLine = -1;
                    while (null != next) {
                        boolean breakWhile = false;
                        int type = next.getType();
                        switch (type) {
                            case LABEL:
                                next = next.getNext();
                                break;
                            case AbstractInsnNode.LINE:
                                curLine = ((LineNumberNode) next).line;
                                next = next.getNext();
                                break;
                            case AbstractInsnNode.JUMP_INSN:
                                AbstractInsnNode tmp = next.getNext();
                                instructions.remove(next);
                                next = tmp;
                                break;
                            case INSN:
                                int op = next.getOpcode();
                                if (op == RETURN) {
                                    tmp = next.getNext();
                                    instructions.remove(next);
                                    next = tmp;
                                    tmp = next.getPrevious();
                                    if (tmp.getType() == AbstractInsnNode.LINE) {
                                        instructions.remove(tmp);
                                    }
                                    break;
                                }
                            case FRAME:
                                breakWhile = true;
                                break;
                            default:
                                AsmContext.line(curLine);
                                E.unexpected("Invalid statement after render result statement at line %s", curLine);
                        }
                        if (breakWhile) {
                            break;
                        }
                    }
                    refreshIteratorNext();
                }
            }
        }

        private static class LoadInsnInfo {
            private String name;
            private InsnList insnList;

            LoadInsnInfo(String name, InsnList insnList) {
                this.insnList = insnList;
                this.name = name;
            }

            void appendTo(InsnList list, S.Buffer paramNames) {
                LdcInsnNode ldc = new LdcInsnNode(name);
                list.add(ldc);
                list.add(insnList);
                MethodInsnNode invokeRenderArg = new MethodInsnNode(INVOKEVIRTUAL, AsmTypes.ACTION_CONTEXT_INTERNAL_NAME, RENDER_NM, RENDER_DESC, false);
                list.add(invokeRenderArg);
                if (paramNames.length() != 0) {
                    paramNames.append(',');
                }
                paramNames.append(name);
            }

            @Override
            public String toString() {
                return S.fmt("Load %s", name);
            }
        }
    }

    private static final String GET_ACTION_CTX_DESC = "()" + AsmTypes.ACTION_CONTEXT_DESC;
    private static final String RENDER_NM = "renderArg";
    private static final String RENDER_DESC = AsmTypes.methodDesc(ActionContext.class, String.class, Object.class);
    private static final String TEMPLATE_LITERAL_NM = "templateLiteral";
    private static final String TEMPLATE_LITERAL_DESC = AsmTypes.methodDesc(ActionContext.class, String.class);
    private static final String RENDER_ARG_NAMES_NM = "__appRenderArgNames";
    private static final String RENDER_ARG_NAMES_DESC = AsmTypes.methodDesc(ActionContext.class, String.class);

}
