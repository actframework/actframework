package act.handler.builtin;

import act.app.ActionContext;
import act.handler.builtin.controller.FastRequestHandler;
import act.handler.builtin.controller.RequestHandlerProxy;
import act.handler.event.BeforeResultCommit;
import act.view.ActServerError;
import org.osgl.http.H;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.mvc.result.Result;

import java.io.Serializable;

public class UnknownHttpMethodHandler extends FastRequestHandler implements Serializable {

    private static Logger logger = LogManager.get(UnknownHttpMethodHandler.class);

    public static final UnknownHttpMethodHandler INSTANCE = new UnknownHttpMethodHandler();

    @Override
    public void handle(ActionContext context) {
        H.Method method = context.req().method();
        Result result = context.config().unknownHttpMethodProcessor().handle(method);
        try {
            result = RequestHandlerProxy.GLOBAL_AFTER_INTERCEPTOR.apply(result, context);
        } catch (Exception e) {
            logger.error(e, "Error calling global after interceptor");
            result = ActServerError.of(e);
        }
        result.apply(context.req(), context.resp());
    }

    private Object readResolve() {
        return INSTANCE;
    }
}
