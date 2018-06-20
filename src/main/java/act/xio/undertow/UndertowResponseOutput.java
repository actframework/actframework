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
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

public class UndertowResponseOutput extends Writer implements Output {

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
    public void write(int c) {
        append((char) c);
    }

    @Override
    public void write(char[] cbuf) {
        write(cbuf, 0, cbuf.length);
    }

    @Override
    public void write(String str) {
        write(str, 0, str.length());
    }

    @Override
    public void write(String str, int off, int len) {
        append(str, off, off + len);
    }

    @Override
    public void write(char[] cbuf, int off, int len) {
        append(cbuf, off, len);
    }

    public UndertowResponseOutput append(char str[], int offset, int len) {
        char[] payload = new char[len];
        System.arraycopy(str, offset, payload, 0, len);
        byte[] bytes = toBytes(payload);
        return append(bytes);
    }


    @Override
    public UndertowResponseOutput append(CharSequence csq) {
        resp.writeContentPart(csq.toString());
        return this;
    }

    @Override
    public UndertowResponseOutput append(CharSequence csq, int start, int end) {
        return append(csq.subSequence(start, end));
    }

    @Override
    public UndertowResponseOutput append(char c) {
        return append("" + c);
    }

    @Override
    public UndertowResponseOutput append(byte[] bytes) {
        return append(ByteBuffer.wrap(bytes));
    }

    @Override
    public UndertowResponseOutput append(byte[] bytes, int start, int end) {
        return append(ByteBuffer.wrap(bytes, start, end));
    }

    @Override
    public UndertowResponseOutput append(byte b) {
        return append(new byte[b]);
    }

    @Override
    public UndertowResponseOutput append(ByteBuffer buffer) {
        resp.writeContentPart(buffer);
        return this;
    }

    @Override
    public OutputStream asOutputStream() {
        return new UndertowResponseOutputStream(resp);
    }

    @Override
    public Writer asWriter() {
        return this;
    }

    private static byte[] toBytes(char[] chars) {
        CharBuffer charBuffer = CharBuffer.wrap(chars);
        ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
        byte[] bytes = Arrays.copyOfRange(byteBuffer.array(),
                byteBuffer.position(), byteBuffer.limit());
        Arrays.fill(charBuffer.array(), '\u0000'); // clear sensitive data
        Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
        return bytes;
    }
}
