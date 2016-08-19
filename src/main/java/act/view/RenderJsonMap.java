package act.view;

import act.app.ActionContext;
import org.osgl.http.H;

import java.util.Map;

/**
 * Render json Map using all {@link ActionContext#renderArgs}
 * by {@link ActionContext}.
 *
 * Note this will render the JSON result without regarding to
 * the http `Accept` header
 */
public class RenderJsonMap extends RenderAny {

    private Map<String, Object> renderArgs;

    public RenderJsonMap() {
    }

    public void apply(ActionContext context) {
        context.accept(H.Format.JSON);
        super.apply(context);
    }
}
