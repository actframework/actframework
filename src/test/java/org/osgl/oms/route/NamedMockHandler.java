package org.osgl.oms.route;


import org.osgl.oms.AppContext;
import org.osgl.oms.action.ActionHandlerBase;

public class NamedMockHandler extends ActionHandlerBase {

    private String name;

    private static final ThreadLocal<String> result = new ThreadLocal<String>();

    public NamedMockHandler(CharSequence name) {
        this.name = name.toString();
    }

    @Override
    public void invoke(AppContext context) {
        result.set(this.name);
    }

    public static String getName() {
        return result.get();
    }
}
