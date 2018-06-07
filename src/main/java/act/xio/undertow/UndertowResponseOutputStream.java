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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class UndertowResponseOutputStream extends OutputStream {

    private UndertowResponse resp;

    public UndertowResponseOutputStream(UndertowResponse resp) {
        this.resp = resp;
    }

    @Override
    public void write(byte[] b) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.wrap(b);
        this.resp.writeContentPart(byteBuffer);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.wrap(b, off, len);
        this.resp.writeContentPart(byteBuffer);
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
        resp.afterWritingContent();
    }

    @Override
    public void write(int b) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(1);
        byteBuffer.put((byte) b);
        byteBuffer.flip();
        this.resp.writeContentPart(byteBuffer);
    }
}
