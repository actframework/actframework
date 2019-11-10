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

import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import org.osgl.$;
import org.osgl.util.Output;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

class NonBlockOutput implements Output {

    private final AtomicBoolean sending = new AtomicBoolean(false);
    private final ConcurrentLinkedQueue<ByteBuffer> pending = new ConcurrentLinkedQueue<>();

    private Sender sender;

    NonBlockOutput(Sender sender) {
        this.sender = $.requireNotNull(sender);
    }

    @Override
    public void open() {
    }

    @Override
    public void close() {
        sender.close(IoCallback.END_EXCHANGE);
    }

    @Override
    public void flush() {
    }

    @Override
    public Output append(CharSequence csq) {
        sender.send(csq.toString());
        return this;
    }

    @Override
    public Output append(CharSequence csq, int start, int end) {
        sender.send(csq.subSequence(start, end).toString());
        return this;
    }

    @Override
    public Output append(char c) {
        return append(String.valueOf(c));
    }

    @Override
    public Output append(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return append(buffer);
    }

    @Override
    public Output append(byte[] bytes, int start, int end) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes, start, end);
        return append(buffer);
    }

    @Override
    public Output append(byte b) {
        ByteBuffer buffer = ByteBuffer.allocate(1);
        buffer.put(b);
        buffer.flip();
        return append(buffer);
    }

    @Override
    public Output append(ByteBuffer buffer) {
        send(buffer);
        return this;
    }

    @Override
    public OutputStream asOutputStream() {
        return Adaptors.asOutputStream(this);
    }

    @Override
    public Writer asWriter() {
        return Adaptors.asWriter(this);
    }

    private IoCallback resume = new IoCallback() {
        @Override
        public void onComplete(HttpServerExchange exchange, Sender sender) {
            ByteBuffer next = pending.poll();
            if (null == next) {
                sending.set(false);
            } else {
                sender.send(next, this);
            }
        }

        @Override
        public void onException(HttpServerExchange exchange, Sender sender, IOException exception) {
        }
    };

    private void send(ByteBuffer buffer) {
        if (sending.compareAndSet(false, true)) {
            sender.send(buffer, resume);
        } else {
            pending.offer(buffer);
            if (sending.compareAndSet(false, true)) {
                pending.poll();
                sender.send(buffer, resume);
            }
        }
    }
}
