package act.controller.bytecode;

import act.app.ActionContext;
import act.asm.Label;
import act.asm.MethodVisitor;
import act.asm.Opcodes;
import act.asm.Type;
import act.asm.tree.*;
import act.controller.meta.HandlerMethodMetaInfo;
import act.controller.meta.LocalVariableMetaInfo;
import act.controller.meta.HandlerParamMetaInfo;
import act.util.AsmTypes;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.mvc.result.Result;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class HandlerEnhancer extends MethodVisitor implements Opcodes {

    private static final Logger logger = LogManager.get(HandlerEnhancer.class);

    private static final String RESULT_CLASS = Result.class.getName();

    private HandlerMethodMetaInfo info;
    private MethodVisitor next;
    private int paramIdShift = 0;

    public HandlerEnhancer(final MethodVisitor mv, HandlerMethodMetaInfo meta, final int access, final String name, final String desc, final String signature, final String[] exceptions) {
        super(ASM5, new MethodNode(access, name, desc, signature, exceptions));
        this.info = meta;
        this.next = mv;
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
                if (AsmTypes.ACTION_CONTEXT_TYPE.equals(param.type())) {
                    info.appCtxLocalVariableTableIndex(index);
                }
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
    public void visitEnd() {
        MethodNode mn = (MethodNode) mv;
        transform(mn);
        mn.accept(next);
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
            this.instructions = mn.instructions;
        }

        void doIt() {
            ListIterator<AbstractInsnNode> itr = instructions.iterator();
            Segment cur = null;
            while (itr.hasNext()) {
                AbstractInsnNode insn = itr.next();
                if (insn.getType() == AbstractInsnNode.LABEL) {
                    cur = new Segment(((LabelNode) insn).getLabel(), meta, instructions, itr, this);
                } else if (null != cur) {
                    cur.handle(insn);
                }
            }
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
            private Map<Integer, InstructionHandler> handlers = C.map(
                    AbstractInsnNode.METHOD_INSN, new InvocationHandler(this, meta)
            );

            Segment(Label start, HandlerMethodMetaInfo meta, InsnList instructions, ListIterator<AbstractInsnNode> itr, Transformer trans) {
                this.startLabel = start;
                this.meta = meta;
                this.instructions = instructions;
                this.itr = itr;
                this.trans = trans;
                trans.lblList.add(start);
            }

            protected void handle(AbstractInsnNode node) {
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
                //String method = n.name;
                //String owner = Type.getType(n.owner).toString();
                if (RESULT_CLASS.equals(retType.getClassName())) {
                    injectRenderArgSetCode(n);
                    injectThrowCode(n);
                }
            }

            private String ctxFieldName() {
                return segment.meta.classInfo().ctxField();
            }

            private int ctxIndex() {
                return segment.meta.appCtxLocalVariableTableIndex();
            }

            private void injectRenderArgSetCode(AbstractInsnNode invokeNode) {
                if (!segment.meta.hasLocalVariableTable()) {
                    logger.warn("local variable table info not found. ActionContext render args will not be automatically populated");
                    return;
                }
                AbstractInsnNode node = invokeNode.getPrevious();
                List<LoadInsnInfo> loadInsnInfoList = C.newList();
                String templatePath = null;
                while (null != node) {
                    int type = node.getType();
                    boolean breakWhile = false;
                    switch (type) {
                        case AbstractInsnNode.LABEL:
                        case AbstractInsnNode.FRAME:
                            node = node.getNext();
                            breakWhile = true;
                            break;
                        case AbstractInsnNode.VAR_INSN:
                            VarInsnNode n = (VarInsnNode) node;
                            if (0 == n.var && !segment.meta.isStatic()) {
                                // skip "this"
                                break;
                            }
                            LoadInsn insn = LoadInsn.of(n.getOpcode());
                            if (insn.isStoreInsn()) {
                                break;
                            }
                            LoadInsnInfo info = new LoadInsnInfo(insn, n.var);
                            loadInsnInfoList.add(info);
                            break;
                        case AbstractInsnNode.LDC_INSN:
                            LdcInsnNode ldc = (LdcInsnNode) node;
                            if (null != templatePath) {
                                logger.warn("Cannot have more than one template path parameter in the render call. Template path[%s] ignored", templatePath);
                            } else if (!(ldc.cst instanceof String)) {
                                logger.warn("Template path must be strictly String type. Found: %s", ldc.cst);
                            } else {
                                templatePath = ldc.cst.toString();
                            }
                        default:
                            //System.out.printf("type\n");
                    }
                    if (breakWhile) {
                        break;
                    }
                    node = node.getPrevious();
                }
                InsnList list = new InsnList();
                int len = loadInsnInfoList.size();
                if (len == 0) {
                    return;
                }
                int appCtxIdx = ctxIndex();

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
                StringBuilder sb = S.builder();
                for (int i = 0; i < len; ++i) {
                    LoadInsnInfo info = loadInsnInfoList.get(i);
                    info.appendTo(list, segment, sb);
                }
                LdcInsnNode ldc = new LdcInsnNode(sb.toString());
                list.add(ldc);
                MethodInsnNode invokeRenderArg = new MethodInsnNode(INVOKEVIRTUAL, AsmTypes.ACTION_CONTEXT_INTERNAL_NAME, RENDER_ARG_NAMES_NM, RENDER_ARG_NAMES_DESC, false);
                list.add(invokeRenderArg);

                InsnNode pop = new InsnNode(POP);
                list.add(pop);

                // setTemplatePath enhancement
                if (null != templatePath) {
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
                    LdcInsnNode insnNode = new LdcInsnNode(templatePath);
                    list.add(insnNode);
                    MethodInsnNode invokeTemplatePath = new MethodInsnNode(INVOKEVIRTUAL, AsmTypes.ACTION_CONTEXT_INTERNAL_NAME, TEMPLATE_PATH_NM, TEMPLATE_PATH_DESC, false);
                    list.add(invokeTemplatePath);
                    pop = new InsnNode(POP);
                    list.add(pop);
                }

                segment.instructions.insertBefore(node, list);
            }

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
                            case AbstractInsnNode.LABEL:
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
                            case AbstractInsnNode.INSN:
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
                            case AbstractInsnNode.FRAME:
                                breakWhile = true;
                                break;
                            default:
                                E.unexpected("Invalid statement after render result statement at line %s", curLine);
                        }
                        if (breakWhile) {
                            break;
                        }
                    }
                    refreshIteratorNext();
                    //System.out.printf("ATHROW inserted\n");
                }
            }
        }

        private static final int _I = 'I';
        private static final int _Z = 'Z';
        private static final int _S = 'S';
        private static final int _B = 'B';
        private static final int _C = 'C';

        private static enum LoadInsn {
            I(ILOAD) {
                void appendTo(InsnList list, int varIndex, String type) {
                    super.appendTo(list, varIndex, type);
                    String owner, desc;
                    switch (type.hashCode()) {
                        case _I:
                            owner = "java/lang/Integer";
                            desc = "(I)Ljava/lang/Integer;";
                            break;
                        case _Z:
                            owner = "java/lang/Boolean";
                            desc = "(Z)Ljava/lang/Boolean;";
                            break;
                        case _S:
                            owner = "java/lang/Short";
                            desc = "(S)Ljava/lang/Short";
                            break;
                        case _B:
                            owner = "java/lang/Byte";
                            desc = "(B)Ljava/lang/Byte;";
                            break;
                        case _C:
                            owner = "java/lang/Character";
                            desc = "(C)Ljava/lang/Character;";
                            break;
                        default:
                            throw E.unexpected("int var type not recognized: %s", type);
                    }
                    MethodInsnNode method = new MethodInsnNode(INVOKESTATIC, owner, "valueOf", desc, false);
                    list.add(method);
                }
            }, L(LLOAD) {
                @Override
                void appendTo(InsnList list, int varIndex, String type) {
                    super.appendTo(list, varIndex, type);
                    MethodInsnNode method = new MethodInsnNode(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                    list.add(method);
                }
            }, F(FLOAD) {
                @Override
                void appendTo(InsnList list, int varIndex, String type) {
                    super.appendTo(list, varIndex, type);
                    MethodInsnNode method = new MethodInsnNode(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                    list.add(method);
                }
            }, D(DLOAD) {
                @Override
                void appendTo(InsnList list, int varIndex, String type) {
                    super.appendTo(list, varIndex, type);
                    MethodInsnNode method = new MethodInsnNode(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                    list.add(method);
                }
            }, A(ALOAD), Store(-1) {
                @Override
                void appendTo(InsnList list, int varIndex, String type) {
                    throw E.unsupport();
                }
            };
            private int opcode;

            LoadInsn(int opcode) {
                this.opcode = opcode;
            }

            static LoadInsn of(int opcode) {
                switch (opcode) {
                    case ILOAD:
                        return I;
                    case LLOAD:
                        return L;
                    case FLOAD:
                        return F;
                    case DLOAD:
                        return D;
                    case ALOAD:
                        return A;
                    default:
                        return Store;
                }
            }

            boolean isStoreInsn() {
                return this == Store;
            }

            void appendTo(InsnList list, int varIndex, String type) {
                VarInsnNode load = new VarInsnNode(opcode, varIndex);
                list.add(load);
            }
        }

        private static class LoadInsnInfo {
            LoadInsn insn;
            int index;

            LoadInsnInfo(LoadInsn insn, int index) {
                this.insn = insn;
                this.index = index;
            }

            void appendTo(InsnList list, Segment segment, StringBuilder paramNames) {
                LocalVariableMetaInfo var = var(segment);
                if (null == var) return;
                LdcInsnNode ldc = new LdcInsnNode(var.name());
                list.add(ldc);
                insn.appendTo(list, index, var.type());
                MethodInsnNode invokeRenderArg = new MethodInsnNode(INVOKEVIRTUAL, AsmTypes.ACTION_CONTEXT_INTERNAL_NAME, RENDER_NM, RENDER_DESC, false);
                list.add(invokeRenderArg);
                if (paramNames.length() != 0) {
                    paramNames.append(',');
                }
                paramNames.append(var.name());
            }

            LocalVariableMetaInfo var(Segment segment) {
                Label lbl = segment.startLabel;
                int pos = -1;
                List<Label> lblList = segment.trans.lblList;
                while (null != lbl) {
                    LocalVariableMetaInfo var = segment.meta.localVariable(index, lbl);
                    if (null != var) return var;
                    if (-1 == pos) {
                        pos = lblList.indexOf(lbl);
                        if (pos <= 0) {
                            return null;
                        }
                    }
                    lbl = lblList.get(--pos);
                }
                return null;
            }

            @Override
            public String toString() {
                return S.fmt("%sLoad %s", insn, index);
            }
        }
    }

    private static final String GET_ACTION_CTX_DESC = "()" + AsmTypes.ACTION_CONTEXT_DESC;
    private static final String RENDER_NM = "renderArg";
    private static final String RENDER_DESC = AsmTypes.methodDesc(ActionContext.class, String.class, Object.class);
    private static final String TEMPLATE_PATH_NM = "templatePath";
    private static final String TEMPLATE_PATH_DESC = AsmTypes.methodDesc(ActionContext.class, String.class);
    private static final String RENDER_ARG_NAMES_NM = "__appRenderArgNames";
    private static final String RENDER_ARG_NAMES_DESC = AsmTypes.methodDesc(ActionContext.class, String.class);

}
