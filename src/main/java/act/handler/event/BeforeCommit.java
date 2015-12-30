package act.handler.event;

import act.app.ActionContext;
import org.osgl.mvc.result.Result;

/**
 * This event will be triggered right before the {@link Result}
 * is committed
 */
public class BeforeCommit extends ResultEvent {
    public BeforeCommit(Result result, ActionContext context) {
        super(result, context);
    }
}
