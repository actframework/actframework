package act.route;


import act.handler.RequestHandlerBase;
import act.app.AppContext;

public class NamedMockHandler extends RequestHandlerBase {

    private String name;

    private static final ThreadLocal<String> result = new ThreadLocal<String>();

    public NamedMockHandler(CharSequence name) {
        this.name = name.toString();
    }

    @Override
    public void handle(AppContext context) {
        result.set(this.name);
    }

    public static String getName() {
        return result.get();
    }
}
