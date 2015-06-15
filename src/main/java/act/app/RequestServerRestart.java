package act.app;

import act.exception.ActException;

public class RequestServerRestart extends ActException {

    public RequestServerRestart() {
        super();
    }

    @Override
    public String getMessage() {
        return "Act restart needed";
    }
}
