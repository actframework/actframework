package act.xio.undertow;

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

import org.osgl.util.Output;

import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;

public class UndertowResponseOutput implements Output {

    private UndertowResponse resp;

    public UndertowResponseOutput(UndertowResponse resp) {
        this.resp = resp;
    }

    @Override
    public void open() {
        resp.beforeWritingContent();
    }

    @Override
    public void close() {
        resp.afterWritingContent();
    }

    @Override
    public void flush() {
    }

    @Override
    public Output append(CharSequence csq) {
        resp.writeContentPart(csq.toString());
        return this;
    }

    @Override
    public Output append(CharSequence csq, int start, int end) {
        return append(csq.subSequence(start, end));
    }

    @Override
    public Output append(char c) {
        return append("" + c);
    }

    @Override
    public Output append(byte[] bytes) {
        return append(ByteBuffer.wrap(bytes));
    }

    @Override
    public Output append(byte[] bytes, int start, int end) {
        return append(ByteBuffer.wrap(bytes, start, end));
    }

    @Override
    public Output append(byte b) {
        return append(new byte[b]);
    }

    @Override
    public Output append(ByteBuffer buffer) {
        resp.writeContentPart(buffer);
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
}
