package act.controller;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
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

import act.ActResponse;
import org.osgl.util.IO;

import java.io.*;
import java.nio.ByteBuffer;

class OutputStreamCache extends OutputStream implements CacheChannel {
    private ByteArrayOutputStream tee = new ByteArrayOutputStream();
    private OutputStream out;
    private ByteBuffer buffer;
    private boolean committed;

    public OutputStreamCache(OutputStream os) {
        this.out = os;
    }

    @Override
    public void write(int b) {
        tee.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        tee.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        tee.write(b, off, len);
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
        commit();
    }

    @Override
    public boolean isCommitted() {
        return committed;
    }

    @Override
    public void commit() {
        if (!committed) {
            byte[] ba = tee.toByteArray();
            ByteBuffer buffer = ByteBuffer.allocateDirect(ba.length);
            buffer.put(ba);
            buffer.flip();
            this.buffer = buffer;
            IO.write(ba).ensureCloseSink().to(out);
            committed = true;
        }
    }

    void apply(ActResponse resp) {
        resp.writeContent(buffer.duplicate());
    }

}
