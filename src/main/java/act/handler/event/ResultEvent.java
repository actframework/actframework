package act.handler.event;

import act.app.ActionContext;
import act.event.ActEvent;
import org.osgl.$;
import org.osgl.mvc.result.Result;

public abstract class ResultEvent extends ActEvent<Result> {

    private ActionContext context;

    public ResultEvent(Result result, ActionContext context) {
        super(result);
        this.context = $.notNull(context);
    }

    public Result result() {
        return source();
    }

    public ActionContext context() {
        return context;
    }
}
