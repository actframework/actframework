package test;

import act.Act;
import act.event.OnEvent;
import act.util.LogSupport;
import act.ws.*;
import org.osgl.mvc.annotation.GetAction;

/**
 * A simple hello world app entry
 *
 * Run this app, try to update some of the code, then
 * press F5 in the browser to watch the immediate change
 * in the browser!
 */
@SuppressWarnings("unused")
@WsEndpoint("/ws2")
public class AppEntry extends LogSupport {

    @WsEndpoint("/ws")
    public static class WsConnHandler extends LogSupport implements WebSocketConnectionListener {
        @Override
        public void onConnect(WebSocketContext context) {
            int n = context.manager().urlRegistry().count();
            context.send("count: " + n);
        }

        @Override
        public void onClose(WebSocketContext context) {
            info("Connection closed: " + context.url());
        }
    }

    @OnEvent
    public void onClose(WebSocketCloseEvent event) {
        info(">>> Connection closed: " + event.source().url());
    }

    @GetAction
    public void home() {
    }

    public static void main(String[] args) throws Exception {
        Act.start();
    }

}
