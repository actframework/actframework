package act.handler;

import act.app.ActionContext;
import act.handler.builtin.UnknownHttpMethodHandler;
import act.route.Router;
import act.util.CORS;
import org.osgl.http.H;
import org.osgl.mvc.result.NotFound;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Process HTTP OPTIONS request
 */
public class OptionsInfoBase {

    private Router router;
    private ConcurrentMap<String, RequestHandler> handlers = new ConcurrentHashMap<>();

    public OptionsInfoBase(Router router) {
        this.router = router;
    }

    public RequestHandler optionHandler(CharSequence path, ActionContext context) {
        String s = S.string(path);
        RequestHandler handler = handlers.get(s);
        if (null == handler) {
            handler = createHandler(path, context);
            handlers.putIfAbsent(s, handler);
        }
        return handler;
    }

    private RequestHandler createHandler(CharSequence path, ActionContext context) {
        if (!router.app().config().corsEnabled()) {
            return UnknownHttpMethodHandler.INSTANCE;
        }
        C.List<H.Method> allowMethods = C.newList();
        C.List<CORS.Handler> corsHandlers = C.newList();
        for (H.Method method: router.supportedHttpMethods()) {
            RequestHandler handler;
            try {
                handler = router.getInvoker(method, path, context);
            } catch (NotFound notFound) {
                continue;
            }
            allowMethods.add(method);
            CORS.Handler corsHandler = handler.corsHandler();
            if (corsHandler != CORS.Handler.DUMB) {
                corsHandlers.add(corsHandler);
            }
        }
        CORS.Handler corsHandler0 = CORS.handler(allowMethods);
        for (CORS.Handler handler: corsHandlers) {
            corsHandler0 = corsHandler0.chain(handler);
        }
        return new OptionHandler(corsHandler0);
    }

}
