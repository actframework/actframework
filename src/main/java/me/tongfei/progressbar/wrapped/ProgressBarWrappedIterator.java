package me.tongfei.progressbar.wrapped;

/*-
 * #%L
 * TongFei ProgressBar
 * %%
 * Copyright (C) 2014 - 2018 Tongfei Chen
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

import me.tongfei.progressbar.ProgressBar;
import org.osgl.util.E;

import java.util.Iterator;

/**
 * @author Tongfei Chen
 * @since 0.6.0
 */
public class ProgressBarWrappedIterator<T> implements Iterator<T> {

    Iterator<T> underlying;
    ProgressBar pb;

    public ProgressBarWrappedIterator(Iterator<T> underlying, String task, long size) {
        this.underlying = underlying;
        pb = new ProgressBar(task, size).start();
    }

    @Override
    public boolean hasNext() {
        boolean r = underlying.hasNext();
        if (!r) pb.stop();
        return r;
    }

    @Override
    public T next() {
        T r = underlying.next();
        pb.step();
        return r;
    }

    @Override
    public void remove() {
        throw E.unsupport();
    }
}
