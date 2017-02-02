package act.handler.builtin;

import act.Act;
import act.app.ActionContext;
import act.conf.AppConfig;
import act.handler.ExpressHandler;
import act.handler.RequestHandler;
import act.handler.UnknownHttpMethodProcessor;
import act.handler.builtin.controller.FastRequestHandler;
import act.handler.builtin.controller.RequestHandlerProxy;
import act.view.ActErrorResult;
import org.osgl.http.H;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.mvc.result.Result;

import java.io.Serializable;

public class UnknownHttpMethodHandler extends FastRequestHandler implements Serializable {

    private static Logger logger = LogManager.get(UnknownHttpMethodHandler.class);
    public static final UnknownHttpMethodHandler INSTANCE = new UnknownHttpMethodHandler();

    private volatile UnknownHttpMethodProcessor configured;

    @Override
    public void handle(ActionContext context) {
        H.Method method = context.req().method();
        Result result = configured(context.config()).handle(method);
        try {
            result = RequestHandlerProxy.GLOBAL_AFTER_INTERCEPTOR.apply(result, context);
        } catch (Exception e) {
            logger.error(e, "Error calling global after interceptor");
            result = ActErrorResult.of(e);
        }
        result.apply(context.req(), context.resp());
    }

    @Override
    public boolean express(ActionContext context) {
        return configured(null) instanceof ExpressHandler;
    }

    private UnknownHttpMethodProcessor configured(AppConfig config) {
        if (null == configured) {
            synchronized (this) {
                if (null == configured) {
                    if (null == config) {
                        config = Act.appConfig();
                    }
                    configured = config.unknownHttpMethodProcessor();
                }
            }
        }
        return configured;
    }

    private Object readResolve() {
        return INSTANCE;
    }
}
