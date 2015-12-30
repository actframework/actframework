package act.handler.builtin;

import act.app.ActionContext;
import act.handler.builtin.controller.FastRequestHandler;
import act.handler.builtin.controller.RequestHandlerProxy;
import org.osgl.http.H;
import org.osgl.mvc.result.Result;

import java.io.Serializable;

public class UnknownHttpMethodHandler extends FastRequestHandler implements Serializable {

    public static final UnknownHttpMethodHandler INSTANCE = new UnknownHttpMethodHandler();

    @Override
    public void handle(ActionContext context) {
        H.Method method = context.req().method();
        Result result = context.config().unknownHttpMethodProcessor().handle(method);
        result = RequestHandlerProxy.GLOBAL_AFTER_INTERCEPTOR.apply(result, context);
        result.apply(context.req(), context.resp());
    }

    private Object readResolve() {
        return INSTANCE;
    }
}
