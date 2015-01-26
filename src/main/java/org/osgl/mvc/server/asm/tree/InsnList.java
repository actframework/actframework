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
package org.osgl.mvc.server.asm.tree;

import org.osgl.mvc.server.asm.MethodVisitor;

import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * A doubly linked list of {@link org.osgl.mvc.server.asm.tree.AbstractInsnNode} objects. <i>This
 * implementation is not thread safe</i>.
 */
public class InsnList {

    /**
     * The number of instructions in this list.
     */
    private int size;

    /**
     * The first instruction in this list. May be <tt>null</tt>.
     */
    private org.osgl.mvc.server.asm.tree.AbstractInsnNode first;

    /**
     * The last instruction in this list. May be <tt>null</tt>.
     */
    private org.osgl.mvc.server.asm.tree.AbstractInsnNode last;

    /**
     * A cache of the instructions of this list. This cache is used to improve
     * the performance of the {@link #get} method.
     */
    org.osgl.mvc.server.asm.tree.AbstractInsnNode[] cache;

    /**
     * Returns the number of instructions in this list.
     *
     * @return the number of instructions in this list.
     */
    public int size() {
        return size;
    }

    /**
     * Returns the first instruction in this list.
     *
     * @return the first instruction in this list, or <tt>null</tt> if the list
     *         is empty.
     */
    public org.osgl.mvc.server.asm.tree.AbstractInsnNode getFirst() {
        return first;
    }

    /**
     * Returns the last instruction in this list.
     *
     * @return the last instruction in this list, or <tt>null</tt> if the list
     *         is empty.
     */
    public org.osgl.mvc.server.asm.tree.AbstractInsnNode getLast() {
        return last;
    }

    /**
     * Returns the instruction whose index is given. This method builds a cache
     * of the instructions in this list to avoid scanning the whole list each
     * time it is called. Once the cache is built, this method run in constant
     * time. This cache is invalidated by all the methods that modify the list.
     *
     * @param index
     *            the index of the instruction that must be returned.
     * @return the instruction whose index is given.
     * @throws IndexOutOfBoundsException
     *             if (index &lt; 0 || index &gt;= size()).
     */
    public org.osgl.mvc.server.asm.tree.AbstractInsnNode get(final int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException();
        }
        if (cache == null) {
            cache = toArray();
        }
        return cache[index];
    }

    /**
     * Returns <tt>true</tt> if the given instruction belongs to this list. This
     * method always scans the instructions of this list until it finds the
     * given instruction or reaches the end of the list.
     *
     * @param insn
     *            an instruction.
     * @return <tt>true</tt> if the given instruction belongs to this list.
     */
    public boolean contains(final org.osgl.mvc.server.asm.tree.AbstractInsnNode insn) {
        org.osgl.mvc.server.asm.tree.AbstractInsnNode i = first;
        while (i != null && i != insn) {
            i = i.next;
        }
        return i != null;
    }

    /**
     * Returns the index of the given instruction in this list. This method
     * builds a cache of the instruction indexes to avoid scanning the whole
     * list each time it is called. Once the cache is built, this method run in
     * constant time. The cache is invalidated by all the methods that modify
     * the list.
     *
     * @param insn
     *            an instruction <i>of this list</i>.
     * @return the index of the given instruction in this list. <i>The result of
     *         this method is undefined if the given instruction does not belong
     *         to this list</i>. Use {@link #contains contains} to test if an
     *         instruction belongs to an instruction list or not.
     */
    public int indexOf(final org.osgl.mvc.server.asm.tree.AbstractInsnNode insn) {
        if (cache == null) {
            cache = toArray();
        }
        return insn.index;
    }

    /**
     * Makes the given visitor visit all of the instructions in this list.
     *
     * @param mv
     *            the method visitor that must visit the instructions.
     */
    public void accept(final MethodVisitor mv) {
        org.osgl.mvc.server.asm.tree.AbstractInsnNode insn = first;
        while (insn != null) {
            insn.accept(mv);
            insn = insn.next;
        }
    }

    /**
     * Returns an iterator over the instructions in this list.
     *
     * @return an iterator over the instructions in this list.
     */
    public ListIterator<org.osgl.mvc.server.asm.tree.AbstractInsnNode> iterator() {
        return iterator(0);
    }

    /**
     * Returns an iterator over the instructions in this list.
     *
     * @return an iterator over the instructions in this list.
     */
    @SuppressWarnings("unchecked")
    public ListIterator<org.osgl.mvc.server.asm.tree.AbstractInsnNode> iterator(int index) {
        return new InsnListIterator(index);
    }

    /**
     * Returns an array containing all of the instructions in this list.
     *
     * @return an array containing all of the instructions in this list.
     */
    public org.osgl.mvc.server.asm.tree.AbstractInsnNode[] toArray() {
        int i = 0;
        org.osgl.mvc.server.asm.tree.AbstractInsnNode elem = first;
        org.osgl.mvc.server.asm.tree.AbstractInsnNode[] insns = new org.osgl.mvc.server.asm.tree.AbstractInsnNode[size];
        while (elem != null) {
            insns[i] = elem;
            elem.index = i++;
            elem = elem.next;
        }
        return insns;
    }

    /**
     * Replaces an instruction of this list with another instruction.
     *
     * @param location
     *            an instruction <i>of this list</i>.
     * @param insn
     *            another instruction, <i>which must not belong to any
     *            {@link org.osgl.mvc.server.asm.tree.InsnList}</i>.
     */
    public void set(final org.osgl.mvc.server.asm.tree.AbstractInsnNode location, final org.osgl.mvc.server.asm.tree.AbstractInsnNode insn) {
        org.osgl.mvc.server.asm.tree.AbstractInsnNode next = location.next;
        insn.next = next;
        if (next != null) {
            next.prev = insn;
        } else {
            last = insn;
        }
        org.osgl.mvc.server.asm.tree.AbstractInsnNode prev = location.prev;
        insn.prev = prev;
        if (prev != null) {
            prev.next = insn;
        } else {
            first = insn;
        }
        if (cache != null) {
            int index = location.index;
            cache[index] = insn;
            insn.index = index;
        } else {
            insn.index = 0; // insn now belongs to an InsnList
        }
        location.index = -1; // i no longer belongs to an InsnList
        location.prev = null;
        location.next = null;
    }

    /**
     * Adds the given instruction to the end of this list.
     *
     * @param insn
     *            an instruction, <i>which must not belong to any
     *            {@link org.osgl.mvc.server.asm.tree.InsnList}</i>.
     */
    public void add(final org.osgl.mvc.server.asm.tree.AbstractInsnNode insn) {
        ++size;
        if (last == null) {
            first = insn;
            last = insn;
        } else {
            last.next = insn;
            insn.prev = last;
        }
        last = insn;
        cache = null;
        insn.index = 0; // insn now belongs to an InsnList
    }

    /**
     * Adds the given instructions to the end of this list.
     *
     * @param insns
     *            an instruction list, which is cleared during the process. This
     *            list must be different from 'this'.
     */
    public void add(final org.osgl.mvc.server.asm.tree.InsnList insns) {
        if (insns.size == 0) {
            return;
        }
        size += insns.size;
        if (last == null) {
            first = insns.first;
            last = insns.last;
        } else {
            org.osgl.mvc.server.asm.tree.AbstractInsnNode elem = insns.first;
            last.next = elem;
            elem.prev = last;
            last = insns.last;
        }
        cache = null;
        insns.removeAll(false);
    }

    /**
     * Inserts the given instruction at the begining of this list.
     *
     * @param insn
     *            an instruction, <i>which must not belong to any
     *            {@link org.osgl.mvc.server.asm.tree.InsnList}</i>.
     */
    public void insert(final org.osgl.mvc.server.asm.tree.AbstractInsnNode insn) {
        ++size;
        if (first == null) {
            first = insn;
            last = insn;
        } else {
            first.prev = insn;
            insn.next = first;
        }
        first = insn;
        cache = null;
        insn.index = 0; // insn now belongs to an InsnList
    }

    /**
     * Inserts the given instructions at the begining of this list.
     *
     * @param insns
     *            an instruction list, which is cleared during the process. This
     *            list must be different from 'this'.
     */
    public void insert(final org.osgl.mvc.server.asm.tree.InsnList insns) {
        if (insns.size == 0) {
            return;
        }
        size += insns.size;
        if (first == null) {
            first = insns.first;
            last = insns.last;
        } else {
            org.osgl.mvc.server.asm.tree.AbstractInsnNode elem = insns.last;
            first.prev = elem;
            elem.next = first;
            first = insns.first;
        }
        cache = null;
        insns.removeAll(false);
    }

    /**
     * Inserts the given instruction after the specified instruction.
     *
     * @param location
     *            an instruction <i>of this list</i> after which insn must be
     *            inserted.
     * @param insn
     *            the instruction to be inserted, <i>which must not belong to
     *            any {@link org.osgl.mvc.server.asm.tree.InsnList}</i>.
     */
    public void insert(final org.osgl.mvc.server.asm.tree.AbstractInsnNode location,
            final org.osgl.mvc.server.asm.tree.AbstractInsnNode insn) {
        ++size;
        org.osgl.mvc.server.asm.tree.AbstractInsnNode next = location.next;
        if (next == null) {
            last = insn;
        } else {
            next.prev = insn;
        }
        location.next = insn;
        insn.next = next;
        insn.prev = location;
        cache = null;
        insn.index = 0; // insn now belongs to an InsnList
    }

    /**
     * Inserts the given instructions after the specified instruction.
     *
     * @param location
     *            an instruction <i>of this list</i> after which the
     *            instructions must be inserted.
     * @param insns
     *            the instruction list to be inserted, which is cleared during
     *            the process. This list must be different from 'this'.
     */
    public void insert(final org.osgl.mvc.server.asm.tree.AbstractInsnNode location, final org.osgl.mvc.server.asm.tree.InsnList insns) {
        if (insns.size == 0) {
            return;
        }
        size += insns.size;
        org.osgl.mvc.server.asm.tree.AbstractInsnNode ifirst = insns.first;
        org.osgl.mvc.server.asm.tree.AbstractInsnNode ilast = insns.last;
        org.osgl.mvc.server.asm.tree.AbstractInsnNode next = location.next;
        if (next == null) {
            last = ilast;
        } else {
            next.prev = ilast;
        }
        location.next = ifirst;
        ilast.next = next;
        ifirst.prev = location;
        cache = null;
        insns.removeAll(false);
    }

    /**
     * Inserts the given instruction before the specified instruction.
     *
     * @param location
     *            an instruction <i>of this list</i> before which insn must be
     *            inserted.
     * @param insn
     *            the instruction to be inserted, <i>which must not belong to
     *            any {@link org.osgl.mvc.server.asm.tree.InsnList}</i>.
     */
    public void insertBefore(final org.osgl.mvc.server.asm.tree.AbstractInsnNode location,
            final org.osgl.mvc.server.asm.tree.AbstractInsnNode insn) {
        ++size;
        org.osgl.mvc.server.asm.tree.AbstractInsnNode prev = location.prev;
        if (prev == null) {
            first = insn;
        } else {
            prev.next = insn;
        }
        location.prev = insn;
        insn.next = location;
        insn.prev = prev;
        cache = null;
        insn.index = 0; // insn now belongs to an InsnList
    }

    /**
     * Inserts the given instructions before the specified instruction.
     *
     * @param location
     *            an instruction <i>of this list</i> before which the
     *            instructions must be inserted.
     * @param insns
     *            the instruction list to be inserted, which is cleared during
     *            the process. This list must be different from 'this'.
     */
    public void insertBefore(final org.osgl.mvc.server.asm.tree.AbstractInsnNode location,
            final org.osgl.mvc.server.asm.tree.InsnList insns) {
        if (insns.size == 0) {
            return;
        }
        size += insns.size;
        org.osgl.mvc.server.asm.tree.AbstractInsnNode ifirst = insns.first;
        org.osgl.mvc.server.asm.tree.AbstractInsnNode ilast = insns.last;
        org.osgl.mvc.server.asm.tree.AbstractInsnNode prev = location.prev;
        if (prev == null) {
            first = ifirst;
        } else {
            prev.next = ifirst;
        }
        location.prev = ilast;
        ilast.next = location;
        ifirst.prev = prev;
        cache = null;
        insns.removeAll(false);
    }

    /**
     * Removes the given instruction from this list.
     *
     * @param insn
     *            the instruction <i>of this list</i> that must be removed.
     */
    public void remove(final org.osgl.mvc.server.asm.tree.AbstractInsnNode insn) {
        --size;
        org.osgl.mvc.server.asm.tree.AbstractInsnNode next = insn.next;
        org.osgl.mvc.server.asm.tree.AbstractInsnNode prev = insn.prev;
        if (next == null) {
            if (prev == null) {
                first = null;
                last = null;
            } else {
                prev.next = null;
                last = prev;
            }
        } else {
            if (prev == null) {
                first = next;
                next.prev = null;
            } else {
                prev.next = next;
                next.prev = prev;
            }
        }
        cache = null;
        insn.index = -1; // insn no longer belongs to an InsnList
        insn.prev = null;
        insn.next = null;
    }

    /**
     * Removes all of the instructions of this list.
     *
     * @param mark
     *            if the instructions must be marked as no longer belonging to
     *            any {@link org.osgl.mvc.server.asm.tree.InsnList}.
     */
    void removeAll(final boolean mark) {
        if (mark) {
            org.osgl.mvc.server.asm.tree.AbstractInsnNode insn = first;
            while (insn != null) {
                org.osgl.mvc.server.asm.tree.AbstractInsnNode next = insn.next;
                insn.index = -1; // insn no longer belongs to an InsnList
                insn.prev = null;
                insn.next = null;
                insn = next;
            }
        }
        size = 0;
        first = null;
        last = null;
        cache = null;
    }

    /**
     * Removes all of the instructions of this list.
     */
    public void clear() {
        removeAll(false);
    }

    /**
     * Reset all labels in the instruction list. This method should be called
     * before reusing same instructions list between several
     * <code>ClassWriter</code>s.
     */
    public void resetLabels() {
        org.osgl.mvc.server.asm.tree.AbstractInsnNode insn = first;
        while (insn != null) {
            if (insn instanceof LabelNode) {
                ((LabelNode) insn).resetLabel();
            }
            insn = insn.next;
        }
    }

    // this class is not generified because it will create bridges
    private final class InsnListIterator implements ListIterator {

        org.osgl.mvc.server.asm.tree.AbstractInsnNode next;

        org.osgl.mvc.server.asm.tree.AbstractInsnNode prev;

        org.osgl.mvc.server.asm.tree.AbstractInsnNode remove;

        InsnListIterator(int index) {
            if (index == size()) {
                next = null;
                prev = getLast();
            } else {
                next = get(index);
                prev = next.prev;
            }
        }

        public boolean hasNext() {
            return next != null;
        }

        public Object next() {
            if (next == null) {
                throw new NoSuchElementException();
            }
            org.osgl.mvc.server.asm.tree.AbstractInsnNode result = next;
            prev = result;
            next = result.next;
            remove = result;
            return result;
        }

        public void remove() {
            if (remove != null) {
                if (remove == next) {
                    next = next.next;
                } else {
                    prev = prev.prev;
                }
                org.osgl.mvc.server.asm.tree.InsnList.this.remove(remove);
                remove = null;
            } else {
                throw new IllegalStateException();
            }
        }

        public boolean hasPrevious() {
            return prev != null;
        }

        public Object previous() {
            org.osgl.mvc.server.asm.tree.AbstractInsnNode result = prev;
            next = result;
            prev = result.prev;
            remove = result;
            return result;
        }

        public int nextIndex() {
            if (next == null) {
                return size();
            }
            if (cache == null) {
                cache = toArray();
            }
            return next.index;
        }

        public int previousIndex() {
            if (prev == null) {
                return -1;
            }
            if (cache == null) {
                cache = toArray();
            }
            return prev.index;
        }

        public void add(Object o) {
            org.osgl.mvc.server.asm.tree.InsnList.this.insertBefore(next, (org.osgl.mvc.server.asm.tree.AbstractInsnNode) o);
            prev = (org.osgl.mvc.server.asm.tree.AbstractInsnNode) o;
            remove = null;
        }

        public void set(Object o) {
            org.osgl.mvc.server.asm.tree.InsnList.this.set(next.prev, (org.osgl.mvc.server.asm.tree.AbstractInsnNode) o);
            prev = (AbstractInsnNode) o;
        }
    }
}
