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
import org.osgl.util.Output;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;

public class OutputCache implements Output {
    private ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private Output tee = Output.Adaptors.of(baos);
    private Output out;
    private ByteBuffer buffer;

    public OutputCache(Output out) {
        this.out = out;
    }

    @Override
    public void open() {
    }

    @Override
    public void close() {
        if (null == buffer) {
            flush();
        }
    }

    @Override
    public void flush() {
        byte[] ba = baos.toByteArray();
        ByteBuffer buffer = ByteBuffer.allocateDirect(ba.length);
        buffer.put(ba);
        buffer.flip();
        this.buffer = buffer;
        // out already closed
        //out.close();
    }

    @Override
    public Output append(CharSequence csq) {
        tee.append(csq);
        out.append(csq);
        return this;
    }

    @Override
    public Output append(CharSequence csq, int start, int end) {
        tee.append(csq, start, end);
        out.append(csq, start, end);
        return this;
    }

    @Override
    public Output append(char c) {
        tee.append(c);
        out.append(c);
        return this;
    }

    @Override
    public Output append(byte[] bytes) {
        tee.append(bytes);
        out.append(bytes);
        return this;
    }

    @Override
    public Output append(byte[] bytes, int start, int end) {
        tee.append(bytes, start, end);
        out.append(bytes, start, end);
        return this;
    }

    @Override
    public Output append(byte b) {
        tee.append(b);
        out.append(b);
        return this;
    }

    @Override
    public Output append(ByteBuffer buffer) {
        tee.append(buffer);
        out.append(buffer);
        return this;
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
