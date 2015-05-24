package act.app;

import act.ActException;

public class RequestRefreshClassLoader extends ActException {

    public static final RequestRefreshClassLoader INSTANCE = new RequestRefreshClassLoader();

    public RequestRefreshClassLoader() {
        super();
    }

    @Override
    public String getMessage() {
        return "app class loader refresh required";
    }
}
