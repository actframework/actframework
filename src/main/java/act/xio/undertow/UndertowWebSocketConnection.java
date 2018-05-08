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

import act.Act;
import act.util.DestroyableBase;
import act.xio.WebSocketConnection;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import org.osgl.$;
import org.osgl.http.H;

import java.io.IOException;

public class UndertowWebSocketConnection extends DestroyableBase implements WebSocketConnection {

    private final WebSocketChannel channel;
    private final String sessionId;
    private final String username;

    public UndertowWebSocketConnection(WebSocketChannel channel, H.Session session) {
        this.channel = $.requireNotNull(channel);
        this.sessionId = session.id();
        this.username = session.get(Act.appConfig().sessionKeyUsername());
    }

    @Override
    public String sessionId() {
        return sessionId;
    }

    @Override
    public String username() {
        return username;
    }

    @Override
    public void send(String message) {
        WebSockets.sendText(message, channel, null);
    }

    @Override
    protected void releaseResources() {
        try {
            channel.sendClose();
        } catch (IOException e) {
            // ignore
        }
    }

    @Override
    public void close() {
        destroy();
    }

    @Override
    public boolean closed() {
        return isDestroyed();
    }
}
