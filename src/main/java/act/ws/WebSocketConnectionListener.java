package act.ws;

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

import act.Act;
import act.app.App;
import act.app.event.SysEventId;
import act.util.*;
import org.osgl.util.E;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * The implementation of this interface will be registered automatically
 * and called when a websocket connection is established.
 *
 * Note if the implementation class is annotated with {@link WsEndpoint}
 * then it will match the URL path specified in `WsEndpoint` with the
 * current websocket connection URL. In case there is no URL setting in
 * the `WsEndpoint` then it will receive call on websocket connect event
 * of any URL path
 */
public interface WebSocketConnectionListener {

    /**
     * Implement this method to process websocket connection.
     *
     * @param context the web socket context
     */
    void onConnect(WebSocketContext context);

    /**
     * Implement this method to process websocket connection close event.
     *
     * @param context the web socket context
     */
    void onClose(WebSocketContext context);

    @NoAutoRegister
    class DelayedResolveProxy implements WebSocketConnectionListener {
        WebSocketConnectionListener realListener;
        public DelayedResolveProxy(final Class<? extends WebSocketConnectionListener> realListenerType) {
            E.NPE(realListenerType);
            final App app = Act.app();
            app.jobManager().on(SysEventId.DEPENDENCY_INJECTOR_PROVISIONED, new Runnable() {
                @Override
                public void run() {
                    realListener = app.getInstance(realListenerType);
                }
            }, true);
        }

        @Override
        public void onConnect(WebSocketContext context) {
            E.illegalStateIf(null == realListener);
            realListener.onConnect(context);
        }

        @Override
        public void onClose(WebSocketContext context) {
            E.illegalStateIf(null == realListener);
            realListener.onClose(context);
        }
    }

    @Singleton
    public class Manager extends LogSupportedDestroyableBase {

        // NOTE we have to leave it as public
        // as the Finder will be load by Application class loader
        // while the manager is not
        public List<WebSocketConnectionListener> freeListeners = new ArrayList<>();

        public void notifyFreeListeners(WebSocketContext context, boolean close) {
            if (close) {
                for (WebSocketConnectionListener listener : freeListeners) {
                    listener.onClose(context);
                }
            } else {
                for (WebSocketConnectionListener listener : freeListeners) {
                    listener.onConnect(context);
                }
            }
        }

        public static class Finder {

            @Inject
            Manager manager;

            @SubClassFinder
            public void found(Class<WebSocketConnectionListener> listenerClass) {
                WsEndpoint endpoint = listenerClass.getAnnotation(WsEndpoint.class);
                if (null == endpoint) {
                    manager.freeListeners.add(Act.getInstance(listenerClass));
                }
            }
        }

    }
}
