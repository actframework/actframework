package act.xio.undertow;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
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
import act.app.ActionContext;
import io.undertow.io.*;
import io.undertow.server.BlockingHttpExchange;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import org.osgl.http.H;
import org.osgl.util.WriterOutputStream;

import java.io.InputStream;
import java.io.OutputStream;

public class ActBlockingExchange implements BlockingHttpExchange {

    public static final AttachmentKey<ActionContext> KEY_APP_CTX = AttachmentKey.create(ActionContext.class);

    private InputStream inputStream;
    private OutputStream outputStream;
    private final HttpServerExchange exchange;

    public ActBlockingExchange(HttpServerExchange exchange, ActionContext context) {
        this.exchange = exchange;
        exchange.putAttachment(KEY_APP_CTX, context);
    }

    @Override
    public InputStream getInputStream() {
        if (inputStream == null) {
            inputStream = new UndertowInputStream(exchange);
        }
        return inputStream;
    }

    @Override
    public OutputStream getOutputStream() {
        if (outputStream == null) {
            outputStream = new UndertowOutputStream(exchange);
        }
        return outputStream;
    }

    @Override
    public Sender getSender() {
        H.Response response = ctx().resp();
        if (response.writerCreated()) {
            return new BlockingSenderImpl(exchange, new WriterOutputStream(response.writer()));
        } else {
            return new BlockingSenderImpl(exchange, response.outputStream());
        }
    }

    @Override
    public Receiver getReceiver() {
        return new BlockingReceiverImpl(this.exchange, this.getInputStream());
    }

    @Override
    public void close() {
        ActionContext ctx = ctx();
        if (!exchange.isComplete()) {
            try {
                UndertowRequest req = (UndertowRequest) ctx.req();
                req.closeAndDrainRequest();
            } finally {
                ActResponse resp = ctx.resp();
                resp.close();
            }
        } else {
            try {
                UndertowRequest req = (UndertowRequest) ctx.req();
                req.freeResources();
            } finally {
                UndertowResponse resp = (UndertowResponse) ctx.resp();
                resp.freeResources();
            }
        }
    }

    private ActionContext ctx() {
        return exchange.getAttachment(KEY_APP_CTX);
    }
}
