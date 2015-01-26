/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.osgl.mvc.server.asm.commons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link org.osgl.mvc.server.asm.MethodVisitor} that keeps track of stack map frame changes between
 * {@link #visitFrame(int, int, Object[], int, Object[]) visitFrame} calls. This
 * adapter must be used with the
 * {@link org.osgl.mvc.server.asm.ClassReader#EXPAND_FRAMES} option. Each
 * visit<i>X</i> instruction delegates to the next visitor in the chain, if any,
 * and then simulates the effect of this instruction on the stack map frame,
 * represented by {@link #locals} and {@link #stack}. The next visitor in the
 * chain can get the state of the stack map frame <i>before</i> each instruction
 * by reading the value of these fields in its visit<i>X</i> methods (this
 * requires a reference to the AnalyzerAdapter that is before it in the chain).
 * If this adapter is used with a class that does not contain stack map table
 * attributes (i.e., pre Java 6 classes) then this adapter may not be able to
 * compute the stack map frame for each instruction. In this case no exception
 * is thrown but the {@link #locals} and {@link #stack} fields will be null for
 * these instructions.
 *
 * @author Eric Bruneton
 */
public class AnalyzerAdapter extends org.osgl.mvc.server.asm.MethodVisitor {

    /**
     * <code>List</code> of the local variable slots for current execution
     * frame. Primitive types are represented by {@link org.osgl.mvc.server.asm.Opcodes#TOP},
     * {@link org.osgl.mvc.server.asm.Opcodes#INTEGER}, {@link org.osgl.mvc.server.asm.Opcodes#FLOAT}, {@link org.osgl.mvc.server.asm.Opcodes#LONG},
     * {@link org.osgl.mvc.server.asm.Opcodes#DOUBLE},{@link org.osgl.mvc.server.asm.Opcodes#NULL} or
     * {@link org.osgl.mvc.server.asm.Opcodes#UNINITIALIZED_THIS} (long and double are represented by
     * two elements, the second one being TOP). Reference types are represented
     * by String objects (representing internal names), and uninitialized types
     * by Label objects (this label designates the NEW instruction that created
     * this uninitialized value). This field is <tt>null</tt> for unreachable
     * instructions.
     */
    public List<Object> locals;

    /**
     * <code>List</code> of the operand stack slots for current execution frame.
     * Primitive types are represented by {@link org.osgl.mvc.server.asm.Opcodes#TOP},
     * {@link org.osgl.mvc.server.asm.Opcodes#INTEGER}, {@link org.osgl.mvc.server.asm.Opcodes#FLOAT}, {@link org.osgl.mvc.server.asm.Opcodes#LONG},
     * {@link org.osgl.mvc.server.asm.Opcodes#DOUBLE},{@link org.osgl.mvc.server.asm.Opcodes#NULL} or
     * {@link org.osgl.mvc.server.asm.Opcodes#UNINITIALIZED_THIS} (long and double are represented by
     * two elements, the second one being TOP). Reference types are represented
     * by String objects (representing internal names), and uninitialized types
     * by Label objects (this label designates the NEW instruction that created
     * this uninitialized value). This field is <tt>null</tt> for unreachable
     * instructions.
     */
    public List<Object> stack;

    /**
     * The labels that designate the next instruction to be visited. May be
     * <tt>null</tt>.
     */
    private List<org.osgl.mvc.server.asm.Label> labels;

    /**
     * Information about uninitialized types in the current execution frame.
     * This map associates internal names to Label objects. Each label
     * designates a NEW instruction that created the currently uninitialized
     * types, and the associated internal name represents the NEW operand, i.e.
     * the final, initialized type value.
     */
    public Map<Object, Object> uninitializedTypes;

    /**
     * The maximum stack size of this method.
     */
    private int maxStack;

    /**
     * The maximum number of local variables of this method.
     */
    private int maxLocals;

    /**
     * The owner's class name.
     */
    private String owner;

    /**
     * Creates a new {@link AnalyzerAdapter}. <i>Subclasses must not use this
     * constructor</i>. Instead, they must use the
     * {@link #AnalyzerAdapter(int, String, int, String, String, org.osgl.mvc.server.asm.MethodVisitor)}
     * version.
     *
     * @param owner
     *            the owner's class name.
     * @param access
     *            the method's access flags (see {@link org.osgl.mvc.server.asm.Opcodes}).
     * @param name
     *            the method's name.
     * @param desc
     *            the method's descriptor (see {@link org.osgl.mvc.server.asm.Type Type}).
     * @param mv
     *            the method visitor to which this adapter delegates calls. May
     *            be <tt>null</tt>.
     * @throws IllegalStateException
     *             If a subclass calls this constructor.
     */
    public AnalyzerAdapter(final String owner, final int access,
            final String name, final String desc, final org.osgl.mvc.server.asm.MethodVisitor mv) {
        this(org.osgl.mvc.server.asm.Opcodes.ASM5, owner, access, name, desc, mv);
        if (getClass() != AnalyzerAdapter.class) {
            throw new IllegalStateException();
        }
    }

    /**
     * Creates a new {@link AnalyzerAdapter}.
     *
     * @param api
     *            the ASM API version implemented by this visitor. Must be one
     *            of {@link org.osgl.mvc.server.asm.Opcodes#ASM4} or {@link org.osgl.mvc.server.asm.Opcodes#ASM5}.
     * @param owner
     *            the owner's class name.
     * @param access
     *            the method's access flags (see {@link org.osgl.mvc.server.asm.Opcodes}).
     * @param name
     *            the method's name.
     * @param desc
     *            the method's descriptor (see {@link org.osgl.mvc.server.asm.Type Type}).
     * @param mv
     *            the method visitor to which this adapter delegates calls. May
     *            be <tt>null</tt>.
     */
    protected AnalyzerAdapter(final int api, final String owner,
            final int access, final String name, final String desc,
            final org.osgl.mvc.server.asm.MethodVisitor mv) {
        super(api, mv);
        this.owner = owner;
        locals = new ArrayList<Object>();
        stack = new ArrayList<Object>();
        uninitializedTypes = new HashMap<Object, Object>();

        if ((access & org.osgl.mvc.server.asm.Opcodes.ACC_STATIC) == 0) {
            if ("<init>".equals(name)) {
                locals.add(org.osgl.mvc.server.asm.Opcodes.UNINITIALIZED_THIS);
            } else {
                locals.add(owner);
            }
        }
        org.osgl.mvc.server.asm.Type[] types = org.osgl.mvc.server.asm.Type.getArgumentTypes(desc);
        for (int i = 0; i < types.length; ++i) {
            org.osgl.mvc.server.asm.Type type = types[i];
            switch (type.getSort()) {
            case org.osgl.mvc.server.asm.Type.BOOLEAN:
            case org.osgl.mvc.server.asm.Type.CHAR:
            case org.osgl.mvc.server.asm.Type.BYTE:
            case org.osgl.mvc.server.asm.Type.SHORT:
            case org.osgl.mvc.server.asm.Type.INT:
                locals.add(org.osgl.mvc.server.asm.Opcodes.INTEGER);
                break;
            case org.osgl.mvc.server.asm.Type.FLOAT:
                locals.add(org.osgl.mvc.server.asm.Opcodes.FLOAT);
                break;
            case org.osgl.mvc.server.asm.Type.LONG:
                locals.add(org.osgl.mvc.server.asm.Opcodes.LONG);
                locals.add(org.osgl.mvc.server.asm.Opcodes.TOP);
                break;
            case org.osgl.mvc.server.asm.Type.DOUBLE:
                locals.add(org.osgl.mvc.server.asm.Opcodes.DOUBLE);
                locals.add(org.osgl.mvc.server.asm.Opcodes.TOP);
                break;
            case org.osgl.mvc.server.asm.Type.ARRAY:
                locals.add(types[i].getDescriptor());
                break;
            // case Type.OBJECT:
            default:
                locals.add(types[i].getInternalName());
            }
        }
        maxLocals = locals.size();
    }

    @Override
    public void visitFrame(final int type, final int nLocal,
            final Object[] local, final int nStack, final Object[] stack) {
        if (type != org.osgl.mvc.server.asm.Opcodes.F_NEW) { // uncompressed frame
            throw new IllegalStateException(
                    "ClassReader.accept() should be called with EXPAND_FRAMES flag");
        }

        if (mv != null) {
            mv.visitFrame(type, nLocal, local, nStack, stack);
        }

        if (this.locals != null) {
            this.locals.clear();
            this.stack.clear();
        } else {
            this.locals = new ArrayList<Object>();
            this.stack = new ArrayList<Object>();
        }
        visitFrameTypes(nLocal, local, this.locals);
        visitFrameTypes(nStack, stack, this.stack);
        maxStack = Math.max(maxStack, this.stack.size());
    }

    private static void visitFrameTypes(final int n, final Object[] types,
            final List<Object> result) {
        for (int i = 0; i < n; ++i) {
            Object type = types[i];
            result.add(type);
            if (type == org.osgl.mvc.server.asm.Opcodes.LONG || type == org.osgl.mvc.server.asm.Opcodes.DOUBLE) {
                result.add(org.osgl.mvc.server.asm.Opcodes.TOP);
            }
        }
    }

    @Override
    public void visitInsn(final int opcode) {
        if (mv != null) {
            mv.visitInsn(opcode);
        }
        execute(opcode, 0, null);
        if ((opcode >= org.osgl.mvc.server.asm.Opcodes.IRETURN && opcode <= org.osgl.mvc.server.asm.Opcodes.RETURN)
                || opcode == org.osgl.mvc.server.asm.Opcodes.ATHROW) {
            this.locals = null;
            this.stack = null;
        }
    }

    @Override
    public void visitIntInsn(final int opcode, final int operand) {
        if (mv != null) {
            mv.visitIntInsn(opcode, operand);
        }
        execute(opcode, operand, null);
    }

    @Override
    public void visitVarInsn(final int opcode, final int var) {
        if (mv != null) {
            mv.visitVarInsn(opcode, var);
        }
        execute(opcode, var, null);
    }

    @Override
    public void visitTypeInsn(final int opcode, final String type) {
        if (opcode == org.osgl.mvc.server.asm.Opcodes.NEW) {
            if (labels == null) {
                org.osgl.mvc.server.asm.Label l = new org.osgl.mvc.server.asm.Label();
                labels = new ArrayList<org.osgl.mvc.server.asm.Label>(3);
                labels.add(l);
                if (mv != null) {
                    mv.visitLabel(l);
                }
            }
            for (int i = 0; i < labels.size(); ++i) {
                uninitializedTypes.put(labels.get(i), type);
            }
        }
        if (mv != null) {
            mv.visitTypeInsn(opcode, type);
        }
        execute(opcode, 0, type);
    }

    @Override
    public void visitFieldInsn(final int opcode, final String owner,
            final String name, final String desc) {
        if (mv != null) {
            mv.visitFieldInsn(opcode, owner, name, desc);
        }
        execute(opcode, 0, desc);
    }

    @Deprecated
    @Override
    public void visitMethodInsn(final int opcode, final String owner,
            final String name, final String desc) {
        if (api >= org.osgl.mvc.server.asm.Opcodes.ASM5) {
            super.visitMethodInsn(opcode, owner, name, desc);
            return;
        }
        doVisitMethodInsn(opcode, owner, name, desc,
                opcode == org.osgl.mvc.server.asm.Opcodes.INVOKEINTERFACE);
    }

    @Override
    public void visitMethodInsn(final int opcode, final String owner,
            final String name, final String desc, final boolean itf) {
        if (api < org.osgl.mvc.server.asm.Opcodes.ASM5) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            return;
        }
        doVisitMethodInsn(opcode, owner, name, desc, itf);
    }

    private void doVisitMethodInsn(int opcode, final String owner,
            final String name, final String desc, final boolean itf) {
        if (mv != null) {
            mv.visitMethodInsn(opcode, owner, name, desc, itf);
        }
        if (this.locals == null) {
            labels = null;
            return;
        }
        pop(desc);
        if (opcode != org.osgl.mvc.server.asm.Opcodes.INVOKESTATIC) {
            Object t = pop();
            if (opcode == org.osgl.mvc.server.asm.Opcodes.INVOKESPECIAL && name.charAt(0) == '<') {
                Object u;
                if (t == org.osgl.mvc.server.asm.Opcodes.UNINITIALIZED_THIS) {
                    u = this.owner;
                } else {
                    u = uninitializedTypes.get(t);
                }
                for (int i = 0; i < locals.size(); ++i) {
                    if (locals.get(i) == t) {
                        locals.set(i, u);
                    }
                }
                for (int i = 0; i < stack.size(); ++i) {
                    if (stack.get(i) == t) {
                        stack.set(i, u);
                    }
                }
            }
        }
        pushDesc(desc);
        labels = null;
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, org.osgl.mvc.server.asm.Handle bsm,
            Object... bsmArgs) {
        if (mv != null) {
            mv.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        }
        if (this.locals == null) {
            labels = null;
            return;
        }
        pop(desc);
        pushDesc(desc);
        labels = null;
    }

    @Override
    public void visitJumpInsn(final int opcode, final org.osgl.mvc.server.asm.Label label) {
        if (mv != null) {
            mv.visitJumpInsn(opcode, label);
        }
        execute(opcode, 0, null);
        if (opcode == org.osgl.mvc.server.asm.Opcodes.GOTO) {
            this.locals = null;
            this.stack = null;
        }
    }

    @Override
    public void visitLabel(final org.osgl.mvc.server.asm.Label label) {
        if (mv != null) {
            mv.visitLabel(label);
        }
        if (labels == null) {
            labels = new ArrayList<org.osgl.mvc.server.asm.Label>(3);
        }
        labels.add(label);
    }

    @Override
    public void visitLdcInsn(final Object cst) {
        if (mv != null) {
            mv.visitLdcInsn(cst);
        }
        if (this.locals == null) {
            labels = null;
            return;
        }
        if (cst instanceof Integer) {
            push(org.osgl.mvc.server.asm.Opcodes.INTEGER);
        } else if (cst instanceof Long) {
            push(org.osgl.mvc.server.asm.Opcodes.LONG);
            push(org.osgl.mvc.server.asm.Opcodes.TOP);
        } else if (cst instanceof Float) {
            push(org.osgl.mvc.server.asm.Opcodes.FLOAT);
        } else if (cst instanceof Double) {
            push(org.osgl.mvc.server.asm.Opcodes.DOUBLE);
            push(org.osgl.mvc.server.asm.Opcodes.TOP);
        } else if (cst instanceof String) {
            push("java/lang/String");
        } else if (cst instanceof org.osgl.mvc.server.asm.Type) {
            int sort = ((org.osgl.mvc.server.asm.Type) cst).getSort();
            if (sort == org.osgl.mvc.server.asm.Type.OBJECT || sort == org.osgl.mvc.server.asm.Type.ARRAY) {
                push("java/lang/Class");
            } else if (sort == org.osgl.mvc.server.asm.Type.METHOD) {
                push("java/lang/invoke/MethodType");
            } else {
                throw new IllegalArgumentException();
            }
        } else if (cst instanceof org.osgl.mvc.server.asm.Handle) {
            push("java/lang/invoke/MethodHandle");
        } else {
            throw new IllegalArgumentException();
        }
        labels = null;
    }

    @Override
    public void visitIincInsn(final int var, final int increment) {
        if (mv != null) {
            mv.visitIincInsn(var, increment);
        }
        execute(org.osgl.mvc.server.asm.Opcodes.IINC, var, null);
    }

    @Override
    public void visitTableSwitchInsn(final int min, final int max,
            final org.osgl.mvc.server.asm.Label dflt, final org.osgl.mvc.server.asm.Label... labels) {
        if (mv != null) {
            mv.visitTableSwitchInsn(min, max, dflt, labels);
        }
        execute(org.osgl.mvc.server.asm.Opcodes.TABLESWITCH, 0, null);
        this.locals = null;
        this.stack = null;
    }

    @Override
    public void visitLookupSwitchInsn(final org.osgl.mvc.server.asm.Label dflt, final int[] keys,
            final org.osgl.mvc.server.asm.Label[] labels) {
        if (mv != null) {
            mv.visitLookupSwitchInsn(dflt, keys, labels);
        }
        execute(org.osgl.mvc.server.asm.Opcodes.LOOKUPSWITCH, 0, null);
        this.locals = null;
        this.stack = null;
    }

    @Override
    public void visitMultiANewArrayInsn(final String desc, final int dims) {
        if (mv != null) {
            mv.visitMultiANewArrayInsn(desc, dims);
        }
        execute(org.osgl.mvc.server.asm.Opcodes.MULTIANEWARRAY, dims, desc);
    }

    @Override
    public void visitMaxs(final int maxStack, final int maxLocals) {
        if (mv != null) {
            this.maxStack = Math.max(this.maxStack, maxStack);
            this.maxLocals = Math.max(this.maxLocals, maxLocals);
            mv.visitMaxs(this.maxStack, this.maxLocals);
        }
    }

    // ------------------------------------------------------------------------

    private Object get(final int local) {
        maxLocals = Math.max(maxLocals, local + 1);
        return local < locals.size() ? locals.get(local) : org.osgl.mvc.server.asm.Opcodes.TOP;
    }

    private void set(final int local, final Object type) {
        maxLocals = Math.max(maxLocals, local + 1);
        while (local >= locals.size()) {
            locals.add(org.osgl.mvc.server.asm.Opcodes.TOP);
        }
        locals.set(local, type);
    }

    private void push(final Object type) {
        stack.add(type);
        maxStack = Math.max(maxStack, stack.size());
    }

    private void pushDesc(final String desc) {
        int index = desc.charAt(0) == '(' ? desc.indexOf(')') + 1 : 0;
        switch (desc.charAt(index)) {
        case 'V':
            return;
        case 'Z':
        case 'C':
        case 'B':
        case 'S':
        case 'I':
            push(org.osgl.mvc.server.asm.Opcodes.INTEGER);
            return;
        case 'F':
            push(org.osgl.mvc.server.asm.Opcodes.FLOAT);
            return;
        case 'J':
            push(org.osgl.mvc.server.asm.Opcodes.LONG);
            push(org.osgl.mvc.server.asm.Opcodes.TOP);
            return;
        case 'D':
            push(org.osgl.mvc.server.asm.Opcodes.DOUBLE);
            push(org.osgl.mvc.server.asm.Opcodes.TOP);
            return;
        case '[':
            if (index == 0) {
                push(desc);
            } else {
                push(desc.substring(index, desc.length()));
            }
            break;
        // case 'L':
        default:
            if (index == 0) {
                push(desc.substring(1, desc.length() - 1));
            } else {
                push(desc.substring(index + 1, desc.length() - 1));
            }
        }
    }

    private Object pop() {
        return stack.remove(stack.size() - 1);
    }

    private void pop(final int n) {
        int size = stack.size();
        int end = size - n;
        for (int i = size - 1; i >= end; --i) {
            stack.remove(i);
        }
    }

    private void pop(final String desc) {
        char c = desc.charAt(0);
        if (c == '(') {
            int n = 0;
            org.osgl.mvc.server.asm.Type[] types = org.osgl.mvc.server.asm.Type.getArgumentTypes(desc);
            for (int i = 0; i < types.length; ++i) {
                n += types[i].getSize();
            }
            pop(n);
        } else if (c == 'J' || c == 'D') {
            pop(2);
        } else {
            pop(1);
        }
    }

    private void execute(final int opcode, final int iarg, final String sarg) {
        if (this.locals == null) {
            labels = null;
            return;
        }
        Object t1, t2, t3, t4;
        switch (opcode) {
        case org.osgl.mvc.server.asm.Opcodes.NOP:
        case org.osgl.mvc.server.asm.Opcodes.INEG:
        case org.osgl.mvc.server.asm.Opcodes.LNEG:
        case org.osgl.mvc.server.asm.Opcodes.FNEG:
        case org.osgl.mvc.server.asm.Opcodes.DNEG:
        case org.osgl.mvc.server.asm.Opcodes.I2B:
        case org.osgl.mvc.server.asm.Opcodes.I2C:
        case org.osgl.mvc.server.asm.Opcodes.I2S:
        case org.osgl.mvc.server.asm.Opcodes.GOTO:
        case org.osgl.mvc.server.asm.Opcodes.RETURN:
            break;
        case org.osgl.mvc.server.asm.Opcodes.ACONST_NULL:
            push(org.osgl.mvc.server.asm.Opcodes.NULL);
            break;
        case org.osgl.mvc.server.asm.Opcodes.ICONST_M1:
        case org.osgl.mvc.server.asm.Opcodes.ICONST_0:
        case org.osgl.mvc.server.asm.Opcodes.ICONST_1:
        case org.osgl.mvc.server.asm.Opcodes.ICONST_2:
        case org.osgl.mvc.server.asm.Opcodes.ICONST_3:
        case org.osgl.mvc.server.asm.Opcodes.ICONST_4:
        case org.osgl.mvc.server.asm.Opcodes.ICONST_5:
        case org.osgl.mvc.server.asm.Opcodes.BIPUSH:
        case org.osgl.mvc.server.asm.Opcodes.SIPUSH:
            push(org.osgl.mvc.server.asm.Opcodes.INTEGER);
            break;
        case org.osgl.mvc.server.asm.Opcodes.LCONST_0:
        case org.osgl.mvc.server.asm.Opcodes.LCONST_1:
            push(org.osgl.mvc.server.asm.Opcodes.LONG);
            push(org.osgl.mvc.server.asm.Opcodes.TOP);
            break;
        case org.osgl.mvc.server.asm.Opcodes.FCONST_0:
        case org.osgl.mvc.server.asm.Opcodes.FCONST_1:
        case org.osgl.mvc.server.asm.Opcodes.FCONST_2:
            push(org.osgl.mvc.server.asm.Opcodes.FLOAT);
            break;
        case org.osgl.mvc.server.asm.Opcodes.DCONST_0:
        case org.osgl.mvc.server.asm.Opcodes.DCONST_1:
            push(org.osgl.mvc.server.asm.Opcodes.DOUBLE);
            push(org.osgl.mvc.server.asm.Opcodes.TOP);
            break;
        case org.osgl.mvc.server.asm.Opcodes.ILOAD:
        case org.osgl.mvc.server.asm.Opcodes.FLOAD:
        case org.osgl.mvc.server.asm.Opcodes.ALOAD:
            push(get(iarg));
            break;
        case org.osgl.mvc.server.asm.Opcodes.LLOAD:
        case org.osgl.mvc.server.asm.Opcodes.DLOAD:
            push(get(iarg));
            push(org.osgl.mvc.server.asm.Opcodes.TOP);
            break;
        case org.osgl.mvc.server.asm.Opcodes.IALOAD:
        case org.osgl.mvc.server.asm.Opcodes.BALOAD:
        case org.osgl.mvc.server.asm.Opcodes.CALOAD:
        case org.osgl.mvc.server.asm.Opcodes.SALOAD:
            pop(2);
            push(org.osgl.mvc.server.asm.Opcodes.INTEGER);
            break;
        case org.osgl.mvc.server.asm.Opcodes.LALOAD:
        case org.osgl.mvc.server.asm.Opcodes.D2L:
            pop(2);
            push(org.osgl.mvc.server.asm.Opcodes.LONG);
            push(org.osgl.mvc.server.asm.Opcodes.TOP);
            break;
        case org.osgl.mvc.server.asm.Opcodes.FALOAD:
            pop(2);
            push(org.osgl.mvc.server.asm.Opcodes.FLOAT);
            break;
        case org.osgl.mvc.server.asm.Opcodes.DALOAD:
        case org.osgl.mvc.server.asm.Opcodes.L2D:
            pop(2);
            push(org.osgl.mvc.server.asm.Opcodes.DOUBLE);
            push(org.osgl.mvc.server.asm.Opcodes.TOP);
            break;
        case org.osgl.mvc.server.asm.Opcodes.AALOAD:
            pop(1);
            t1 = pop();
            if (t1 instanceof String) {
                pushDesc(((String) t1).substring(1));
            } else {
                push("java/lang/Object");
            }
            break;
        case org.osgl.mvc.server.asm.Opcodes.ISTORE:
        case org.osgl.mvc.server.asm.Opcodes.FSTORE:
        case org.osgl.mvc.server.asm.Opcodes.ASTORE:
            t1 = pop();
            set(iarg, t1);
            if (iarg > 0) {
                t2 = get(iarg - 1);
                if (t2 == org.osgl.mvc.server.asm.Opcodes.LONG || t2 == org.osgl.mvc.server.asm.Opcodes.DOUBLE) {
                    set(iarg - 1, org.osgl.mvc.server.asm.Opcodes.TOP);
                }
            }
            break;
        case org.osgl.mvc.server.asm.Opcodes.LSTORE:
        case org.osgl.mvc.server.asm.Opcodes.DSTORE:
            pop(1);
            t1 = pop();
            set(iarg, t1);
            set(iarg + 1, org.osgl.mvc.server.asm.Opcodes.TOP);
            if (iarg > 0) {
                t2 = get(iarg - 1);
                if (t2 == org.osgl.mvc.server.asm.Opcodes.LONG || t2 == org.osgl.mvc.server.asm.Opcodes.DOUBLE) {
                    set(iarg - 1, org.osgl.mvc.server.asm.Opcodes.TOP);
                }
            }
            break;
        case org.osgl.mvc.server.asm.Opcodes.IASTORE:
        case org.osgl.mvc.server.asm.Opcodes.BASTORE:
        case org.osgl.mvc.server.asm.Opcodes.CASTORE:
        case org.osgl.mvc.server.asm.Opcodes.SASTORE:
        case org.osgl.mvc.server.asm.Opcodes.FASTORE:
        case org.osgl.mvc.server.asm.Opcodes.AASTORE:
            pop(3);
            break;
        case org.osgl.mvc.server.asm.Opcodes.LASTORE:
        case org.osgl.mvc.server.asm.Opcodes.DASTORE:
            pop(4);
            break;
        case org.osgl.mvc.server.asm.Opcodes.POP:
        case org.osgl.mvc.server.asm.Opcodes.IFEQ:
        case org.osgl.mvc.server.asm.Opcodes.IFNE:
        case org.osgl.mvc.server.asm.Opcodes.IFLT:
        case org.osgl.mvc.server.asm.Opcodes.IFGE:
        case org.osgl.mvc.server.asm.Opcodes.IFGT:
        case org.osgl.mvc.server.asm.Opcodes.IFLE:
        case org.osgl.mvc.server.asm.Opcodes.IRETURN:
        case org.osgl.mvc.server.asm.Opcodes.FRETURN:
        case org.osgl.mvc.server.asm.Opcodes.ARETURN:
        case org.osgl.mvc.server.asm.Opcodes.TABLESWITCH:
        case org.osgl.mvc.server.asm.Opcodes.LOOKUPSWITCH:
        case org.osgl.mvc.server.asm.Opcodes.ATHROW:
        case org.osgl.mvc.server.asm.Opcodes.MONITORENTER:
        case org.osgl.mvc.server.asm.Opcodes.MONITOREXIT:
        case org.osgl.mvc.server.asm.Opcodes.IFNULL:
        case org.osgl.mvc.server.asm.Opcodes.IFNONNULL:
            pop(1);
            break;
        case org.osgl.mvc.server.asm.Opcodes.POP2:
        case org.osgl.mvc.server.asm.Opcodes.IF_ICMPEQ:
        case org.osgl.mvc.server.asm.Opcodes.IF_ICMPNE:
        case org.osgl.mvc.server.asm.Opcodes.IF_ICMPLT:
        case org.osgl.mvc.server.asm.Opcodes.IF_ICMPGE:
        case org.osgl.mvc.server.asm.Opcodes.IF_ICMPGT:
        case org.osgl.mvc.server.asm.Opcodes.IF_ICMPLE:
        case org.osgl.mvc.server.asm.Opcodes.IF_ACMPEQ:
        case org.osgl.mvc.server.asm.Opcodes.IF_ACMPNE:
        case org.osgl.mvc.server.asm.Opcodes.LRETURN:
        case org.osgl.mvc.server.asm.Opcodes.DRETURN:
            pop(2);
            break;
        case org.osgl.mvc.server.asm.Opcodes.DUP:
            t1 = pop();
            push(t1);
            push(t1);
            break;
        case org.osgl.mvc.server.asm.Opcodes.DUP_X1:
            t1 = pop();
            t2 = pop();
            push(t1);
            push(t2);
            push(t1);
            break;
        case org.osgl.mvc.server.asm.Opcodes.DUP_X2:
            t1 = pop();
            t2 = pop();
            t3 = pop();
            push(t1);
            push(t3);
            push(t2);
            push(t1);
            break;
        case org.osgl.mvc.server.asm.Opcodes.DUP2:
            t1 = pop();
            t2 = pop();
            push(t2);
            push(t1);
            push(t2);
            push(t1);
            break;
        case org.osgl.mvc.server.asm.Opcodes.DUP2_X1:
            t1 = pop();
            t2 = pop();
            t3 = pop();
            push(t2);
            push(t1);
            push(t3);
            push(t2);
            push(t1);
            break;
        case org.osgl.mvc.server.asm.Opcodes.DUP2_X2:
            t1 = pop();
            t2 = pop();
            t3 = pop();
            t4 = pop();
            push(t2);
            push(t1);
            push(t4);
            push(t3);
            push(t2);
            push(t1);
            break;
        case org.osgl.mvc.server.asm.Opcodes.SWAP:
            t1 = pop();
            t2 = pop();
            push(t1);
            push(t2);
            break;
        case org.osgl.mvc.server.asm.Opcodes.IADD:
        case org.osgl.mvc.server.asm.Opcodes.ISUB:
        case org.osgl.mvc.server.asm.Opcodes.IMUL:
        case org.osgl.mvc.server.asm.Opcodes.IDIV:
        case org.osgl.mvc.server.asm.Opcodes.IREM:
        case org.osgl.mvc.server.asm.Opcodes.IAND:
        case org.osgl.mvc.server.asm.Opcodes.IOR:
        case org.osgl.mvc.server.asm.Opcodes.IXOR:
        case org.osgl.mvc.server.asm.Opcodes.ISHL:
        case org.osgl.mvc.server.asm.Opcodes.ISHR:
        case org.osgl.mvc.server.asm.Opcodes.IUSHR:
        case org.osgl.mvc.server.asm.Opcodes.L2I:
        case org.osgl.mvc.server.asm.Opcodes.D2I:
        case org.osgl.mvc.server.asm.Opcodes.FCMPL:
        case org.osgl.mvc.server.asm.Opcodes.FCMPG:
            pop(2);
            push(org.osgl.mvc.server.asm.Opcodes.INTEGER);
            break;
        case org.osgl.mvc.server.asm.Opcodes.LADD:
        case org.osgl.mvc.server.asm.Opcodes.LSUB:
        case org.osgl.mvc.server.asm.Opcodes.LMUL:
        case org.osgl.mvc.server.asm.Opcodes.LDIV:
        case org.osgl.mvc.server.asm.Opcodes.LREM:
        case org.osgl.mvc.server.asm.Opcodes.LAND:
        case org.osgl.mvc.server.asm.Opcodes.LOR:
        case org.osgl.mvc.server.asm.Opcodes.LXOR:
            pop(4);
            push(org.osgl.mvc.server.asm.Opcodes.LONG);
            push(org.osgl.mvc.server.asm.Opcodes.TOP);
            break;
        case org.osgl.mvc.server.asm.Opcodes.FADD:
        case org.osgl.mvc.server.asm.Opcodes.FSUB:
        case org.osgl.mvc.server.asm.Opcodes.FMUL:
        case org.osgl.mvc.server.asm.Opcodes.FDIV:
        case org.osgl.mvc.server.asm.Opcodes.FREM:
        case org.osgl.mvc.server.asm.Opcodes.L2F:
        case org.osgl.mvc.server.asm.Opcodes.D2F:
            pop(2);
            push(org.osgl.mvc.server.asm.Opcodes.FLOAT);
            break;
        case org.osgl.mvc.server.asm.Opcodes.DADD:
        case org.osgl.mvc.server.asm.Opcodes.DSUB:
        case org.osgl.mvc.server.asm.Opcodes.DMUL:
        case org.osgl.mvc.server.asm.Opcodes.DDIV:
        case org.osgl.mvc.server.asm.Opcodes.DREM:
            pop(4);
            push(org.osgl.mvc.server.asm.Opcodes.DOUBLE);
            push(org.osgl.mvc.server.asm.Opcodes.TOP);
            break;
        case org.osgl.mvc.server.asm.Opcodes.LSHL:
        case org.osgl.mvc.server.asm.Opcodes.LSHR:
        case org.osgl.mvc.server.asm.Opcodes.LUSHR:
            pop(3);
            push(org.osgl.mvc.server.asm.Opcodes.LONG);
            push(org.osgl.mvc.server.asm.Opcodes.TOP);
            break;
        case org.osgl.mvc.server.asm.Opcodes.IINC:
            set(iarg, org.osgl.mvc.server.asm.Opcodes.INTEGER);
            break;
        case org.osgl.mvc.server.asm.Opcodes.I2L:
        case org.osgl.mvc.server.asm.Opcodes.F2L:
            pop(1);
            push(org.osgl.mvc.server.asm.Opcodes.LONG);
            push(org.osgl.mvc.server.asm.Opcodes.TOP);
            break;
        case org.osgl.mvc.server.asm.Opcodes.I2F:
            pop(1);
            push(org.osgl.mvc.server.asm.Opcodes.FLOAT);
            break;
        case org.osgl.mvc.server.asm.Opcodes.I2D:
        case org.osgl.mvc.server.asm.Opcodes.F2D:
            pop(1);
            push(org.osgl.mvc.server.asm.Opcodes.DOUBLE);
            push(org.osgl.mvc.server.asm.Opcodes.TOP);
            break;
        case org.osgl.mvc.server.asm.Opcodes.F2I:
        case org.osgl.mvc.server.asm.Opcodes.ARRAYLENGTH:
        case org.osgl.mvc.server.asm.Opcodes.INSTANCEOF:
            pop(1);
            push(org.osgl.mvc.server.asm.Opcodes.INTEGER);
            break;
        case org.osgl.mvc.server.asm.Opcodes.LCMP:
        case org.osgl.mvc.server.asm.Opcodes.DCMPL:
        case org.osgl.mvc.server.asm.Opcodes.DCMPG:
            pop(4);
            push(org.osgl.mvc.server.asm.Opcodes.INTEGER);
            break;
        case org.osgl.mvc.server.asm.Opcodes.JSR:
        case org.osgl.mvc.server.asm.Opcodes.RET:
            throw new RuntimeException("JSR/RET are not supported");
        case org.osgl.mvc.server.asm.Opcodes.GETSTATIC:
            pushDesc(sarg);
            break;
        case org.osgl.mvc.server.asm.Opcodes.PUTSTATIC:
            pop(sarg);
            break;
        case org.osgl.mvc.server.asm.Opcodes.GETFIELD:
            pop(1);
            pushDesc(sarg);
            break;
        case org.osgl.mvc.server.asm.Opcodes.PUTFIELD:
            pop(sarg);
            pop();
            break;
        case org.osgl.mvc.server.asm.Opcodes.NEW:
            push(labels.get(0));
            break;
        case org.osgl.mvc.server.asm.Opcodes.NEWARRAY:
            pop();
            switch (iarg) {
            case org.osgl.mvc.server.asm.Opcodes.T_BOOLEAN:
                pushDesc("[Z");
                break;
            case org.osgl.mvc.server.asm.Opcodes.T_CHAR:
                pushDesc("[C");
                break;
            case org.osgl.mvc.server.asm.Opcodes.T_BYTE:
                pushDesc("[B");
                break;
            case org.osgl.mvc.server.asm.Opcodes.T_SHORT:
                pushDesc("[S");
                break;
            case org.osgl.mvc.server.asm.Opcodes.T_INT:
                pushDesc("[I");
                break;
            case org.osgl.mvc.server.asm.Opcodes.T_FLOAT:
                pushDesc("[F");
                break;
            case org.osgl.mvc.server.asm.Opcodes.T_DOUBLE:
                pushDesc("[D");
                break;
            // case Opcodes.T_LONG:
            default:
                pushDesc("[J");
                break;
            }
            break;
        case org.osgl.mvc.server.asm.Opcodes.ANEWARRAY:
            pop();
            pushDesc("[" + org.osgl.mvc.server.asm.Type.getObjectType(sarg));
            break;
        case org.osgl.mvc.server.asm.Opcodes.CHECKCAST:
            pop();
            pushDesc(org.osgl.mvc.server.asm.Type.getObjectType(sarg).getDescriptor());
            break;
        // case Opcodes.MULTIANEWARRAY:
        default:
            pop(iarg);
            pushDesc(sarg);
            break;
        }
        labels = null;
    }
}
