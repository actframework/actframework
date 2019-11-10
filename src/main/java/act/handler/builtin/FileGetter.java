package act.handler.builtin;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.ActResponse;
import act.app.ActionContext;
import act.app.App;
import act.handler.builtin.controller.FastRequestHandler;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.util.FastStr;
import org.osgl.util.MimeType;
import org.osgl.util.S;

import java.io.File;

public class FileGetter extends FastRequestHandler {

    private File base;
    private FastRequestHandler delegate;

    public FileGetter(String base, App app) {
        this(app.file(base));
    }

    public FileGetter(File base) {
        this.base = $.requireNotNull(base);
        this.delegate = verifyBase(base);
    }

    @Override
    public boolean express(ActionContext context) {
        return null != delegate;
    }

    /**
     * No result commit event triggering for file getter
     *
     * @param context the action context.
     * @return `true`
     */
    @Override
    public boolean skipEvents(ActionContext context) {
        return true;
    }

    @Override
    protected void releaseResources() {
        base = null;
    }

    @Override
    public void handle(ActionContext context) {
        if (null != delegate) {
            delegate.handle(context);
            return;
        }
        context.handler(this);
        File file = base;
        H.Format fmt;
        if (base.isDirectory()) {
            String path = context.__pathParamVal();
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
        ActResponse resp = context.prepareRespForResultEvaluation();
        fmt = contentType(file.getPath());
        resp.contentType(fmt);
        context.applyCorsSpec().applyContentSecurityPolicy().applyContentType();
        resp.send(file);
    }

    // for unit test
    public File base() {
        return base;
    }

    public static H.Format contentType(String path) {
        H.Format retVal = null;
        if (path.contains(".")) {
            FastStr s = FastStr.unsafeOf(path).afterLast('.');
            retVal = H.Format.of(s.toString());
        }
        return null == retVal ? H.Format.BINARY : retVal;
    }

    public static boolean isText(H.Format format) {
        return MimeType.findByContentType(format.contentType()).test(MimeType.Trait.text);
    }

    public static boolean isBinary(H.Format format) {
        return !isText(format);
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

    /*
     * If base is valid then return null
     * otherwise return delegate request handler
     */
    private FastRequestHandler verifyBase(File base) {
        if (!base.exists()) {
            logger.warn("file base not exists: " + base);
            return AlwaysNotFound.INSTANCE;
        }
        if (!base.canRead()) {
            logger.warn("cannot read file base: " + base);
            return AlwaysForbidden.INSTANCE;
        }
        if (base.isDirectory() & (!base.canExecute())) {
            logger.warn("cannot access directory: " + base);
            return AlwaysForbidden.INSTANCE;
        }
        return null;
    }
}
