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
import org.osgl.util.Output;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;

public class OutputCache implements Output, CacheChannel {
    private ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private Output tee = Output.Adaptors.of(baos);
    private Output out;
    private ByteBuffer buffer;
    private boolean committed;

    public OutputCache(Output out) {
        this.out = out;
    }

    @Override
    public void open() {
    }

    @Override
    public void close() {
        commit();
    }

    @Override
    public void flush() {
    }

    @Override
    public Output append(CharSequence csq) {
        tee.append(csq);
        return this;
    }

    @Override
    public Output append(CharSequence csq, int start, int end) {
        tee.append(csq, start, end);
        return this;
    }

    @Override
    public Output append(char c) {
        tee.append(c);
        return this;
    }

    @Override
    public Output append(byte[] bytes) {
        tee.append(bytes);
        return this;
    }

    @Override
    public Output append(byte[] bytes, int start, int end) {
        tee.append(bytes, start, end);
        return this;
    }

    @Override
    public Output append(byte b) {
        tee.append(b);
        return this;
    }

    @Override
    public Output append(ByteBuffer buffer) {
        tee.append(buffer);
        return this;
    }

    public boolean isCommitted() {
        return committed;
    }

    @Override
    public void commit() {
        if (!committed) {
            byte[] ba = baos.toByteArray();
            ByteBuffer buffer = ByteBuffer.allocateDirect(ba.length);
            buffer.put(ba);
            buffer.flip();
            this.buffer = buffer;
            out.append(ba);
            IO.close(out);
            committed = true;
        }
    }

    @Override
    public OutputStream asOutputStream() {
        return Output.Adaptors.asOutputStream(this);
    }

    @Override
    public Writer asWriter() {
        return Output.Adaptors.asWriter(this);
    }

    void apply(ActResponse resp) {
        resp.writeContent(buffer.duplicate());
    }

}
