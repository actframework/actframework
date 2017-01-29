package act.handler.event;

import act.app.ActionContext;
import act.event.ActEvent;
import act.event.SystemEvent;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.exception.NotAppliedException;
import org.osgl.http.H;
import org.osgl.mvc.result.Result;

public abstract class ResultEvent extends ActEvent<Result> implements SystemEvent {

    private final H.Request req;
    private final H.Response resp;


    public ResultEvent(Result result, H.Request req, H.Response resp) {
        super(result);
        this.req = $.notNull(req);
        this.resp = $.notNull(resp);
    }

    public Result result() {
        return source();
    }

    public H.Request request() {
        return req;
    }

    public H.Response response() {
        return resp;
    }

    public static final $.Func3<Result, H.Request<?>, H.Response<?>, Void> BEFORE_COMMIT_HANDLER =
            new $.F3<Result, H.Request<?>, H.Response<?>, Void>() {
                @Override
                public Void apply(Result result, H.Request<?> request, H.Response<?> response) throws NotAppliedException, Osgl.Break {
                    ActionContext context = request.context();
                    context.applyCorsSpec().applyContentType();
                    context.app().eventBus().trigger(new BeforeResultCommit(result, request, response));
                    return null;
                }
            };


    public static final $.Func3<Result, H.Request<?>, H.Response<?>, Void> AFTER_COMMIT_HANDLER =
            new $.F3<Result, H.Request<?>, H.Response<?>, Void>() {
                @Override
                public Void apply(Result result, H.Request<?> request, H.Response<?> response) throws NotAppliedException, Osgl.Break {
                    ActionContext context = request.context();
                    context.app().eventBus().trigger(new AfterResultCommit(result, request, response));
                    return null;
                }
            };

}
