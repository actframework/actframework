package act.handler.event;

import org.osgl.http.H;
import org.osgl.mvc.result.Result;

/**
 * This event will be triggered right before the {@link Result}
 * is committed
 */
public class AfterResultCommit extends ResultEvent {
    public AfterResultCommit(Result result, H.Request request, H.Response response) {
        super(result, request, response);
    }
}
