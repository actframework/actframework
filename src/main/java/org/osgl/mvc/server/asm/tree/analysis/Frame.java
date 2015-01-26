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

import java.util.ArrayList;
import java.util.List;

/**
 * A symbolic execution stack frame. A stack frame contains a set of local
 * variable slots, and an operand stack. Warning: long and double values are
 * represented by <i>two</i> slots in local variables, and by <i>one</i> slot in
 * the operand stack.
 * 
 * @param <V>
 *            type of the Value used for the analysis.
 * 
 * @author Eric Bruneton
 */
public class Frame<V extends Value> {

    /**
     * The expected return type of the analyzed method, or <tt>null</tt> if the
     * method returns void.
     */
    private V returnValue;

    /**
     * The local variables and operand stack of this frame.
     */
    private V[] values;

    /**
     * The number of local variables of this frame.
     */
    private int locals;

    /**
     * The number of elements in the operand stack.
     */
    private int top;

    /**
     * Constructs a new frame with the given size.
     * 
     * @param nLocals
     *            the maximum number of local variables of the frame.
     * @param nStack
     *            the maximum stack size of the frame.
     */
    public Frame(final int nLocals, final int nStack) {
        this.values = (V[]) new Value[nLocals + nStack];
        this.locals = nLocals;
    }

    /**
     * Constructs a new frame that is identical to the given frame.
     * 
     * @param src
     *            a frame.
     */
    public Frame(final Frame<? extends V> src) {
        this(src.locals, src.values.length - src.locals);
        init(src);
    }

    /**
     * Copies the state of the given frame into this frame.
     * 
     * @param src
     *            a frame.
     * @return this frame.
     */
    public Frame<V> init(final Frame<? extends V> src) {
        returnValue = src.returnValue;
        System.arraycopy(src.values, 0, values, 0, values.length);
        top = src.top;
        return this;
    }

    /**
     * Sets the expected return type of the analyzed method.
     * 
     * @param v
     *            the expected return type of the analyzed method, or
     *            <tt>null</tt> if the method returns void.
     */
    public void setReturn(final V v) {
        returnValue = v;
    }

    /**
     * Returns the maximum number of local variables of this frame.
     * 
     * @return the maximum number of local variables of this frame.
     */
    public int getLocals() {
        return locals;
    }

    /**
     * Returns the maximum stack size of this frame.
     * 
     * @return the maximum stack size of this frame.
     */
    public int getMaxStackSize() {
        return values.length - locals;
    }
    
    /**
     * Returns the value of the given local variable.
     * 
     * @param i
     *            a local variable index.
     * @return the value of the given local variable.
     * @throws IndexOutOfBoundsException
     *             if the variable does not exist.
     */
    public V getLocal(final int i) throws IndexOutOfBoundsException {
        if (i >= locals) {
            throw new IndexOutOfBoundsException(
                    "Trying to access an inexistant local variable");
        }
        return values[i];
    }

    /**
     * Sets the value of the given local variable.
     * 
     * @param i
     *            a local variable index.
     * @param value
     *            the new value of this local variable.
     * @throws IndexOutOfBoundsException
     *             if the variable does not exist.
     */
    public void setLocal(final int i, final V value)
            throws IndexOutOfBoundsException {
        if (i >= locals) {
            throw new IndexOutOfBoundsException(
                    "Trying to access an inexistant local variable " + i);
        }
        values[i] = value;
    }

    /**
     * Returns the number of values in the operand stack of this frame. Long and
     * double values are treated as single values.
     * 
     * @return the number of values in the operand stack of this frame.
     */
    public int getStackSize() {
        return top;
    }

    /**
     * Returns the value of the given operand stack slot.
     * 
     * @param i
     *            the index of an operand stack slot.
     * @return the value of the given operand stack slot.
     * @throws IndexOutOfBoundsException
     *             if the operand stack slot does not exist.
     */
    public V getStack(final int i) throws IndexOutOfBoundsException {
        return values[i + locals];
    }

    /**
     * Clears the operand stack of this frame.
     */
    public void clearStack() {
        top = 0;
    }

    /**
     * Pops a value from the operand stack of this frame.
     * 
     * @return the value that has been popped from the stack.
     * @throws IndexOutOfBoundsException
     *             if the operand stack is empty.
     */
    public V pop() throws IndexOutOfBoundsException {
        if (top == 0) {
            throw new IndexOutOfBoundsException(
                    "Cannot pop operand off an empty stack.");
        }
        return values[--top + locals];
    }

    /**
     * Pushes a value into the operand stack of this frame.
     * 
     * @param value
     *            the value that must be pushed into the stack.
     * @throws IndexOutOfBoundsException
     *             if the operand stack is full.
     */
    public void push(final V value) throws IndexOutOfBoundsException {
        if (top + locals >= values.length) {
            throw new IndexOutOfBoundsException(
                    "Insufficient maximum stack size.");
        }
        values[top++ + locals] = value;
    }

    public void execute(final org.osgl.mvc.server.asm.tree.AbstractInsnNode insn,
            final org.osgl.mvc.server.asm.tree.analysis.Interpreter<V> interpreter) throws AnalyzerException {
        V value1, value2, value3, value4;
        List<V> values;
        int var;

        switch (insn.getOpcode()) {
        case org.osgl.mvc.server.asm.Opcodes.NOP:
            break;
        case org.osgl.mvc.server.asm.Opcodes.ACONST_NULL:
        case org.osgl.mvc.server.asm.Opcodes.ICONST_M1:
        case org.osgl.mvc.server.asm.Opcodes.ICONST_0:
        case org.osgl.mvc.server.asm.Opcodes.ICONST_1:
        case org.osgl.mvc.server.asm.Opcodes.ICONST_2:
        case org.osgl.mvc.server.asm.Opcodes.ICONST_3:
        case org.osgl.mvc.server.asm.Opcodes.ICONST_4:
        case org.osgl.mvc.server.asm.Opcodes.ICONST_5:
        case org.osgl.mvc.server.asm.Opcodes.LCONST_0:
        case org.osgl.mvc.server.asm.Opcodes.LCONST_1:
        case org.osgl.mvc.server.asm.Opcodes.FCONST_0:
        case org.osgl.mvc.server.asm.Opcodes.FCONST_1:
        case org.osgl.mvc.server.asm.Opcodes.FCONST_2:
        case org.osgl.mvc.server.asm.Opcodes.DCONST_0:
        case org.osgl.mvc.server.asm.Opcodes.DCONST_1:
        case org.osgl.mvc.server.asm.Opcodes.BIPUSH:
        case org.osgl.mvc.server.asm.Opcodes.SIPUSH:
        case org.osgl.mvc.server.asm.Opcodes.LDC:
            push(interpreter.newOperation(insn));
            break;
        case org.osgl.mvc.server.asm.Opcodes.ILOAD:
        case org.osgl.mvc.server.asm.Opcodes.LLOAD:
        case org.osgl.mvc.server.asm.Opcodes.FLOAD:
        case org.osgl.mvc.server.asm.Opcodes.DLOAD:
        case org.osgl.mvc.server.asm.Opcodes.ALOAD:
            push(interpreter.copyOperation(insn,
                    getLocal(((org.osgl.mvc.server.asm.tree.VarInsnNode) insn).var)));
            break;
        case org.osgl.mvc.server.asm.Opcodes.IALOAD:
        case org.osgl.mvc.server.asm.Opcodes.LALOAD:
        case org.osgl.mvc.server.asm.Opcodes.FALOAD:
        case org.osgl.mvc.server.asm.Opcodes.DALOAD:
        case org.osgl.mvc.server.asm.Opcodes.AALOAD:
        case org.osgl.mvc.server.asm.Opcodes.BALOAD:
        case org.osgl.mvc.server.asm.Opcodes.CALOAD:
        case org.osgl.mvc.server.asm.Opcodes.SALOAD:
            value2 = pop();
            value1 = pop();
            push(interpreter.binaryOperation(insn, value1, value2));
            break;
        case org.osgl.mvc.server.asm.Opcodes.ISTORE:
        case org.osgl.mvc.server.asm.Opcodes.LSTORE:
        case org.osgl.mvc.server.asm.Opcodes.FSTORE:
        case org.osgl.mvc.server.asm.Opcodes.DSTORE:
        case org.osgl.mvc.server.asm.Opcodes.ASTORE:
            value1 = interpreter.copyOperation(insn, pop());
            var = ((org.osgl.mvc.server.asm.tree.VarInsnNode) insn).var;
            setLocal(var, value1);
            if (value1.getSize() == 2) {
                setLocal(var + 1, interpreter.newValue(null));
            }
            if (var > 0) {
                Value local = getLocal(var - 1);
                if (local != null && local.getSize() == 2) {
                    setLocal(var - 1, interpreter.newValue(null));
                }
            }
            break;
        case org.osgl.mvc.server.asm.Opcodes.IASTORE:
        case org.osgl.mvc.server.asm.Opcodes.LASTORE:
        case org.osgl.mvc.server.asm.Opcodes.FASTORE:
        case org.osgl.mvc.server.asm.Opcodes.DASTORE:
        case org.osgl.mvc.server.asm.Opcodes.AASTORE:
        case org.osgl.mvc.server.asm.Opcodes.BASTORE:
        case org.osgl.mvc.server.asm.Opcodes.CASTORE:
        case org.osgl.mvc.server.asm.Opcodes.SASTORE:
            value3 = pop();
            value2 = pop();
            value1 = pop();
            interpreter.ternaryOperation(insn, value1, value2, value3);
            break;
        case org.osgl.mvc.server.asm.Opcodes.POP:
            if (pop().getSize() == 2) {
                throw new AnalyzerException(insn, "Illegal use of POP");
            }
            break;
        case org.osgl.mvc.server.asm.Opcodes.POP2:
            if (pop().getSize() == 1) {
                if (pop().getSize() != 1) {
                    throw new AnalyzerException(insn, "Illegal use of POP2");
                }
            }
            break;
        case org.osgl.mvc.server.asm.Opcodes.DUP:
            value1 = pop();
            if (value1.getSize() != 1) {
                throw new AnalyzerException(insn, "Illegal use of DUP");
            }
            push(value1);
            push(interpreter.copyOperation(insn, value1));
            break;
        case org.osgl.mvc.server.asm.Opcodes.DUP_X1:
            value1 = pop();
            value2 = pop();
            if (value1.getSize() != 1 || value2.getSize() != 1) {
                throw new AnalyzerException(insn, "Illegal use of DUP_X1");
            }
            push(interpreter.copyOperation(insn, value1));
            push(value2);
            push(value1);
            break;
        case org.osgl.mvc.server.asm.Opcodes.DUP_X2:
            value1 = pop();
            if (value1.getSize() == 1) {
                value2 = pop();
                if (value2.getSize() == 1) {
                    value3 = pop();
                    if (value3.getSize() == 1) {
                        push(interpreter.copyOperation(insn, value1));
                        push(value3);
                        push(value2);
                        push(value1);
                        break;
                    }
                } else {
                    push(interpreter.copyOperation(insn, value1));
                    push(value2);
                    push(value1);
                    break;
                }
            }
            throw new AnalyzerException(insn, "Illegal use of DUP_X2");
        case org.osgl.mvc.server.asm.Opcodes.DUP2:
            value1 = pop();
            if (value1.getSize() == 1) {
                value2 = pop();
                if (value2.getSize() == 1) {
                    push(value2);
                    push(value1);
                    push(interpreter.copyOperation(insn, value2));
                    push(interpreter.copyOperation(insn, value1));
                    break;
                }
            } else {
                push(value1);
                push(interpreter.copyOperation(insn, value1));
                break;
            }
            throw new AnalyzerException(insn, "Illegal use of DUP2");
        case org.osgl.mvc.server.asm.Opcodes.DUP2_X1:
            value1 = pop();
            if (value1.getSize() == 1) {
                value2 = pop();
                if (value2.getSize() == 1) {
                    value3 = pop();
                    if (value3.getSize() == 1) {
                        push(interpreter.copyOperation(insn, value2));
                        push(interpreter.copyOperation(insn, value1));
                        push(value3);
                        push(value2);
                        push(value1);
                        break;
                    }
                }
            } else {
                value2 = pop();
                if (value2.getSize() == 1) {
                    push(interpreter.copyOperation(insn, value1));
                    push(value2);
                    push(value1);
                    break;
                }
            }
            throw new AnalyzerException(insn, "Illegal use of DUP2_X1");
        case org.osgl.mvc.server.asm.Opcodes.DUP2_X2:
            value1 = pop();
            if (value1.getSize() == 1) {
                value2 = pop();
                if (value2.getSize() == 1) {
                    value3 = pop();
                    if (value3.getSize() == 1) {
                        value4 = pop();
                        if (value4.getSize() == 1) {
                            push(interpreter.copyOperation(insn, value2));
                            push(interpreter.copyOperation(insn, value1));
                            push(value4);
                            push(value3);
                            push(value2);
                            push(value1);
                            break;
                        }
                    } else {
                        push(interpreter.copyOperation(insn, value2));
                        push(interpreter.copyOperation(insn, value1));
                        push(value3);
                        push(value2);
                        push(value1);
                        break;
                    }
                }
            } else {
                value2 = pop();
                if (value2.getSize() == 1) {
                    value3 = pop();
                    if (value3.getSize() == 1) {
                        push(interpreter.copyOperation(insn, value1));
                        push(value3);
                        push(value2);
                        push(value1);
                        break;
                    }
                } else {
                    push(interpreter.copyOperation(insn, value1));
                    push(value2);
                    push(value1);
                    break;
                }
            }
            throw new AnalyzerException(insn, "Illegal use of DUP2_X2");
        case org.osgl.mvc.server.asm.Opcodes.SWAP:
            value2 = pop();
            value1 = pop();
            if (value1.getSize() != 1 || value2.getSize() != 1) {
                throw new AnalyzerException(insn, "Illegal use of SWAP");
            }
            push(interpreter.copyOperation(insn, value2));
            push(interpreter.copyOperation(insn, value1));
            break;
        case org.osgl.mvc.server.asm.Opcodes.IADD:
        case org.osgl.mvc.server.asm.Opcodes.LADD:
        case org.osgl.mvc.server.asm.Opcodes.FADD:
        case org.osgl.mvc.server.asm.Opcodes.DADD:
        case org.osgl.mvc.server.asm.Opcodes.ISUB:
        case org.osgl.mvc.server.asm.Opcodes.LSUB:
        case org.osgl.mvc.server.asm.Opcodes.FSUB:
        case org.osgl.mvc.server.asm.Opcodes.DSUB:
        case org.osgl.mvc.server.asm.Opcodes.IMUL:
        case org.osgl.mvc.server.asm.Opcodes.LMUL:
        case org.osgl.mvc.server.asm.Opcodes.FMUL:
        case org.osgl.mvc.server.asm.Opcodes.DMUL:
        case org.osgl.mvc.server.asm.Opcodes.IDIV:
        case org.osgl.mvc.server.asm.Opcodes.LDIV:
        case org.osgl.mvc.server.asm.Opcodes.FDIV:
        case org.osgl.mvc.server.asm.Opcodes.DDIV:
        case org.osgl.mvc.server.asm.Opcodes.IREM:
        case org.osgl.mvc.server.asm.Opcodes.LREM:
        case org.osgl.mvc.server.asm.Opcodes.FREM:
        case org.osgl.mvc.server.asm.Opcodes.DREM:
            value2 = pop();
            value1 = pop();
            push(interpreter.binaryOperation(insn, value1, value2));
            break;
        case org.osgl.mvc.server.asm.Opcodes.INEG:
        case org.osgl.mvc.server.asm.Opcodes.LNEG:
        case org.osgl.mvc.server.asm.Opcodes.FNEG:
        case org.osgl.mvc.server.asm.Opcodes.DNEG:
            push(interpreter.unaryOperation(insn, pop()));
            break;
        case org.osgl.mvc.server.asm.Opcodes.ISHL:
        case org.osgl.mvc.server.asm.Opcodes.LSHL:
        case org.osgl.mvc.server.asm.Opcodes.ISHR:
        case org.osgl.mvc.server.asm.Opcodes.LSHR:
        case org.osgl.mvc.server.asm.Opcodes.IUSHR:
        case org.osgl.mvc.server.asm.Opcodes.LUSHR:
        case org.osgl.mvc.server.asm.Opcodes.IAND:
        case org.osgl.mvc.server.asm.Opcodes.LAND:
        case org.osgl.mvc.server.asm.Opcodes.IOR:
        case org.osgl.mvc.server.asm.Opcodes.LOR:
        case org.osgl.mvc.server.asm.Opcodes.IXOR:
        case org.osgl.mvc.server.asm.Opcodes.LXOR:
            value2 = pop();
            value1 = pop();
            push(interpreter.binaryOperation(insn, value1, value2));
            break;
        case org.osgl.mvc.server.asm.Opcodes.IINC:
            var = ((org.osgl.mvc.server.asm.tree.IincInsnNode) insn).var;
            setLocal(var, interpreter.unaryOperation(insn, getLocal(var)));
            break;
        case org.osgl.mvc.server.asm.Opcodes.I2L:
        case org.osgl.mvc.server.asm.Opcodes.I2F:
        case org.osgl.mvc.server.asm.Opcodes.I2D:
        case org.osgl.mvc.server.asm.Opcodes.L2I:
        case org.osgl.mvc.server.asm.Opcodes.L2F:
        case org.osgl.mvc.server.asm.Opcodes.L2D:
        case org.osgl.mvc.server.asm.Opcodes.F2I:
        case org.osgl.mvc.server.asm.Opcodes.F2L:
        case org.osgl.mvc.server.asm.Opcodes.F2D:
        case org.osgl.mvc.server.asm.Opcodes.D2I:
        case org.osgl.mvc.server.asm.Opcodes.D2L:
        case org.osgl.mvc.server.asm.Opcodes.D2F:
        case org.osgl.mvc.server.asm.Opcodes.I2B:
        case org.osgl.mvc.server.asm.Opcodes.I2C:
        case org.osgl.mvc.server.asm.Opcodes.I2S:
            push(interpreter.unaryOperation(insn, pop()));
            break;
        case org.osgl.mvc.server.asm.Opcodes.LCMP:
        case org.osgl.mvc.server.asm.Opcodes.FCMPL:
        case org.osgl.mvc.server.asm.Opcodes.FCMPG:
        case org.osgl.mvc.server.asm.Opcodes.DCMPL:
        case org.osgl.mvc.server.asm.Opcodes.DCMPG:
            value2 = pop();
            value1 = pop();
            push(interpreter.binaryOperation(insn, value1, value2));
            break;
        case org.osgl.mvc.server.asm.Opcodes.IFEQ:
        case org.osgl.mvc.server.asm.Opcodes.IFNE:
        case org.osgl.mvc.server.asm.Opcodes.IFLT:
        case org.osgl.mvc.server.asm.Opcodes.IFGE:
        case org.osgl.mvc.server.asm.Opcodes.IFGT:
        case org.osgl.mvc.server.asm.Opcodes.IFLE:
            interpreter.unaryOperation(insn, pop());
            break;
        case org.osgl.mvc.server.asm.Opcodes.IF_ICMPEQ:
        case org.osgl.mvc.server.asm.Opcodes.IF_ICMPNE:
        case org.osgl.mvc.server.asm.Opcodes.IF_ICMPLT:
        case org.osgl.mvc.server.asm.Opcodes.IF_ICMPGE:
        case org.osgl.mvc.server.asm.Opcodes.IF_ICMPGT:
        case org.osgl.mvc.server.asm.Opcodes.IF_ICMPLE:
        case org.osgl.mvc.server.asm.Opcodes.IF_ACMPEQ:
        case org.osgl.mvc.server.asm.Opcodes.IF_ACMPNE:
            value2 = pop();
            value1 = pop();
            interpreter.binaryOperation(insn, value1, value2);
            break;
        case org.osgl.mvc.server.asm.Opcodes.GOTO:
            break;
        case org.osgl.mvc.server.asm.Opcodes.JSR:
            push(interpreter.newOperation(insn));
            break;
        case org.osgl.mvc.server.asm.Opcodes.RET:
            break;
        case org.osgl.mvc.server.asm.Opcodes.TABLESWITCH:
        case org.osgl.mvc.server.asm.Opcodes.LOOKUPSWITCH:
            interpreter.unaryOperation(insn, pop());
            break;
        case org.osgl.mvc.server.asm.Opcodes.IRETURN:
        case org.osgl.mvc.server.asm.Opcodes.LRETURN:
        case org.osgl.mvc.server.asm.Opcodes.FRETURN:
        case org.osgl.mvc.server.asm.Opcodes.DRETURN:
        case org.osgl.mvc.server.asm.Opcodes.ARETURN:
            value1 = pop();
            interpreter.unaryOperation(insn, value1);
            interpreter.returnOperation(insn, value1, returnValue);
            break;
        case org.osgl.mvc.server.asm.Opcodes.RETURN:
            if (returnValue != null) {
                throw new AnalyzerException(insn, "Incompatible return type");
            }
            break;
        case org.osgl.mvc.server.asm.Opcodes.GETSTATIC:
            push(interpreter.newOperation(insn));
            break;
        case org.osgl.mvc.server.asm.Opcodes.PUTSTATIC:
            interpreter.unaryOperation(insn, pop());
            break;
        case org.osgl.mvc.server.asm.Opcodes.GETFIELD:
            push(interpreter.unaryOperation(insn, pop()));
            break;
        case org.osgl.mvc.server.asm.Opcodes.PUTFIELD:
            value2 = pop();
            value1 = pop();
            interpreter.binaryOperation(insn, value1, value2);
            break;
        case org.osgl.mvc.server.asm.Opcodes.INVOKEVIRTUAL:
        case org.osgl.mvc.server.asm.Opcodes.INVOKESPECIAL:
        case org.osgl.mvc.server.asm.Opcodes.INVOKESTATIC:
        case org.osgl.mvc.server.asm.Opcodes.INVOKEINTERFACE: {
            values = new ArrayList<V>();
            String desc = ((org.osgl.mvc.server.asm.tree.MethodInsnNode) insn).desc;
            for (int i = org.osgl.mvc.server.asm.Type.getArgumentTypes(desc).length; i > 0; --i) {
                values.add(0, pop());
            }
            if (insn.getOpcode() != org.osgl.mvc.server.asm.Opcodes.INVOKESTATIC) {
                values.add(0, pop());
            }
            if (org.osgl.mvc.server.asm.Type.getReturnType(desc) == org.osgl.mvc.server.asm.Type.VOID_TYPE) {
                interpreter.naryOperation(insn, values);
            } else {
                push(interpreter.naryOperation(insn, values));
            }
            break;
        }
        case org.osgl.mvc.server.asm.Opcodes.INVOKEDYNAMIC: {
            values = new ArrayList<V>();
            String desc = ((org.osgl.mvc.server.asm.tree.InvokeDynamicInsnNode) insn).desc;
            for (int i = org.osgl.mvc.server.asm.Type.getArgumentTypes(desc).length; i > 0; --i) {
                values.add(0, pop());
            }
            if (org.osgl.mvc.server.asm.Type.getReturnType(desc) == org.osgl.mvc.server.asm.Type.VOID_TYPE) {
                interpreter.naryOperation(insn, values);
            } else {
                push(interpreter.naryOperation(insn, values));
            }
            break;
        }
        case org.osgl.mvc.server.asm.Opcodes.NEW:
            push(interpreter.newOperation(insn));
            break;
        case org.osgl.mvc.server.asm.Opcodes.NEWARRAY:
        case org.osgl.mvc.server.asm.Opcodes.ANEWARRAY:
        case org.osgl.mvc.server.asm.Opcodes.ARRAYLENGTH:
            push(interpreter.unaryOperation(insn, pop()));
            break;
        case org.osgl.mvc.server.asm.Opcodes.ATHROW:
            interpreter.unaryOperation(insn, pop());
            break;
        case org.osgl.mvc.server.asm.Opcodes.CHECKCAST:
        case org.osgl.mvc.server.asm.Opcodes.INSTANCEOF:
            push(interpreter.unaryOperation(insn, pop()));
            break;
        case org.osgl.mvc.server.asm.Opcodes.MONITORENTER:
        case org.osgl.mvc.server.asm.Opcodes.MONITOREXIT:
            interpreter.unaryOperation(insn, pop());
            break;
        case org.osgl.mvc.server.asm.Opcodes.MULTIANEWARRAY:
            values = new ArrayList<V>();
            for (int i = ((org.osgl.mvc.server.asm.tree.MultiANewArrayInsnNode) insn).dims; i > 0; --i) {
                values.add(0, pop());
            }
            push(interpreter.naryOperation(insn, values));
            break;
        case org.osgl.mvc.server.asm.Opcodes.IFNULL:
        case org.osgl.mvc.server.asm.Opcodes.IFNONNULL:
            interpreter.unaryOperation(insn, pop());
            break;
        default:
            throw new RuntimeException("Illegal opcode " + insn.getOpcode());
        }
    }

    /**
     * Merges this frame with the given frame.
     *
     * @param frame
     *            a frame.
     * @param interpreter
     *            the interpreter used to merge values.
     * @return <tt>true</tt> if this frame has been changed as a result of the
     *         merge operation, or <tt>false</tt> otherwise.
     * @throws AnalyzerException
     *             if the frames have incompatible sizes.
     */
    public boolean merge(final Frame<? extends V> frame,
            final org.osgl.mvc.server.asm.tree.analysis.Interpreter<V> interpreter) throws AnalyzerException {
        if (top != frame.top) {
            throw new AnalyzerException(null, "Incompatible stack heights");
        }
        boolean changes = false;
        for (int i = 0; i < locals + top; ++i) {
            V v = interpreter.merge(values[i], frame.values[i]);
            if (!v.equals(values[i])) {
                values[i] = v;
                changes = true;
            }
        }
        return changes;
    }

    /**
     * Merges this frame with the given frame (case of a RET instruction).
     * 
     * @param frame
     *            a frame
     * @param access
     *            the local variables that have been accessed by the subroutine
     *            to which the RET instruction corresponds.
     * @return <tt>true</tt> if this frame has been changed as a result of the
     *         merge operation, or <tt>false</tt> otherwise.
     */
    public boolean merge(final Frame<? extends V> frame, final boolean[] access) {
        boolean changes = false;
        for (int i = 0; i < locals; ++i) {
            if (!access[i] && !values[i].equals(frame.values[i])) {
                values[i] = frame.values[i];
                changes = true;
            }
        }
        return changes;
    }

    /**
     * Returns a string representation of this frame.
     * 
     * @return a string representation of this frame.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < getLocals(); ++i) {
            sb.append(getLocal(i));
        }
        sb.append(' ');
        for (int i = 0; i < getStackSize(); ++i) {
            sb.append(getStack(i).toString());
        }
        return sb.toString();
    }
}
