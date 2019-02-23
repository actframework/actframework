package act.ws;

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

import act.cli.Command;
import act.cli.Optional;
import org.osgl.util.S;

import javax.inject.Inject;

public class WebSocketAdminConsole {

    @Inject
    private WebSocketConnectionManager manager;

    @Command(name = "act.ws.conn.by-user", help = "report websocket connection number by user")
    public int userConnections(@Optional("specify user name") String username) {
        WebSocketConnectionRegistry registry = manager.usernameRegistry();
        return S.blank(username) ? registry.count() : registry.count(username);
    }

    @Command(name = "act.ws.conn.by-tag", help = "report websocket connection number by (channel) tag ")
    public int tagConnections(@Optional("specify the tag label") String label) {
        WebSocketConnectionRegistry registry = manager.tagRegistry();
        return S.blank(label) ? registry.count() : registry.count(label);
    }

    @Command(name = "act.ws.conn.by-session", help = "report websocket connection number by session id")
    public int sessionConnections(@Optional("specify the session id") String sessionId) {
        WebSocketConnectionRegistry registry = manager.sessionRegistry();
        return S.blank(sessionId) ? registry.count() : registry.count(sessionId);
    }

}
