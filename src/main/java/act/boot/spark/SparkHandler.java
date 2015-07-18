package act.boot.spark;

import act.app.ActionContext;
import act.controller.Controller;
import org.osgl._;
import org.osgl.exception.NotAppliedException;

/**
 * App developer use this interface implement the controller or filter logic
 */
public abstract class SparkHandler extends _.F1<ActionContext, Object> {

    /**
     * Controller action method shall either return an {@link Object} or throw out a
     * {@link org.osgl.mvc.result.Result}, or return a {@code null} if it is a filter
     * implementation.
     * <ul>
     *     <li>When an {@code Object} is returned, the framework will use
     *     {@link Controller.Util#inferResult(Object, ActionContext)}
     *     to infer a {@link org.osgl.mvc.result.Result} from the object, and then
     *     apply the result been inferred</li>
     *     <li>When a {@code Result} is returned directly, the framework will
     *     apply it</li>
     *     <li>When {@code Result} is thrown out, the framework will capture it and
     *     apply it</li>
     *     <li>When {@code null} is returned in a filter, then framework will continue with
     *     next filter or action handler.</li>
     *     <li>When {@code null} is returned in all filters and also action handlers, the
     *     framework will emit a {@link org.osgl.mvc.result.ServerError}</li>
     * </ul>
     *
     * @param context
     * @return
     * @throws NotAppliedException
     * @throws _.Break
     */
    @Override
    public final Object apply(ActionContext context) throws NotAppliedException, _.Break {
        return handle(context);
    }

    /**
     * Application shall override this method to implement controller logic
     *
     * @param context Stores all relevant information required for request handling
     * @return an Object as the request handle result
     */
    protected abstract Object handle(ActionContext context);
}
