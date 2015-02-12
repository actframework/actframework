package org.osgl.oms.be.controller;

import org.osgl.mvc.result.Result;
import org.osgl.oms.app.AppContext;
import org.osgl.oms.asm.Label;
import org.osgl.oms.asm.MethodVisitor;
import org.osgl.oms.asm.Opcodes;
import org.osgl.oms.asm.Type;
import org.osgl.oms.asm.tree.*;
import org.osgl.oms.asm.util.Printer;
import org.osgl.oms.be.Types;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class ActionMethodEnhancer extends MethodVisitor implements Opcodes {

    private static final String RESULT_CLASS = Result.class.getName();

    String name;
    ActionMethodMetaInfo meta;
    MethodVisitor next;

    public ActionMethodEnhancer(final MethodVisitor mv, ActionMethodMetaInfo meta, final int access, final String name, final String desc, final String signature, final String[] exceptions) {
        super(ASM5, new MethodNode(access, name, desc, signature, exceptions));
        this.meta = meta;
        this.name = name;
        this.next = mv;
    }

    @Override
    public void visitEnd() {
        MethodNode mn = (MethodNode)mv;
        transform(mn);
        mn.accept(next);
    }

    private void transform(MethodNode mn) {
        new Transformer(mn, meta).doIt();
    }

    private static class Transformer {
        MethodNode mn;
        InsnList instructions;
        private ActionMethodMetaInfo meta;
        List<Label> lblList = C.newSizedList(20);

        Transformer(MethodNode mn, ActionMethodMetaInfo meta) {
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
                    cur = new Segment(((LabelNode)insn).getLabel(), meta, instructions, itr, this);
                } else if (null != cur) {
                    cur.handle(insn);
                }
            }
        }

        private static abstract class InstructionHandler {
            Segment segment;
            InstructionHandler(Segment segment) {
                this.segment = segment;
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
            ActionMethodMetaInfo meta;
            ListIterator<AbstractInsnNode> itr;
            Transformer trans;
            private Map<Integer, InstructionHandler> handlers = C.map(
                    AbstractInsnNode.METHOD_INSN, new InvocationHandler(this)
            );

            Segment(Label start, ActionMethodMetaInfo meta, InsnList instructions, ListIterator<AbstractInsnNode> itr, Transformer trans) {
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
//                } else {
//                    try {
//                        System.out.printf("%s: %s\n", Printer.OPCODES[node.getOpcode()], node.getType());
//                    } catch (Exception e) {
//                        System.out.println(node.getClass() + ": " + node.getType() + " : " + node.getOpcode());
//                    }
                }
            }
        }

        private static class InvocationHandler extends InstructionHandler {

            InvocationHandler(Segment segment) {
                super(segment);
            }

            @Override
            protected void handle(AbstractInsnNode node) {
                MethodInsnNode n = (MethodInsnNode)node;
                Type type = Type.getMethodType(n.desc);
                Type retType = type.getReturnType();
                String method = n.name;
                String owner = Type.getType(n.owner).toString();
                if (RESULT_CLASS.equals(retType.getClassName())) {
                    injectRenderArgSetCode(n);
                    injectThrowCode(n);
                }
                String invokeType = Printer.OPCODES[n.getOpcode()];
            }

            private int appCtxIndex() {
                return segment.meta.appContextIndex();
            }

            private void injectRenderArgSetCode(AbstractInsnNode invokeNode) {
                if (!segment.meta.hasLocalVariableTable()) {
                    System.out.println("Warning: local variable table info not found. AppContext render args will not be automatically populated");
                    return;
                }
                AbstractInsnNode node = invokeNode.getPrevious();
                List<LoadInsnInfo> loadInsnInfoList = C.newList();
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
                            VarInsnNode n = (VarInsnNode)node;
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
                int appCtxIdx = appCtxIndex();
                if (appCtxIdx < 0) {
                    MethodInsnNode getAppCtx = new MethodInsnNode(INVOKESTATIC, Types.APP_CONTEXT_INTERNAL_NAME, "get", GET_APP_CTX_DESC, false);
                    list.add(getAppCtx);
                } else {
                    LabelNode lbl = new LabelNode();
                    VarInsnNode loadCtx = new VarInsnNode(ALOAD, appCtxIdx);
                    list.add(lbl);
                    list.add(loadCtx);
                }
                for (int i = 0; i < len; ++i) {
                    LoadInsnInfo info = loadInsnInfoList.get(i);
                    info.appendTo(list, segment);
                }
                InsnNode pop = new InsnNode(POP);
                list.add(pop);
                segment.instructions.insertBefore(node, list);
            }

            private void injectThrowCode(AbstractInsnNode invokeNode) {
                if (segment.meta.hasReturnType) {
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
                                curLine = ((LineNumberNode)next).line;
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
            }, L(LLOAD), F(FLOAD), D(DLOAD), A(ALOAD), Store (-1) {
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

            void appendTo(InsnList list, Segment segment) {
                LocalVariableMetaInfo var = var(segment);
                if (null == var) return;
                //LabelNode lbl = new LabelNode();
                //VarInsnNode loadCtx = new VarInsnNode(ALOAD, appCtxIndex);
                //list.add(lbl);
                //list.add(loadCtx);
                LdcInsnNode ldc = new LdcInsnNode(var.name());
                if (var.name().equals("email")) {
                    System.out.println("");
                }
                list.add(ldc);
                insn.appendTo(list, index, var.type());
                MethodInsnNode invokeRenderArg = new MethodInsnNode(INVOKEVIRTUAL, Types.APP_CONTEXT_INTERNAL_NAME, RENDER_NM, RENDER_DESC, false);
                list.add(invokeRenderArg);
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

    private static final String GET_APP_CTX_DESC = "()" + Types.APP_CONTEXT_DESC;
    private static final String RENDER_NM = "renderArg";
    private static final String RENDER_DESC1 = "(Ljava/lang/String;Ljava/lang/Object;)Lorg/osgl/mvc/server/AppContext;";
    private static final String RENDER_DESC = Types.methodDesc(AppContext.class, String.class, Object.class);

}
