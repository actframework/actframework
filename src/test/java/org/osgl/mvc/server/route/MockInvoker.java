package org.osgl.mvc.server.route;


import org.osgl.mvc.server.AppContext;
import org.osgl.mvc.server.action.ActionInvokerBase;

public class MockInvoker extends ActionInvokerBase {

    private String name;

    private static final ThreadLocal<String> result = new ThreadLocal<String>();

    public MockInvoker(CharSequence name) {
        this.name = name.toString();
    }

    @Override
    public void invoke(AppContext context) {
        result.set(this.name);
    }

    public static String getResult() {
        return result.get();
    }
}
