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

/**
 * A {@link org.osgl.mvc.server.asm.MethodVisitor} providing a more detailed API to generate and
 * transform instructions.
 *
 * @author Eric Bruneton
 */
public class InstructionAdapter extends org.osgl.mvc.server.asm.MethodVisitor {

    public final static org.osgl.mvc.server.asm.Type OBJECT_TYPE = org.osgl.mvc.server.asm.Type.getType("Ljava/lang/Object;");

    /**
     * Creates a new {@link InstructionAdapter}. <i>Subclasses must not use this
     * constructor</i>. Instead, they must use the
     * {@link #InstructionAdapter(int, org.osgl.mvc.server.asm.MethodVisitor)} version.
     *
     * @param mv
     *            the method visitor to which this adapter delegates calls.
     * @throws IllegalStateException
     *             If a subclass calls this constructor.
     */
    public InstructionAdapter(final org.osgl.mvc.server.asm.MethodVisitor mv) {
        this(org.osgl.mvc.server.asm.Opcodes.ASM5, mv);
        if (getClass() != InstructionAdapter.class) {
            throw new IllegalStateException();
        }
    }

    /**
     * Creates a new {@link InstructionAdapter}.
     *
     * @param api
     *            the ASM API version implemented by this visitor. Must be one
     *            of {@link org.osgl.mvc.server.asm.Opcodes#ASM4} or {@link org.osgl.mvc.server.asm.Opcodes#ASM5}.
     * @param mv
     *            the method visitor to which this adapter delegates calls.
     */
    protected InstructionAdapter(final int api, final org.osgl.mvc.server.asm.MethodVisitor mv) {
        super(api, mv);
    }

    @Override
    public void visitInsn(final int opcode) {
        switch (opcode) {
        case org.osgl.mvc.server.asm.Opcodes.NOP:
            nop();
            break;
        case org.osgl.mvc.server.asm.Opcodes.ACONST_NULL:
            aconst(null);
            break;
        case org.osgl.mvc.server.asm.Opcodes.ICONST_M1:
        case org.osgl.mvc.server.asm.Opcodes.ICONST_0:
        case org.osgl.mvc.server.asm.Opcodes.ICONST_1:
        case org.osgl.mvc.server.asm.Opcodes.ICONST_2:
        case org.osgl.mvc.server.asm.Opcodes.ICONST_3:
        case org.osgl.mvc.server.asm.Opcodes.ICONST_4:
        case org.osgl.mvc.server.asm.Opcodes.ICONST_5:
            iconst(opcode - org.osgl.mvc.server.asm.Opcodes.ICONST_0);
            break;
        case org.osgl.mvc.server.asm.Opcodes.LCONST_0:
        case org.osgl.mvc.server.asm.Opcodes.LCONST_1:
            lconst(opcode - org.osgl.mvc.server.asm.Opcodes.LCONST_0);
            break;
        case org.osgl.mvc.server.asm.Opcodes.FCONST_0:
        case org.osgl.mvc.server.asm.Opcodes.FCONST_1:
        case org.osgl.mvc.server.asm.Opcodes.FCONST_2:
            fconst(opcode - org.osgl.mvc.server.asm.Opcodes.FCONST_0);
            break;
        case org.osgl.mvc.server.asm.Opcodes.DCONST_0:
        case org.osgl.mvc.server.asm.Opcodes.DCONST_1:
            dconst(opcode - org.osgl.mvc.server.asm.Opcodes.DCONST_0);
            break;
        case org.osgl.mvc.server.asm.Opcodes.IALOAD:
            aload(org.osgl.mvc.server.asm.Type.INT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.LALOAD:
            aload(org.osgl.mvc.server.asm.Type.LONG_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.FALOAD:
            aload(org.osgl.mvc.server.asm.Type.FLOAT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.DALOAD:
            aload(org.osgl.mvc.server.asm.Type.DOUBLE_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.AALOAD:
            aload(OBJECT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.BALOAD:
            aload(org.osgl.mvc.server.asm.Type.BYTE_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.CALOAD:
            aload(org.osgl.mvc.server.asm.Type.CHAR_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.SALOAD:
            aload(org.osgl.mvc.server.asm.Type.SHORT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.IASTORE:
            astore(org.osgl.mvc.server.asm.Type.INT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.LASTORE:
            astore(org.osgl.mvc.server.asm.Type.LONG_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.FASTORE:
            astore(org.osgl.mvc.server.asm.Type.FLOAT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.DASTORE:
            astore(org.osgl.mvc.server.asm.Type.DOUBLE_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.AASTORE:
            astore(OBJECT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.BASTORE:
            astore(org.osgl.mvc.server.asm.Type.BYTE_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.CASTORE:
            astore(org.osgl.mvc.server.asm.Type.CHAR_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.SASTORE:
            astore(org.osgl.mvc.server.asm.Type.SHORT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.POP:
            pop();
            break;
        case org.osgl.mvc.server.asm.Opcodes.POP2:
            pop2();
            break;
        case org.osgl.mvc.server.asm.Opcodes.DUP:
            dup();
            break;
        case org.osgl.mvc.server.asm.Opcodes.DUP_X1:
            dupX1();
            break;
        case org.osgl.mvc.server.asm.Opcodes.DUP_X2:
            dupX2();
            break;
        case org.osgl.mvc.server.asm.Opcodes.DUP2:
            dup2();
            break;
        case org.osgl.mvc.server.asm.Opcodes.DUP2_X1:
            dup2X1();
            break;
        case org.osgl.mvc.server.asm.Opcodes.DUP2_X2:
            dup2X2();
            break;
        case org.osgl.mvc.server.asm.Opcodes.SWAP:
            swap();
            break;
        case org.osgl.mvc.server.asm.Opcodes.IADD:
            add(org.osgl.mvc.server.asm.Type.INT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.LADD:
            add(org.osgl.mvc.server.asm.Type.LONG_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.FADD:
            add(org.osgl.mvc.server.asm.Type.FLOAT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.DADD:
            add(org.osgl.mvc.server.asm.Type.DOUBLE_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.ISUB:
            sub(org.osgl.mvc.server.asm.Type.INT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.LSUB:
            sub(org.osgl.mvc.server.asm.Type.LONG_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.FSUB:
            sub(org.osgl.mvc.server.asm.Type.FLOAT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.DSUB:
            sub(org.osgl.mvc.server.asm.Type.DOUBLE_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.IMUL:
            mul(org.osgl.mvc.server.asm.Type.INT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.LMUL:
            mul(org.osgl.mvc.server.asm.Type.LONG_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.FMUL:
            mul(org.osgl.mvc.server.asm.Type.FLOAT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.DMUL:
            mul(org.osgl.mvc.server.asm.Type.DOUBLE_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.IDIV:
            div(org.osgl.mvc.server.asm.Type.INT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.LDIV:
            div(org.osgl.mvc.server.asm.Type.LONG_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.FDIV:
            div(org.osgl.mvc.server.asm.Type.FLOAT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.DDIV:
            div(org.osgl.mvc.server.asm.Type.DOUBLE_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.IREM:
            rem(org.osgl.mvc.server.asm.Type.INT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.LREM:
            rem(org.osgl.mvc.server.asm.Type.LONG_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.FREM:
            rem(org.osgl.mvc.server.asm.Type.FLOAT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.DREM:
            rem(org.osgl.mvc.server.asm.Type.DOUBLE_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.INEG:
            neg(org.osgl.mvc.server.asm.Type.INT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.LNEG:
            neg(org.osgl.mvc.server.asm.Type.LONG_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.FNEG:
            neg(org.osgl.mvc.server.asm.Type.FLOAT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.DNEG:
            neg(org.osgl.mvc.server.asm.Type.DOUBLE_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.ISHL:
            shl(org.osgl.mvc.server.asm.Type.INT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.LSHL:
            shl(org.osgl.mvc.server.asm.Type.LONG_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.ISHR:
            shr(org.osgl.mvc.server.asm.Type.INT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.LSHR:
            shr(org.osgl.mvc.server.asm.Type.LONG_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.IUSHR:
            ushr(org.osgl.mvc.server.asm.Type.INT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.LUSHR:
            ushr(org.osgl.mvc.server.asm.Type.LONG_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.IAND:
            and(org.osgl.mvc.server.asm.Type.INT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.LAND:
            and(org.osgl.mvc.server.asm.Type.LONG_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.IOR:
            or(org.osgl.mvc.server.asm.Type.INT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.LOR:
            or(org.osgl.mvc.server.asm.Type.LONG_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.IXOR:
            xor(org.osgl.mvc.server.asm.Type.INT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.LXOR:
            xor(org.osgl.mvc.server.asm.Type.LONG_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.I2L:
            cast(org.osgl.mvc.server.asm.Type.INT_TYPE, org.osgl.mvc.server.asm.Type.LONG_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.I2F:
            cast(org.osgl.mvc.server.asm.Type.INT_TYPE, org.osgl.mvc.server.asm.Type.FLOAT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.I2D:
            cast(org.osgl.mvc.server.asm.Type.INT_TYPE, org.osgl.mvc.server.asm.Type.DOUBLE_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.L2I:
            cast(org.osgl.mvc.server.asm.Type.LONG_TYPE, org.osgl.mvc.server.asm.Type.INT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.L2F:
            cast(org.osgl.mvc.server.asm.Type.LONG_TYPE, org.osgl.mvc.server.asm.Type.FLOAT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.L2D:
            cast(org.osgl.mvc.server.asm.Type.LONG_TYPE, org.osgl.mvc.server.asm.Type.DOUBLE_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.F2I:
            cast(org.osgl.mvc.server.asm.Type.FLOAT_TYPE, org.osgl.mvc.server.asm.Type.INT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.F2L:
            cast(org.osgl.mvc.server.asm.Type.FLOAT_TYPE, org.osgl.mvc.server.asm.Type.LONG_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.F2D:
            cast(org.osgl.mvc.server.asm.Type.FLOAT_TYPE, org.osgl.mvc.server.asm.Type.DOUBLE_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.D2I:
            cast(org.osgl.mvc.server.asm.Type.DOUBLE_TYPE, org.osgl.mvc.server.asm.Type.INT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.D2L:
            cast(org.osgl.mvc.server.asm.Type.DOUBLE_TYPE, org.osgl.mvc.server.asm.Type.LONG_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.D2F:
            cast(org.osgl.mvc.server.asm.Type.DOUBLE_TYPE, org.osgl.mvc.server.asm.Type.FLOAT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.I2B:
            cast(org.osgl.mvc.server.asm.Type.INT_TYPE, org.osgl.mvc.server.asm.Type.BYTE_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.I2C:
            cast(org.osgl.mvc.server.asm.Type.INT_TYPE, org.osgl.mvc.server.asm.Type.CHAR_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.I2S:
            cast(org.osgl.mvc.server.asm.Type.INT_TYPE, org.osgl.mvc.server.asm.Type.SHORT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.LCMP:
            lcmp();
            break;
        case org.osgl.mvc.server.asm.Opcodes.FCMPL:
            cmpl(org.osgl.mvc.server.asm.Type.FLOAT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.FCMPG:
            cmpg(org.osgl.mvc.server.asm.Type.FLOAT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.DCMPL:
            cmpl(org.osgl.mvc.server.asm.Type.DOUBLE_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.DCMPG:
            cmpg(org.osgl.mvc.server.asm.Type.DOUBLE_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.IRETURN:
            areturn(org.osgl.mvc.server.asm.Type.INT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.LRETURN:
            areturn(org.osgl.mvc.server.asm.Type.LONG_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.FRETURN:
            areturn(org.osgl.mvc.server.asm.Type.FLOAT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.DRETURN:
            areturn(org.osgl.mvc.server.asm.Type.DOUBLE_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.ARETURN:
            areturn(OBJECT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.RETURN:
            areturn(org.osgl.mvc.server.asm.Type.VOID_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.ARRAYLENGTH:
            arraylength();
            break;
        case org.osgl.mvc.server.asm.Opcodes.ATHROW:
            athrow();
            break;
        case org.osgl.mvc.server.asm.Opcodes.MONITORENTER:
            monitorenter();
            break;
        case org.osgl.mvc.server.asm.Opcodes.MONITOREXIT:
            monitorexit();
            break;
        default:
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void visitIntInsn(final int opcode, final int operand) {
        switch (opcode) {
        case org.osgl.mvc.server.asm.Opcodes.BIPUSH:
            iconst(operand);
            break;
        case org.osgl.mvc.server.asm.Opcodes.SIPUSH:
            iconst(operand);
            break;
        case org.osgl.mvc.server.asm.Opcodes.NEWARRAY:
            switch (operand) {
            case org.osgl.mvc.server.asm.Opcodes.T_BOOLEAN:
                newarray(org.osgl.mvc.server.asm.Type.BOOLEAN_TYPE);
                break;
            case org.osgl.mvc.server.asm.Opcodes.T_CHAR:
                newarray(org.osgl.mvc.server.asm.Type.CHAR_TYPE);
                break;
            case org.osgl.mvc.server.asm.Opcodes.T_BYTE:
                newarray(org.osgl.mvc.server.asm.Type.BYTE_TYPE);
                break;
            case org.osgl.mvc.server.asm.Opcodes.T_SHORT:
                newarray(org.osgl.mvc.server.asm.Type.SHORT_TYPE);
                break;
            case org.osgl.mvc.server.asm.Opcodes.T_INT:
                newarray(org.osgl.mvc.server.asm.Type.INT_TYPE);
                break;
            case org.osgl.mvc.server.asm.Opcodes.T_FLOAT:
                newarray(org.osgl.mvc.server.asm.Type.FLOAT_TYPE);
                break;
            case org.osgl.mvc.server.asm.Opcodes.T_LONG:
                newarray(org.osgl.mvc.server.asm.Type.LONG_TYPE);
                break;
            case org.osgl.mvc.server.asm.Opcodes.T_DOUBLE:
                newarray(org.osgl.mvc.server.asm.Type.DOUBLE_TYPE);
                break;
            default:
                throw new IllegalArgumentException();
            }
            break;
        default:
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void visitVarInsn(final int opcode, final int var) {
        switch (opcode) {
        case org.osgl.mvc.server.asm.Opcodes.ILOAD:
            load(var, org.osgl.mvc.server.asm.Type.INT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.LLOAD:
            load(var, org.osgl.mvc.server.asm.Type.LONG_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.FLOAD:
            load(var, org.osgl.mvc.server.asm.Type.FLOAT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.DLOAD:
            load(var, org.osgl.mvc.server.asm.Type.DOUBLE_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.ALOAD:
            load(var, OBJECT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.ISTORE:
            store(var, org.osgl.mvc.server.asm.Type.INT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.LSTORE:
            store(var, org.osgl.mvc.server.asm.Type.LONG_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.FSTORE:
            store(var, org.osgl.mvc.server.asm.Type.FLOAT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.DSTORE:
            store(var, org.osgl.mvc.server.asm.Type.DOUBLE_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.ASTORE:
            store(var, OBJECT_TYPE);
            break;
        case org.osgl.mvc.server.asm.Opcodes.RET:
            ret(var);
            break;
        default:
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void visitTypeInsn(final int opcode, final String type) {
        org.osgl.mvc.server.asm.Type t = org.osgl.mvc.server.asm.Type.getObjectType(type);
        switch (opcode) {
        case org.osgl.mvc.server.asm.Opcodes.NEW:
            anew(t);
            break;
        case org.osgl.mvc.server.asm.Opcodes.ANEWARRAY:
            newarray(t);
            break;
        case org.osgl.mvc.server.asm.Opcodes.CHECKCAST:
            checkcast(t);
            break;
        case org.osgl.mvc.server.asm.Opcodes.INSTANCEOF:
            instanceOf(t);
            break;
        default:
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void visitFieldInsn(final int opcode, final String owner,
            final String name, final String desc) {
        switch (opcode) {
        case org.osgl.mvc.server.asm.Opcodes.GETSTATIC:
            getstatic(owner, name, desc);
            break;
        case org.osgl.mvc.server.asm.Opcodes.PUTSTATIC:
            putstatic(owner, name, desc);
            break;
        case org.osgl.mvc.server.asm.Opcodes.GETFIELD:
            getfield(owner, name, desc);
            break;
        case org.osgl.mvc.server.asm.Opcodes.PUTFIELD:
            putfield(owner, name, desc);
            break;
        default:
            throw new IllegalArgumentException();
        }
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
        switch (opcode) {
        case org.osgl.mvc.server.asm.Opcodes.INVOKESPECIAL:
            invokespecial(owner, name, desc, itf);
            break;
        case org.osgl.mvc.server.asm.Opcodes.INVOKEVIRTUAL:
            invokevirtual(owner, name, desc, itf);
            break;
        case org.osgl.mvc.server.asm.Opcodes.INVOKESTATIC:
            invokestatic(owner, name, desc, itf);
            break;
        case org.osgl.mvc.server.asm.Opcodes.INVOKEINTERFACE:
            invokeinterface(owner, name, desc);
            break;
        default:
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, org.osgl.mvc.server.asm.Handle bsm,
            Object... bsmArgs) {
        invokedynamic(name, desc, bsm, bsmArgs);
    }

    @Override
    public void visitJumpInsn(final int opcode, final org.osgl.mvc.server.asm.Label label) {
        switch (opcode) {
        case org.osgl.mvc.server.asm.Opcodes.IFEQ:
            ifeq(label);
            break;
        case org.osgl.mvc.server.asm.Opcodes.IFNE:
            ifne(label);
            break;
        case org.osgl.mvc.server.asm.Opcodes.IFLT:
            iflt(label);
            break;
        case org.osgl.mvc.server.asm.Opcodes.IFGE:
            ifge(label);
            break;
        case org.osgl.mvc.server.asm.Opcodes.IFGT:
            ifgt(label);
            break;
        case org.osgl.mvc.server.asm.Opcodes.IFLE:
            ifle(label);
            break;
        case org.osgl.mvc.server.asm.Opcodes.IF_ICMPEQ:
            ificmpeq(label);
            break;
        case org.osgl.mvc.server.asm.Opcodes.IF_ICMPNE:
            ificmpne(label);
            break;
        case org.osgl.mvc.server.asm.Opcodes.IF_ICMPLT:
            ificmplt(label);
            break;
        case org.osgl.mvc.server.asm.Opcodes.IF_ICMPGE:
            ificmpge(label);
            break;
        case org.osgl.mvc.server.asm.Opcodes.IF_ICMPGT:
            ificmpgt(label);
            break;
        case org.osgl.mvc.server.asm.Opcodes.IF_ICMPLE:
            ificmple(label);
            break;
        case org.osgl.mvc.server.asm.Opcodes.IF_ACMPEQ:
            ifacmpeq(label);
            break;
        case org.osgl.mvc.server.asm.Opcodes.IF_ACMPNE:
            ifacmpne(label);
            break;
        case org.osgl.mvc.server.asm.Opcodes.GOTO:
            goTo(label);
            break;
        case org.osgl.mvc.server.asm.Opcodes.JSR:
            jsr(label);
            break;
        case org.osgl.mvc.server.asm.Opcodes.IFNULL:
            ifnull(label);
            break;
        case org.osgl.mvc.server.asm.Opcodes.IFNONNULL:
            ifnonnull(label);
            break;
        default:
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void visitLabel(final org.osgl.mvc.server.asm.Label label) {
        mark(label);
    }

    @Override
    public void visitLdcInsn(final Object cst) {
        if (cst instanceof Integer) {
            int val = ((Integer) cst).intValue();
            iconst(val);
        } else if (cst instanceof Byte) {
            int val = ((Byte) cst).intValue();
            iconst(val);
        } else if (cst instanceof Character) {
            int val = ((Character) cst).charValue();
            iconst(val);
        } else if (cst instanceof Short) {
            int val = ((Short) cst).intValue();
            iconst(val);
        } else if (cst instanceof Boolean) {
            int val = ((Boolean) cst).booleanValue() ? 1 : 0;
            iconst(val);
        } else if (cst instanceof Float) {
            float val = ((Float) cst).floatValue();
            fconst(val);
        } else if (cst instanceof Long) {
            long val = ((Long) cst).longValue();
            lconst(val);
        } else if (cst instanceof Double) {
            double val = ((Double) cst).doubleValue();
            dconst(val);
        } else if (cst instanceof String) {
            aconst(cst);
        } else if (cst instanceof org.osgl.mvc.server.asm.Type) {
            tconst((org.osgl.mvc.server.asm.Type) cst);
        } else if (cst instanceof org.osgl.mvc.server.asm.Handle) {
            hconst((org.osgl.mvc.server.asm.Handle) cst);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void visitIincInsn(final int var, final int increment) {
        iinc(var, increment);
    }

    @Override
    public void visitTableSwitchInsn(final int min, final int max,
            final org.osgl.mvc.server.asm.Label dflt, final org.osgl.mvc.server.asm.Label... labels) {
        tableswitch(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(final org.osgl.mvc.server.asm.Label dflt, final int[] keys,
            final org.osgl.mvc.server.asm.Label[] labels) {
        lookupswitch(dflt, keys, labels);
    }

    @Override
    public void visitMultiANewArrayInsn(final String desc, final int dims) {
        multianewarray(desc, dims);
    }

    // -----------------------------------------------------------------------

    public void nop() {
        mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.NOP);
    }

    public void aconst(final Object cst) {
        if (cst == null) {
            mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.ACONST_NULL);
        } else {
            mv.visitLdcInsn(cst);
        }
    }

    public void iconst(final int cst) {
        if (cst >= -1 && cst <= 5) {
            mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.ICONST_0 + cst);
        } else if (cst >= Byte.MIN_VALUE && cst <= Byte.MAX_VALUE) {
            mv.visitIntInsn(org.osgl.mvc.server.asm.Opcodes.BIPUSH, cst);
        } else if (cst >= Short.MIN_VALUE && cst <= Short.MAX_VALUE) {
            mv.visitIntInsn(org.osgl.mvc.server.asm.Opcodes.SIPUSH, cst);
        } else {
            mv.visitLdcInsn(new Integer(cst));
        }
    }

    public void lconst(final long cst) {
        if (cst == 0L || cst == 1L) {
            mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.LCONST_0 + (int) cst);
        } else {
            mv.visitLdcInsn(new Long(cst));
        }
    }

    public void fconst(final float cst) {
        int bits = Float.floatToIntBits(cst);
        if (bits == 0L || bits == 0x3f800000 || bits == 0x40000000) { // 0..2
            mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.FCONST_0 + (int) cst);
        } else {
            mv.visitLdcInsn(new Float(cst));
        }
    }

    public void dconst(final double cst) {
        long bits = Double.doubleToLongBits(cst);
        if (bits == 0L || bits == 0x3ff0000000000000L) { // +0.0d and 1.0d
            mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.DCONST_0 + (int) cst);
        } else {
            mv.visitLdcInsn(new Double(cst));
        }
    }

    public void tconst(final org.osgl.mvc.server.asm.Type type) {
        mv.visitLdcInsn(type);
    }

    public void hconst(final org.osgl.mvc.server.asm.Handle handle) {
        mv.visitLdcInsn(handle);
    }

    public void load(final int var, final org.osgl.mvc.server.asm.Type type) {
        mv.visitVarInsn(type.getOpcode(org.osgl.mvc.server.asm.Opcodes.ILOAD), var);
    }

    public void aload(final org.osgl.mvc.server.asm.Type type) {
        mv.visitInsn(type.getOpcode(org.osgl.mvc.server.asm.Opcodes.IALOAD));
    }

    public void store(final int var, final org.osgl.mvc.server.asm.Type type) {
        mv.visitVarInsn(type.getOpcode(org.osgl.mvc.server.asm.Opcodes.ISTORE), var);
    }

    public void astore(final org.osgl.mvc.server.asm.Type type) {
        mv.visitInsn(type.getOpcode(org.osgl.mvc.server.asm.Opcodes.IASTORE));
    }

    public void pop() {
        mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.POP);
    }

    public void pop2() {
        mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.POP2);
    }

    public void dup() {
        mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.DUP);
    }

    public void dup2() {
        mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.DUP2);
    }

    public void dupX1() {
        mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.DUP_X1);
    }

    public void dupX2() {
        mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.DUP_X2);
    }

    public void dup2X1() {
        mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.DUP2_X1);
    }

    public void dup2X2() {
        mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.DUP2_X2);
    }

    public void swap() {
        mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.SWAP);
    }

    public void add(final org.osgl.mvc.server.asm.Type type) {
        mv.visitInsn(type.getOpcode(org.osgl.mvc.server.asm.Opcodes.IADD));
    }

    public void sub(final org.osgl.mvc.server.asm.Type type) {
        mv.visitInsn(type.getOpcode(org.osgl.mvc.server.asm.Opcodes.ISUB));
    }

    public void mul(final org.osgl.mvc.server.asm.Type type) {
        mv.visitInsn(type.getOpcode(org.osgl.mvc.server.asm.Opcodes.IMUL));
    }

    public void div(final org.osgl.mvc.server.asm.Type type) {
        mv.visitInsn(type.getOpcode(org.osgl.mvc.server.asm.Opcodes.IDIV));
    }

    public void rem(final org.osgl.mvc.server.asm.Type type) {
        mv.visitInsn(type.getOpcode(org.osgl.mvc.server.asm.Opcodes.IREM));
    }

    public void neg(final org.osgl.mvc.server.asm.Type type) {
        mv.visitInsn(type.getOpcode(org.osgl.mvc.server.asm.Opcodes.INEG));
    }

    public void shl(final org.osgl.mvc.server.asm.Type type) {
        mv.visitInsn(type.getOpcode(org.osgl.mvc.server.asm.Opcodes.ISHL));
    }

    public void shr(final org.osgl.mvc.server.asm.Type type) {
        mv.visitInsn(type.getOpcode(org.osgl.mvc.server.asm.Opcodes.ISHR));
    }

    public void ushr(final org.osgl.mvc.server.asm.Type type) {
        mv.visitInsn(type.getOpcode(org.osgl.mvc.server.asm.Opcodes.IUSHR));
    }

    public void and(final org.osgl.mvc.server.asm.Type type) {
        mv.visitInsn(type.getOpcode(org.osgl.mvc.server.asm.Opcodes.IAND));
    }

    public void or(final org.osgl.mvc.server.asm.Type type) {
        mv.visitInsn(type.getOpcode(org.osgl.mvc.server.asm.Opcodes.IOR));
    }

    public void xor(final org.osgl.mvc.server.asm.Type type) {
        mv.visitInsn(type.getOpcode(org.osgl.mvc.server.asm.Opcodes.IXOR));
    }

    public void iinc(final int var, final int increment) {
        mv.visitIincInsn(var, increment);
    }

    public void cast(final org.osgl.mvc.server.asm.Type from, final org.osgl.mvc.server.asm.Type to) {
        if (from != to) {
            if (from == org.osgl.mvc.server.asm.Type.DOUBLE_TYPE) {
                if (to == org.osgl.mvc.server.asm.Type.FLOAT_TYPE) {
                    mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.D2F);
                } else if (to == org.osgl.mvc.server.asm.Type.LONG_TYPE) {
                    mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.D2L);
                } else {
                    mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.D2I);
                    cast(org.osgl.mvc.server.asm.Type.INT_TYPE, to);
                }
            } else if (from == org.osgl.mvc.server.asm.Type.FLOAT_TYPE) {
                if (to == org.osgl.mvc.server.asm.Type.DOUBLE_TYPE) {
                    mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.F2D);
                } else if (to == org.osgl.mvc.server.asm.Type.LONG_TYPE) {
                    mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.F2L);
                } else {
                    mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.F2I);
                    cast(org.osgl.mvc.server.asm.Type.INT_TYPE, to);
                }
            } else if (from == org.osgl.mvc.server.asm.Type.LONG_TYPE) {
                if (to == org.osgl.mvc.server.asm.Type.DOUBLE_TYPE) {
                    mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.L2D);
                } else if (to == org.osgl.mvc.server.asm.Type.FLOAT_TYPE) {
                    mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.L2F);
                } else {
                    mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.L2I);
                    cast(org.osgl.mvc.server.asm.Type.INT_TYPE, to);
                }
            } else {
                if (to == org.osgl.mvc.server.asm.Type.BYTE_TYPE) {
                    mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.I2B);
                } else if (to == org.osgl.mvc.server.asm.Type.CHAR_TYPE) {
                    mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.I2C);
                } else if (to == org.osgl.mvc.server.asm.Type.DOUBLE_TYPE) {
                    mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.I2D);
                } else if (to == org.osgl.mvc.server.asm.Type.FLOAT_TYPE) {
                    mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.I2F);
                } else if (to == org.osgl.mvc.server.asm.Type.LONG_TYPE) {
                    mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.I2L);
                } else if (to == org.osgl.mvc.server.asm.Type.SHORT_TYPE) {
                    mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.I2S);
                }
            }
        }
    }

    public void lcmp() {
        mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.LCMP);
    }

    public void cmpl(final org.osgl.mvc.server.asm.Type type) {
        mv.visitInsn(type == org.osgl.mvc.server.asm.Type.FLOAT_TYPE ? org.osgl.mvc.server.asm.Opcodes.FCMPL : org.osgl.mvc.server.asm.Opcodes.DCMPL);
    }

    public void cmpg(final org.osgl.mvc.server.asm.Type type) {
        mv.visitInsn(type == org.osgl.mvc.server.asm.Type.FLOAT_TYPE ? org.osgl.mvc.server.asm.Opcodes.FCMPG : org.osgl.mvc.server.asm.Opcodes.DCMPG);
    }

    public void ifeq(final org.osgl.mvc.server.asm.Label label) {
        mv.visitJumpInsn(org.osgl.mvc.server.asm.Opcodes.IFEQ, label);
    }

    public void ifne(final org.osgl.mvc.server.asm.Label label) {
        mv.visitJumpInsn(org.osgl.mvc.server.asm.Opcodes.IFNE, label);
    }

    public void iflt(final org.osgl.mvc.server.asm.Label label) {
        mv.visitJumpInsn(org.osgl.mvc.server.asm.Opcodes.IFLT, label);
    }

    public void ifge(final org.osgl.mvc.server.asm.Label label) {
        mv.visitJumpInsn(org.osgl.mvc.server.asm.Opcodes.IFGE, label);
    }

    public void ifgt(final org.osgl.mvc.server.asm.Label label) {
        mv.visitJumpInsn(org.osgl.mvc.server.asm.Opcodes.IFGT, label);
    }

    public void ifle(final org.osgl.mvc.server.asm.Label label) {
        mv.visitJumpInsn(org.osgl.mvc.server.asm.Opcodes.IFLE, label);
    }

    public void ificmpeq(final org.osgl.mvc.server.asm.Label label) {
        mv.visitJumpInsn(org.osgl.mvc.server.asm.Opcodes.IF_ICMPEQ, label);
    }

    public void ificmpne(final org.osgl.mvc.server.asm.Label label) {
        mv.visitJumpInsn(org.osgl.mvc.server.asm.Opcodes.IF_ICMPNE, label);
    }

    public void ificmplt(final org.osgl.mvc.server.asm.Label label) {
        mv.visitJumpInsn(org.osgl.mvc.server.asm.Opcodes.IF_ICMPLT, label);
    }

    public void ificmpge(final org.osgl.mvc.server.asm.Label label) {
        mv.visitJumpInsn(org.osgl.mvc.server.asm.Opcodes.IF_ICMPGE, label);
    }

    public void ificmpgt(final org.osgl.mvc.server.asm.Label label) {
        mv.visitJumpInsn(org.osgl.mvc.server.asm.Opcodes.IF_ICMPGT, label);
    }

    public void ificmple(final org.osgl.mvc.server.asm.Label label) {
        mv.visitJumpInsn(org.osgl.mvc.server.asm.Opcodes.IF_ICMPLE, label);
    }

    public void ifacmpeq(final org.osgl.mvc.server.asm.Label label) {
        mv.visitJumpInsn(org.osgl.mvc.server.asm.Opcodes.IF_ACMPEQ, label);
    }

    public void ifacmpne(final org.osgl.mvc.server.asm.Label label) {
        mv.visitJumpInsn(org.osgl.mvc.server.asm.Opcodes.IF_ACMPNE, label);
    }

    public void goTo(final org.osgl.mvc.server.asm.Label label) {
        mv.visitJumpInsn(org.osgl.mvc.server.asm.Opcodes.GOTO, label);
    }

    public void jsr(final org.osgl.mvc.server.asm.Label label) {
        mv.visitJumpInsn(org.osgl.mvc.server.asm.Opcodes.JSR, label);
    }

    public void ret(final int var) {
        mv.visitVarInsn(org.osgl.mvc.server.asm.Opcodes.RET, var);
    }

    public void tableswitch(final int min, final int max, final org.osgl.mvc.server.asm.Label dflt,
            final org.osgl.mvc.server.asm.Label... labels) {
        mv.visitTableSwitchInsn(min, max, dflt, labels);
    }

    public void lookupswitch(final org.osgl.mvc.server.asm.Label dflt, final int[] keys,
            final org.osgl.mvc.server.asm.Label[] labels) {
        mv.visitLookupSwitchInsn(dflt, keys, labels);
    }

    public void areturn(final org.osgl.mvc.server.asm.Type t) {
        mv.visitInsn(t.getOpcode(org.osgl.mvc.server.asm.Opcodes.IRETURN));
    }

    public void getstatic(final String owner, final String name,
            final String desc) {
        mv.visitFieldInsn(org.osgl.mvc.server.asm.Opcodes.GETSTATIC, owner, name, desc);
    }

    public void putstatic(final String owner, final String name,
            final String desc) {
        mv.visitFieldInsn(org.osgl.mvc.server.asm.Opcodes.PUTSTATIC, owner, name, desc);
    }

    public void getfield(final String owner, final String name,
            final String desc) {
        mv.visitFieldInsn(org.osgl.mvc.server.asm.Opcodes.GETFIELD, owner, name, desc);
    }

    public void putfield(final String owner, final String name,
            final String desc) {
        mv.visitFieldInsn(org.osgl.mvc.server.asm.Opcodes.PUTFIELD, owner, name, desc);
    }

    @Deprecated
    public void invokevirtual(final String owner, final String name,
            final String desc) {
        if (api >= org.osgl.mvc.server.asm.Opcodes.ASM5) {
            invokevirtual(owner, name, desc, false);
            return;
        }
        mv.visitMethodInsn(org.osgl.mvc.server.asm.Opcodes.INVOKEVIRTUAL, owner, name, desc);
    }

    public void invokevirtual(final String owner, final String name,
            final String desc, final boolean itf) {
        if (api < org.osgl.mvc.server.asm.Opcodes.ASM5) {
            if (itf) {
                throw new IllegalArgumentException(
                        "INVOKEVIRTUAL on interfaces require ASM 5");
            }
            invokevirtual(owner, name, desc);
            return;
        }
        mv.visitMethodInsn(org.osgl.mvc.server.asm.Opcodes.INVOKEVIRTUAL, owner, name, desc, itf);
    }

    @Deprecated
    public void invokespecial(final String owner, final String name,
            final String desc) {
        if (api >= org.osgl.mvc.server.asm.Opcodes.ASM5) {
            invokespecial(owner, name, desc, false);
            return;
        }
        mv.visitMethodInsn(org.osgl.mvc.server.asm.Opcodes.INVOKESPECIAL, owner, name, desc, false);
    }

    public void invokespecial(final String owner, final String name,
            final String desc, final boolean itf) {
        if (api < org.osgl.mvc.server.asm.Opcodes.ASM5) {
            if (itf) {
                throw new IllegalArgumentException(
                        "INVOKESPECIAL on interfaces require ASM 5");
            }
            invokespecial(owner, name, desc);
            return;
        }
        mv.visitMethodInsn(org.osgl.mvc.server.asm.Opcodes.INVOKESPECIAL, owner, name, desc, itf);
    }

    @Deprecated
    public void invokestatic(final String owner, final String name,
            final String desc) {
        if (api >= org.osgl.mvc.server.asm.Opcodes.ASM5) {
            invokestatic(owner, name, desc, false);
            return;
        }
        mv.visitMethodInsn(org.osgl.mvc.server.asm.Opcodes.INVOKESTATIC, owner, name, desc, false);
    }

    public void invokestatic(final String owner, final String name,
            final String desc, final boolean itf) {
        if (api < org.osgl.mvc.server.asm.Opcodes.ASM5) {
            if (itf) {
                throw new IllegalArgumentException(
                        "INVOKESTATIC on interfaces require ASM 5");
            }
            invokestatic(owner, name, desc);
            return;
        }
        mv.visitMethodInsn(org.osgl.mvc.server.asm.Opcodes.INVOKESTATIC, owner, name, desc, itf);
    }

    public void invokeinterface(final String owner, final String name,
            final String desc) {
        mv.visitMethodInsn(org.osgl.mvc.server.asm.Opcodes.INVOKEINTERFACE, owner, name, desc, true);
    }

    public void invokedynamic(String name, String desc, org.osgl.mvc.server.asm.Handle bsm,
            Object[] bsmArgs) {
        mv.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
    }

    public void anew(final org.osgl.mvc.server.asm.Type type) {
        mv.visitTypeInsn(org.osgl.mvc.server.asm.Opcodes.NEW, type.getInternalName());
    }

    public void newarray(final org.osgl.mvc.server.asm.Type type) {
        int typ;
        switch (type.getSort()) {
        case org.osgl.mvc.server.asm.Type.BOOLEAN:
            typ = org.osgl.mvc.server.asm.Opcodes.T_BOOLEAN;
            break;
        case org.osgl.mvc.server.asm.Type.CHAR:
            typ = org.osgl.mvc.server.asm.Opcodes.T_CHAR;
            break;
        case org.osgl.mvc.server.asm.Type.BYTE:
            typ = org.osgl.mvc.server.asm.Opcodes.T_BYTE;
            break;
        case org.osgl.mvc.server.asm.Type.SHORT:
            typ = org.osgl.mvc.server.asm.Opcodes.T_SHORT;
            break;
        case org.osgl.mvc.server.asm.Type.INT:
            typ = org.osgl.mvc.server.asm.Opcodes.T_INT;
            break;
        case org.osgl.mvc.server.asm.Type.FLOAT:
            typ = org.osgl.mvc.server.asm.Opcodes.T_FLOAT;
            break;
        case org.osgl.mvc.server.asm.Type.LONG:
            typ = org.osgl.mvc.server.asm.Opcodes.T_LONG;
            break;
        case org.osgl.mvc.server.asm.Type.DOUBLE:
            typ = org.osgl.mvc.server.asm.Opcodes.T_DOUBLE;
            break;
        default:
            mv.visitTypeInsn(org.osgl.mvc.server.asm.Opcodes.ANEWARRAY, type.getInternalName());
            return;
        }
        mv.visitIntInsn(org.osgl.mvc.server.asm.Opcodes.NEWARRAY, typ);
    }

    public void arraylength() {
        mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.ARRAYLENGTH);
    }

    public void athrow() {
        mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.ATHROW);
    }

    public void checkcast(final org.osgl.mvc.server.asm.Type type) {
        mv.visitTypeInsn(org.osgl.mvc.server.asm.Opcodes.CHECKCAST, type.getInternalName());
    }

    public void instanceOf(final org.osgl.mvc.server.asm.Type type) {
        mv.visitTypeInsn(org.osgl.mvc.server.asm.Opcodes.INSTANCEOF, type.getInternalName());
    }

    public void monitorenter() {
        mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.MONITORENTER);
    }

    public void monitorexit() {
        mv.visitInsn(org.osgl.mvc.server.asm.Opcodes.MONITOREXIT);
    }

    public void multianewarray(final String desc, final int dims) {
        mv.visitMultiANewArrayInsn(desc, dims);
    }

    public void ifnull(final org.osgl.mvc.server.asm.Label label) {
        mv.visitJumpInsn(org.osgl.mvc.server.asm.Opcodes.IFNULL, label);
    }

    public void ifnonnull(final org.osgl.mvc.server.asm.Label label) {
        mv.visitJumpInsn(org.osgl.mvc.server.asm.Opcodes.IFNONNULL, label);
    }

    public void mark(final org.osgl.mvc.server.asm.Label label) {
        mv.visitLabel(label);
    }
}
