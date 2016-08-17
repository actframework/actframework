package act.handler.builtin.controller;

import act.handler.RequestHandlerBase;

/**
 * For any handler that does not require the framework to lookup incoming request
 * and construct the sessions, it shall extends from this class
 */
public abstract class FastRequestHandler extends RequestHandlerBase {
    @Override
    public boolean requireResolveContext() {
        return false;
    }

    @Override
    public boolean sessionFree() {
        return true;
    }
}
