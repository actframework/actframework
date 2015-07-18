package act.view;

import act.app.ActionContext;
import org.osgl.http.H;
import org.osgl.mvc.result.RenderBinary;
import org.osgl.mvc.result.RenderJSON;
import org.osgl.mvc.result.Result;
import org.osgl.storage.ISObject;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Render a template with template path and arguments provided
 * by {@link ActionContext}
 */
public class RenderAny extends Result {

    private Map<String, Object> renderArgs;

    public RenderAny() {
        super(H.Status.OK);
    }

    @Override
    public void apply(H.Request req, H.Response resp) {
        throw E.unsupport("RenderAny does not support " +
                "apply to request and response. Please use apply(AppContext) instead");
    }

    // TODO: Allow plugin to support rendering pdf, xls or other binary types
    public void apply(ActionContext context) {
        H.Format fmt = context.accept();
        switch (fmt) {
            case json:
                List<String> varNames = context.__appRenderArgNames();
                if (null != varNames && !varNames.isEmpty()) {
                    Map<String, Object> map = C.newMap();
                    for (String name : varNames) {
                        map.put(name, context.renderArg(name));
                    }
                    new RenderJSON(map).apply(context.req(), context.resp());
                    return;
                }
            case html:
            case txt:
            case csv:
                new RenderTemplate().apply(context);
                return;
            case pdf:
            case xls:
            case xlsx:
            case doc:
            case docx:
                varNames = context.__appRenderArgNames();
                if (null != varNames && !varNames.isEmpty()) {
                    Object firstVar = context.renderArg(varNames.get(0));
                    String action = S.str(context.actionPath()).afterLast(".").toString();
                    if (firstVar instanceof File) {
                        File file = (File) firstVar;
                        new RenderBinary(file, action).apply(context.req(), context.resp());
                        return;
                    } else if (firstVar instanceof InputStream) {
                        InputStream is = (InputStream)firstVar;
                        new RenderBinary(is, action).apply(context.req(), context.resp());
                        return;
                    } else if (firstVar instanceof ISObject) {
                        ISObject sobj = (ISObject) firstVar;
                        new RenderBinary(sobj.asInputStream(), action).apply(context.req(), context.resp());
                        return;
                    }
                }
            default:
                throw E.unsupport("Format not supported: %s", fmt);
        }
    }
}
