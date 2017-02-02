package act.handler.builtin;

import act.app.ActionContext;
import act.app.App;
import act.controller.ParamNames;
import act.handler.builtin.controller.FastRequestHandler;
import org.osgl.http.H;
import org.osgl.util.E;
import org.osgl.util.FastStr;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;

public class StaticFileGetter extends FastRequestHandler {
    private File base;

    public StaticFileGetter(String base, App app) {
        E.NPE(base);
        this.base = app.file(base);
    }

    public StaticFileGetter(File base) {
        E.NPE(base);
        this.base = base;
    }

    @Override
    protected void releaseResources() {
        base = null;
    }

    @Override
    public void handle(ActionContext context) {
        context.handler(this);
        File file = base;
        if (!file.exists()) {
            // try load from resource
            AlwaysNotFound.INSTANCE.handle(context);
            return;
        }
        H.Format fmt;
        if (base.isDirectory()) {
            String path = context.paramVal(ParamNames.PATH);
            if (S.blank(path)) {
                AlwaysForbidden.INSTANCE.handle(context);
                return;
            }
            file = new File(base, path);
            if (!file.exists()) {
                AlwaysNotFound.INSTANCE.handle(context);
                return;
            }
            if (file.isDirectory() || !file.canRead()) {
                AlwaysForbidden.INSTANCE.handle(context);
                return;
            }
        }
        H.Response resp = context.resp();
        fmt = contentType(file.getPath());
        resp.contentType(fmt.contentType());
        InputStream is = new BufferedInputStream(IO.is(file));
        IO.copy(is, context.resp().outputStream());
    }

    // for unit test
    public File base() {
        return base;
    }

    static H.Format contentType(String path) {
        H.Format retVal = null;
        if (path.contains(".")) {
            FastStr s = FastStr.unsafeOf(path).afterLast('.');
            retVal = H.Format.of(s.toString());
        }
        return null == retVal ? H.Format.BINARY : retVal;
    }

    @Override
    public boolean supportPartialPath() {
        return base.isDirectory();
    }

    @Override
    public String toString() {
        boolean dir = supportPartialPath();
        return "file: " + (dir ? base().getPath() + "/**" : base().getPath());
    }
}
