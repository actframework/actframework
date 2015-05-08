package org.osgl.oms.handler.builtin;

import org.osgl.http.H;
import org.osgl.mvc.result.NotFound;
import org.osgl.oms.app.AppContext;
import org.osgl.oms.controller.ParamNames;
import org.osgl.oms.handler.RequestHandlerBase;
import org.osgl.oms.handler.builtin.controller.FastRequestHandler;
import org.osgl.util.E;
import org.osgl.util.FastStr;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.InputStream;

public class StaticFileGetter extends FastRequestHandler {
    private String base;
    // by default base is folder
    private boolean baseIsFile;

    public StaticFileGetter(String base) {
        this(base, false);
    }

    public StaticFileGetter(String base, boolean baseIsFile) {
        E.NPE(base);
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        this.base = base;
        this.baseIsFile = baseIsFile;
    }

    @Override
    public void handle(AppContext context) {
        String path = base;
        if (!baseIsFile) {
            path = context.param(ParamNames.PATH);
            if (S.blank(path)) {
                path = base;
            } else {
                StringBuilder sb = new StringBuilder(base);
                if (!path.startsWith("/")) sb.append("/");
                path = sb.append(path).toString();
            }
        }
        H.Format fmt = contentType(path);
        InputStream is = inputStream(path);
        if (null == is) {
            throw new NotFound();
        }
        H.Response resp = context.resp();
        if (H.Format.unknown != fmt) {
            resp.contentType(fmt.toContentType());
        }
        IO.copy(is, context.resp().outputStream());
    }

    // for unit test
    public String base() {
        return base;
    }

    private H.Format contentType(String path) {
        FastStr s = FastStr.unsafeOf(path).afterLast('.');
        return H.Format.valueOfIgnoreCase(s.toString());
    }

    protected InputStream inputStream(String path) {
        return StaticFileGetter.class.getResourceAsStream(path);
    }

    @Override
    public boolean supportPartialPath() {
        return !baseIsFile;
    }

}
