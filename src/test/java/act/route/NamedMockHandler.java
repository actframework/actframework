package act.route;


import act.app.ActionContext;
import act.handler.RequestHandlerBase;

public class NamedMockHandler extends RequestHandlerBase {

    private String name;

    private static final ThreadLocal<String> result = new ThreadLocal<String>();

    public NamedMockHandler(CharSequence name) {
        this.name = name.toString();
    }

    @Override
    public void handle(ActionContext context) {
        result.set(this.name);
    }

    public static String getName() {
        return result.get();
    }

    @Override
    public boolean sessionFree() {
        return true;
    }
}
