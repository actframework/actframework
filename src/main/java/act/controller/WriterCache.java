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
import org.osgl.util.Charsets;
import org.osgl.util.IO;

import java.io.*;
import java.nio.ByteBuffer;

public class WriterCache extends Writer implements CacheChannel {
    private StringWriter tee = new StringWriter();
    private Writer out;
    private ByteBuffer buffer;
    private boolean committed;

    public WriterCache(Writer out) {
        this.out = out;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        out.write(cbuf, off, len);
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
        commit();
    }

    @Override
    public void commit() {
        if (!committed) {
            String content = tee.toString();
            byte[] ba = content.getBytes(Charsets.UTF_8);
            ByteBuffer buffer = ByteBuffer.allocateDirect(ba.length);
            buffer.put(ba);
            buffer.flip();
            this.buffer = buffer;
            IO.write(content).ensureCloseSink().to(out);
            committed = true;
        }
    }

    @Override
    public boolean isCommitted() {
        return committed;
    }

    void apply(ActResponse resp) {
        resp.writeContent(buffer.duplicate());
    }

}
