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
package org.osgl.mvc.server.asm.tree.analysis;

import java.util.List;

/**
 * An {@link org.osgl.mvc.server.asm.tree.analysis.Interpreter} for {@link org.osgl.mvc.server.asm.tree.analysis.BasicValue} values.
 * 
 * @author Eric Bruneton
 * @author Bing Ran
 */
public class BasicInterpreter extends org.osgl.mvc.server.asm.tree.analysis.Interpreter<org.osgl.mvc.server.asm.tree.analysis.BasicValue> implements
        org.osgl.mvc.server.asm.Opcodes {

    public BasicInterpreter() {
        super(ASM5);
    }

    protected BasicInterpreter(final int api) {
        super(api);
    }

    @Override
    public org.osgl.mvc.server.asm.tree.analysis.BasicValue newValue(final org.osgl.mvc.server.asm.Type type) {
        if (type == null) {
            return org.osgl.mvc.server.asm.tree.analysis.BasicValue.UNINITIALIZED_VALUE;
        }
        switch (type.getSort()) {
        case org.osgl.mvc.server.asm.Type.VOID:
            return null;
        case org.osgl.mvc.server.asm.Type.BOOLEAN:
        case org.osgl.mvc.server.asm.Type.CHAR:
        case org.osgl.mvc.server.asm.Type.BYTE:
        case org.osgl.mvc.server.asm.Type.SHORT:
        case org.osgl.mvc.server.asm.Type.INT:
            return org.osgl.mvc.server.asm.tree.analysis.BasicValue.INT_VALUE;
        case org.osgl.mvc.server.asm.Type.FLOAT:
            return org.osgl.mvc.server.asm.tree.analysis.BasicValue.FLOAT_VALUE;
        case org.osgl.mvc.server.asm.Type.LONG:
            return org.osgl.mvc.server.asm.tree.analysis.BasicValue.LONG_VALUE;
        case org.osgl.mvc.server.asm.Type.DOUBLE:
            return org.osgl.mvc.server.asm.tree.analysis.BasicValue.DOUBLE_VALUE;
        case org.osgl.mvc.server.asm.Type.ARRAY:
        case org.osgl.mvc.server.asm.Type.OBJECT:
            return org.osgl.mvc.server.asm.tree.analysis.BasicValue.REFERENCE_VALUE;
        default:
            throw new Error("Internal error");
        }
    }

    @Override
    public org.osgl.mvc.server.asm.tree.analysis.BasicValue newOperation(final org.osgl.mvc.server.asm.tree.AbstractInsnNode insn)
            throws org.osgl.mvc.server.asm.tree.analysis.AnalyzerException {
        switch (insn.getOpcode()) {
        case ACONST_NULL:
            return newValue(org.osgl.mvc.server.asm.Type.getObjectType("null"));
        case ICONST_M1:
        case ICONST_0:
        case ICONST_1:
        case ICONST_2:
        case ICONST_3:
        case ICONST_4:
        case ICONST_5:
            return org.osgl.mvc.server.asm.tree.analysis.BasicValue.INT_VALUE;
        case LCONST_0:
        case LCONST_1:
            return org.osgl.mvc.server.asm.tree.analysis.BasicValue.LONG_VALUE;
        case FCONST_0:
        case FCONST_1:
        case FCONST_2:
            return org.osgl.mvc.server.asm.tree.analysis.BasicValue.FLOAT_VALUE;
        case DCONST_0:
        case DCONST_1:
            return org.osgl.mvc.server.asm.tree.analysis.BasicValue.DOUBLE_VALUE;
        case BIPUSH:
        case SIPUSH:
            return org.osgl.mvc.server.asm.tree.analysis.BasicValue.INT_VALUE;
        case LDC:
            Object cst = ((org.osgl.mvc.server.asm.tree.LdcInsnNode) insn).cst;
            if (cst instanceof Integer) {
                return org.osgl.mvc.server.asm.tree.analysis.BasicValue.INT_VALUE;
            } else if (cst instanceof Float) {
                return org.osgl.mvc.server.asm.tree.analysis.BasicValue.FLOAT_VALUE;
            } else if (cst instanceof Long) {
                return org.osgl.mvc.server.asm.tree.analysis.BasicValue.LONG_VALUE;
            } else if (cst instanceof Double) {
                return org.osgl.mvc.server.asm.tree.analysis.BasicValue.DOUBLE_VALUE;
            } else if (cst instanceof String) {
                return newValue(org.osgl.mvc.server.asm.Type.getObjectType("java/lang/String"));
            } else if (cst instanceof org.osgl.mvc.server.asm.Type) {
                int sort = ((org.osgl.mvc.server.asm.Type) cst).getSort();
                if (sort == org.osgl.mvc.server.asm.Type.OBJECT || sort == org.osgl.mvc.server.asm.Type.ARRAY) {
                    return newValue(org.osgl.mvc.server.asm.Type.getObjectType("java/lang/Class"));
                } else if (sort == org.osgl.mvc.server.asm.Type.METHOD) {
                    return newValue(org.osgl.mvc.server.asm.Type
                            .getObjectType("java/lang/invoke/MethodType"));
                } else {
                    throw new IllegalArgumentException("Illegal LDC constant "
                            + cst);
                }
            } else if (cst instanceof org.osgl.mvc.server.asm.Handle) {
                return newValue(org.osgl.mvc.server.asm.Type
                        .getObjectType("java/lang/invoke/MethodHandle"));
            } else {
                throw new IllegalArgumentException("Illegal LDC constant "
                        + cst);
            }
        case JSR:
            return org.osgl.mvc.server.asm.tree.analysis.BasicValue.RETURNADDRESS_VALUE;
        case GETSTATIC:
            return newValue(org.osgl.mvc.server.asm.Type.getType(((org.osgl.mvc.server.asm.tree.FieldInsnNode) insn).desc));
        case NEW:
            return newValue(org.osgl.mvc.server.asm.Type.getObjectType(((org.osgl.mvc.server.asm.tree.TypeInsnNode) insn).desc));
        default:
            throw new Error("Internal error.");
        }
    }

    @Override
    public org.osgl.mvc.server.asm.tree.analysis.BasicValue copyOperation(final org.osgl.mvc.server.asm.tree.AbstractInsnNode insn,
            final org.osgl.mvc.server.asm.tree.analysis.BasicValue value) throws org.osgl.mvc.server.asm.tree.analysis.AnalyzerException {
        return value;
    }

    @Override
    public org.osgl.mvc.server.asm.tree.analysis.BasicValue unaryOperation(final org.osgl.mvc.server.asm.tree.AbstractInsnNode insn,
            final org.osgl.mvc.server.asm.tree.analysis.BasicValue value) throws org.osgl.mvc.server.asm.tree.analysis.AnalyzerException {
        switch (insn.getOpcode()) {
        case INEG:
        case IINC:
        case L2I:
        case F2I:
        case D2I:
        case I2B:
        case I2C:
        case I2S:
            return org.osgl.mvc.server.asm.tree.analysis.BasicValue.INT_VALUE;
        case FNEG:
        case I2F:
        case L2F:
        case D2F:
            return org.osgl.mvc.server.asm.tree.analysis.BasicValue.FLOAT_VALUE;
        case LNEG:
        case I2L:
        case F2L:
        case D2L:
            return org.osgl.mvc.server.asm.tree.analysis.BasicValue.LONG_VALUE;
        case DNEG:
        case I2D:
        case L2D:
        case F2D:
            return org.osgl.mvc.server.asm.tree.analysis.BasicValue.DOUBLE_VALUE;
        case IFEQ:
        case IFNE:
        case IFLT:
        case IFGE:
        case IFGT:
        case IFLE:
        case TABLESWITCH:
        case LOOKUPSWITCH:
        case IRETURN:
        case LRETURN:
        case FRETURN:
        case DRETURN:
        case ARETURN:
        case PUTSTATIC:
            return null;
        case GETFIELD:
            return newValue(org.osgl.mvc.server.asm.Type.getType(((org.osgl.mvc.server.asm.tree.FieldInsnNode) insn).desc));
        case NEWARRAY:
            switch (((org.osgl.mvc.server.asm.tree.IntInsnNode) insn).operand) {
            case T_BOOLEAN:
                return newValue(org.osgl.mvc.server.asm.Type.getType("[Z"));
            case T_CHAR:
                return newValue(org.osgl.mvc.server.asm.Type.getType("[C"));
            case T_BYTE:
                return newValue(org.osgl.mvc.server.asm.Type.getType("[B"));
            case T_SHORT:
                return newValue(org.osgl.mvc.server.asm.Type.getType("[S"));
            case T_INT:
                return newValue(org.osgl.mvc.server.asm.Type.getType("[I"));
            case T_FLOAT:
                return newValue(org.osgl.mvc.server.asm.Type.getType("[F"));
            case T_DOUBLE:
                return newValue(org.osgl.mvc.server.asm.Type.getType("[D"));
            case T_LONG:
                return newValue(org.osgl.mvc.server.asm.Type.getType("[J"));
            default:
                throw new org.osgl.mvc.server.asm.tree.analysis.AnalyzerException(insn, "Invalid array type");
            }
        case ANEWARRAY:
            String desc = ((org.osgl.mvc.server.asm.tree.TypeInsnNode) insn).desc;
            return newValue(org.osgl.mvc.server.asm.Type.getType("[" + org.osgl.mvc.server.asm.Type.getObjectType(desc)));
        case ARRAYLENGTH:
            return org.osgl.mvc.server.asm.tree.analysis.BasicValue.INT_VALUE;
        case ATHROW:
            return null;
        case CHECKCAST:
            desc = ((org.osgl.mvc.server.asm.tree.TypeInsnNode) insn).desc;
            return newValue(org.osgl.mvc.server.asm.Type.getObjectType(desc));
        case INSTANCEOF:
            return org.osgl.mvc.server.asm.tree.analysis.BasicValue.INT_VALUE;
        case MONITORENTER:
        case MONITOREXIT:
        case IFNULL:
        case IFNONNULL:
            return null;
        default:
            throw new Error("Internal error.");
        }
    }

    @Override
    public org.osgl.mvc.server.asm.tree.analysis.BasicValue binaryOperation(final org.osgl.mvc.server.asm.tree.AbstractInsnNode insn,
            final org.osgl.mvc.server.asm.tree.analysis.BasicValue value1, final org.osgl.mvc.server.asm.tree.analysis.BasicValue value2)
            throws org.osgl.mvc.server.asm.tree.analysis.AnalyzerException {
        switch (insn.getOpcode()) {
        case IALOAD:
        case BALOAD:
        case CALOAD:
        case SALOAD:
        case IADD:
        case ISUB:
        case IMUL:
        case IDIV:
        case IREM:
        case ISHL:
        case ISHR:
        case IUSHR:
        case IAND:
        case IOR:
        case IXOR:
            return org.osgl.mvc.server.asm.tree.analysis.BasicValue.INT_VALUE;
        case FALOAD:
        case FADD:
        case FSUB:
        case FMUL:
        case FDIV:
        case FREM:
            return org.osgl.mvc.server.asm.tree.analysis.BasicValue.FLOAT_VALUE;
        case LALOAD:
        case LADD:
        case LSUB:
        case LMUL:
        case LDIV:
        case LREM:
        case LSHL:
        case LSHR:
        case LUSHR:
        case LAND:
        case LOR:
        case LXOR:
            return org.osgl.mvc.server.asm.tree.analysis.BasicValue.LONG_VALUE;
        case DALOAD:
        case DADD:
        case DSUB:
        case DMUL:
        case DDIV:
        case DREM:
            return org.osgl.mvc.server.asm.tree.analysis.BasicValue.DOUBLE_VALUE;
        case AALOAD:
            return org.osgl.mvc.server.asm.tree.analysis.BasicValue.REFERENCE_VALUE;
        case LCMP:
        case FCMPL:
        case FCMPG:
        case DCMPL:
        case DCMPG:
            return org.osgl.mvc.server.asm.tree.analysis.BasicValue.INT_VALUE;
        case IF_ICMPEQ:
        case IF_ICMPNE:
        case IF_ICMPLT:
        case IF_ICMPGE:
        case IF_ICMPGT:
        case IF_ICMPLE:
        case IF_ACMPEQ:
        case IF_ACMPNE:
        case PUTFIELD:
            return null;
        default:
            throw new Error("Internal error.");
        }
    }

    @Override
    public org.osgl.mvc.server.asm.tree.analysis.BasicValue ternaryOperation(final org.osgl.mvc.server.asm.tree.AbstractInsnNode insn,
            final org.osgl.mvc.server.asm.tree.analysis.BasicValue value1, final org.osgl.mvc.server.asm.tree.analysis.BasicValue value2,
            final org.osgl.mvc.server.asm.tree.analysis.BasicValue value3) throws org.osgl.mvc.server.asm.tree.analysis.AnalyzerException {
        return null;
    }

    @Override
    public org.osgl.mvc.server.asm.tree.analysis.BasicValue naryOperation(final org.osgl.mvc.server.asm.tree.AbstractInsnNode insn,
            final List<? extends org.osgl.mvc.server.asm.tree.analysis.BasicValue> values) throws org.osgl.mvc.server.asm.tree.analysis.AnalyzerException {
        int opcode = insn.getOpcode();
        if (opcode == MULTIANEWARRAY) {
            return newValue(org.osgl.mvc.server.asm.Type.getType(((org.osgl.mvc.server.asm.tree.MultiANewArrayInsnNode) insn).desc));
        } else if (opcode == INVOKEDYNAMIC) {
            return newValue(org.osgl.mvc.server.asm.Type
                    .getReturnType(((org.osgl.mvc.server.asm.tree.InvokeDynamicInsnNode) insn).desc));
        } else {
            return newValue(org.osgl.mvc.server.asm.Type.getReturnType(((org.osgl.mvc.server.asm.tree.MethodInsnNode) insn).desc));
        }
    }

    @Override
    public void returnOperation(final org.osgl.mvc.server.asm.tree.AbstractInsnNode insn,
            final org.osgl.mvc.server.asm.tree.analysis.BasicValue value, final org.osgl.mvc.server.asm.tree.analysis.BasicValue expected)
            throws org.osgl.mvc.server.asm.tree.analysis.AnalyzerException {
    }

    @Override
    public org.osgl.mvc.server.asm.tree.analysis.BasicValue merge(final org.osgl.mvc.server.asm.tree.analysis.BasicValue v, final org.osgl.mvc.server.asm.tree.analysis.BasicValue w) {
        if (!v.equals(w)) {
            return org.osgl.mvc.server.asm.tree.analysis.BasicValue.UNINITIALIZED_VALUE;
        }
        return v;
    }
}
