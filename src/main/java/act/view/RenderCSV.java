package act.view;

import act.cli.view.CliView;
import act.util.ActContext;
import act.util.PropertySpec;
import org.osgl.http.H;
import org.osgl.mvc.result.RenderContent;

/**
 * Render object as CSV
 */
public class RenderCSV extends RenderContent {

    private static RenderCSV _INSTANCE = new RenderCSV() {
        @Override
        public String content() {
            return payload().message;
        }
    };

    private ActContext context;

    private RenderCSV() {
        super(H.Format.CSV);
    }

    public RenderCSV(Object v, PropertySpec.MetaInfo spec, ActContext context) {
        super(render(v, spec, context), H.Format.CSV);
        this.context = context;
    }

    public RenderCSV(H.Status status, Object v, PropertySpec.MetaInfo spec, ActContext context) {
        super(status, render(v, spec, context), H.Format.CSV);
        this.context = context;
    }

    public static RenderCSV get(Object v, PropertySpec.MetaInfo spec, ActContext context) {
        payload.get().message(render(v, spec, context));
        return _INSTANCE;
    }

    public static RenderCSV get(H.Status status, Object v, PropertySpec.MetaInfo spec, ActContext context) {
        payload.get().message(render(v, spec, context)).status(status);
        return _INSTANCE;
    }

    private static String render(Object v, PropertySpec.MetaInfo spec, ActContext context) {
        return CliView.CSV.render(v, spec, context);
    }
}
