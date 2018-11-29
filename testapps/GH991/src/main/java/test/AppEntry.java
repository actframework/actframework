package test;

import act.Act;
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
public class AppEntry {

    @WsEndpoint("/ws")
    public static class WsConnHandler implements WebSocketConnectionListener {
        @Override
        public void onConnect(WebSocketContext context) {
            int n = context.manager().urlRegistry().count();
            context.send("count: " + n);
        }
    }

    @GetAction
    public void home() {
    }

    public static void main(String[] args) throws Exception {
        Act.start();
    }

}
