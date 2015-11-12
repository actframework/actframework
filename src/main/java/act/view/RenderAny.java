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

import static org.osgl.http.H.Format.*;

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
        if (fmt == UNKNOWN) {
            throw E.unsupport("Unknown accept content type");
        }
        if (JSON == fmt) {
            List<String> varNames = context.__appRenderArgNames();
            Map<String, Object> map = C.newMap();
            if (null != varNames && !varNames.isEmpty()) {
                for (String name : varNames) {
                    map.put(name, context.renderArg(name));
                }
            }
            new RenderJSON(map).apply(context.req(), context.resp());
            return;
        } else if (HTML == fmt || TXT == fmt || CSV == fmt) {
            new RenderTemplate().apply(context);
            return;
        } else if (PDF == fmt || XLS == fmt || XLSX == fmt || DOC == fmt || DOCX == fmt) {
            List<String> varNames = context.__appRenderArgNames();
            if (null != varNames && !varNames.isEmpty()) {
                Object firstVar = context.renderArg(varNames.get(0));
                String action = S.str(context.actionPath()).afterLast(".").toString();
                if (firstVar instanceof File) {
                    File file = (File) firstVar;
                    new RenderBinary(file, action).apply(context.req(), context.resp());
                } else if (firstVar instanceof InputStream) {
                    InputStream is = (InputStream)firstVar;
                    new RenderBinary(is, action).apply(context.req(), context.resp());
                } else if (firstVar instanceof ISObject) {
                    ISObject sobj = (ISObject) firstVar;
                    new RenderBinary(sobj.asInputStream(), action).apply(context.req(), context.resp());
                }
                throw E.unsupport("Unknown render arg type [%s] for binary response", firstVar.getClass());
            } else {
                throw E.unexpected("No render arg found for binary response");
            }
        }
        throw E.unexpected("Unknown accept content type: %s", fmt.contentType());
    }
}
