/*
 * @(#) ChunkedArrayList.java
 *
 * javautil Java Utility Library
 * Copyright (c) 2013, 2014 Peter Wall
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.pwall.util;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.RandomAccess;

/**
 * A {@link List} implementation optimised for the following case:
 * <ol>
 *   <li>The list may grow very large (several thousand elements), almost exclusively by
 *   addition at end</li>
 *   <li>Insertions in the middle or at the start of the list occur seldom or never</li>
 *   <li>Removals are rare (except by {@link #clear()}), particularly from the middle or start
 *   of the list</li>
 *   <li>Access to the list is both random (an individual item) and serial (a sequence starting
 *   at a nominated point)</li>
 * </ol>
 * The list is implemented as a set of chunks, each of which is an {@link ArrayList}
 * pre-allocated to a specified chunk size.  This has the advantage over a single
 * {@code ArrayList} that growth in the list does not cause constant re-allocation of arrays of
 * increasing size, with the consequent copying of the previous entries.
 *
 * @author Peter Wall
 * @param <E> the element type
 */
public class ChunkedArrayList<E> extends AbstractList<E> implements RandomAccess {

    public static final int defaultChunkSize = 1000;
    public static final int defaultInitialChunks = 20;
    public static final int minimumChunkSize = 2;

    private List<List<E>> outerList;
    private int chunkSize;

    /**
     * Construct a {@code ChunkedArrayList} with the specified chunk size and initial number of
     * chunks.
     *
     * @param chunkSize      the chunk size
     * @param initialChunks  the initial number of chunks
     * @throws IllegalArgumentException if the chunk size is less than the minimum
     */
    public ChunkedArrayList(int chunkSize, int initialChunks) {
        if (chunkSize < minimumChunkSize)
            throw new IllegalArgumentException("Chunk size " + chunkSize + " too low");
        outerList = new ArrayList<List<E>>(initialChunks);
        this.chunkSize = chunkSize;
    }

    /**
     * Construct a {@code ChunkedArrayList} with the specified chunk size and the default
     * initial number of chunks.
     *
     * @param chunkSize      the chunk size
     * @throws IllegalArgumentException if the chunk size is less than the minimum
     */
    public ChunkedArrayList(int chunkSize) {
        this(chunkSize, defaultInitialChunks);
    }

    /**
     * Construct a {@code ChunkedArrayList} with the default chunk size and initial number of
     * chunks.
     */
    public ChunkedArrayList() {
        this(defaultChunkSize);
    }

    /**
     * Construct a {@code ChunkedArrayList} with the default chunk size and initial number of
     * chunks, and then populate the list from an existing {@link Collection}.
     *
     * @param c the {@link Collection} to be copied to this list
     */
    public ChunkedArrayList(Collection<? extends E> c) {
        this();
        addAll(c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        int n = outerList.size();
        if (n == 0)
            return 0;
        n--;
        long result = (long)n * chunkSize + outerList.get(n).size();
        return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(Object o) {
        for (List<E> innerList : outerList)
            if (innerList.contains(o))
                return true;
        return false;
    }

    /**
     * Appends the specified element to the end of this list.
     *
     * @param e {@inheritDoc}
     * @return  {@inheritDoc}
     */
    @Override
    public boolean add(E e) {
        int n = outerList.size();
        List<E> innerList;
        if (n == 0 || (innerList = outerList.get(n - 1)).size() == chunkSize) {
            innerList = new ArrayList<E>(chunkSize);
            outerList.add(innerList);
        }
        innerList.add(e);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(Object o) {
        for (int i = 0, n = outerList.size(); i < n; i++) {
            List<E> innerList = outerList.get(i);
            if (innerList.remove(o)) {
                if (innerList.size() == 0)
                    outerList.remove(i);
                else {
                    while (++i < n) {
                        List<E> innerListNext = outerList.get(i);
                        innerList.add(innerListNext.remove(0));
                        if (innerListNext.size() == 0) {
                            outerList.remove(i);
                            break;
                        }
                        innerList = innerListNext;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Removes all of the elements from this list.  The list will be empty after this call
     * returns.
     */
    @Override
    public void clear() {
        for (List<E> innerList : outerList)
            innerList.clear();
        outerList.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public E get(int index) {
        if (index < 0 || index >= size())
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
        return outerList.get(index / chunkSize).get(index % chunkSize);
    }

    /**
     * Replaces the element at the specified position in this list with the specified element.
     *
     * @throws ClassCastException            {@inheritDoc}
     * @throws IndexOutOfBoundsException     {@inheritDoc}
     */
    @Override
    public E set(int index, E element) {
        if (index < 0 || index >= size())
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
        return outerList.get(index / chunkSize).set(index % chunkSize, element);
    }

    /**
     * Inserts the supplied element at the specified position in this list.  Shifts the element
     * currently at that position (if any) and any subsequent elements to the right (adds one to
     * their indices).
     *
     * @throws ClassCastException            {@inheritDoc}
     * @throws IndexOutOfBoundsException     {@inheritDoc}
     */
    @Override
    public void add(int index, E element) {
        if (index < 0 || index > size())
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
        int i = index % chunkSize;
        int j = index / chunkSize;
        while (j < outerList.size()) {
            List<E> innerList = outerList.get(j);
            if (innerList.size() < chunkSize) {
                innerList.add(i, element);
                return;
            }
            E movedElement = innerList.remove(chunkSize - 1);
            innerList.add(i, element);
            element = movedElement;
            j++;
            i = 0;
        }
        List<E> innerList = new ArrayList<E>(chunkSize);
        outerList.add(innerList);
        innerList.add(element);
    }

    /**
     * Removes the element at the specified position in this list.  Shifts any subsequent
     * elements to the left (subtracts one from their indices).  Returns the element that was
     * removed from the list.
     *
     * @throws IndexOutOfBoundsException     {@inheritDoc}
     */
    @Override
    public E remove(int index) {
        if (index < 0 || index >= size())
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
        int i = index % chunkSize;
        int j = index / chunkSize;
        List<E> innerList = outerList.get(j);
        E result = innerList.remove(i);
        if (innerList.size() == 0)
            outerList.remove(j);
        else {
            while (++j < outerList.size()) {
                List<E> innerListNext = outerList.get(j);
                innerList.add(innerListNext.remove(0));
                if (innerListNext.size() == 0) {
                    outerList.remove(j);
                    break;
                }
                innerList = innerListNext;
            }
        }
        return result;
    }

    /**
     * Returns the index of the first occurrence of the specified element in this list, or -1
     * if this list does not contain the element.  More formally, returns the lowest index
     * <tt>i</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>, or -1 if there
     * is no such index.
     *
     * @param o {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public int indexOf(Object o) {
        for (int i = 0, n = outerList.size(); i < n; i++) {
            List<E> innerList = outerList.get(i);
            int result = innerList.indexOf(o);
            if (result >= 0)
                return i * chunkSize + result;
        }
        return -1;
    }

    /**
     * Returns the index of the last occurrence of the specified element in this list, or -1 if
     * this list does not contain the element.  More formally, returns the highest index
     * <tt>i</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>, or -1 if there
     * is no such index.
     *
     * @param o {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public int lastIndexOf(Object o) {
        for (int i = outerList.size() - 1; i >= 0; i--) {
            List<E> innerList = outerList.get(i);
            int result = innerList.lastIndexOf(o);
            if (result >= 0)
                return i * chunkSize + result;
        }
        return -1;
    }

    private String outOfBoundsMsg(int index) {
        return "Index: " + index + ", Size: " + size();
    }

}